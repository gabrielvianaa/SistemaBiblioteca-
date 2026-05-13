package com.example.biblioteca.cli;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.service.BibliotecaServiceDB;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Menu CLI de Empréstimos com controle de perfil.
 *
 * LEITOR        — listar ativos, atrasos, histórico próprio
 * BIBLIOTECARIO — tudo: realizar empréstimo + devolver + consultas
 * ADMIN         — tudo que BIBLIOTECARIO faz
 */
public class MenuEmprestimos {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BibliotecaServiceDB service;
    private final Usuario             usuario;

    public MenuEmprestimos(BibliotecaServiceDB service, Usuario usuario) {
        this.service = service;
        this.usuario = usuario;
    }

    // ── helpers de perfil ─────────────────────────────────────────────────────

    private boolean podeOperar() {
        return "ADMIN".equals(usuario.getPerfil())
                || "BIBLIOTECARIO".equals(usuario.getPerfil());
    }

    // ── menu ──────────────────────────────────────────────────────────────────

    public void exibir() {
        boolean voltar = false;
        while (!voltar) {
            Console.titulo("GERENCIAMENTO DE EMPRESTIMOS  [" + usuario.getPerfil() + "]");

            // Operações restritas — BIBLIOTECARIO e ADMIN
            if (podeOperar()) {
                Console.opcao(1, "Realizar novo emprestimo");
                Console.opcao(2, "Registrar devolucao");
            } else {
                System.out.println("  [1] Realizar novo emprestimo  [apenas BIBLIOTECARIO/ADMIN]");
                System.out.println("  [2] Registrar devolucao       [apenas BIBLIOTECARIO/ADMIN]");
            }

            // Consultas — todos os perfis
            Console.opcao(3, "Listar emprestimos ativos");
            Console.opcao(4, "Listar emprestimos em atraso");
            Console.opcao(5, "Historico de emprestimos por usuario");
            Console.opcao(0, "Voltar ao menu principal");
            System.out.println();

            int op = Console.lerOpcao(0, 5);

            // Bloqueia 1 e 2 para LEITOR mesmo que ele tente digitar o número
            if ((op == 1 || op == 2) && !podeOperar()) {
                Console.erro("Acesso negado. Apenas BIBLIOTECARIO ou ADMIN podem realizar esta operacao.");
                Console.pausar();
                continue;
            }

            switch (op) {
                case 1 -> realizarEmprestimo();
                case 2 -> registrarDevolucao();
                case 3 -> listarAtivos();
                case 4 -> listarEmAtraso();
                case 5 -> historicoUsuario();
                case 0 -> voltar = true;
            }
        }
    }

    // ── 1: Realizar emprestimo ───────────────────────────────────────────────

    private void realizarEmprestimo() {
        Console.titulo("REALIZAR EMPRESTIMO");

        List<Livro> disponiveis = service.listarDisponiveis();
        if (disponiveis.isEmpty()) {
            Console.aviso("Nao ha livros disponiveis para emprestimo no momento.");
            Console.pausar();
            return;
        }

        Console.subtitulo("Livros disponiveis");
        disponiveis.forEach(l ->
                Console.info(String.format("%-20s | %s", l.getIsbn(), l.getTitulo()))
        );
        System.out.println();

        try {
            String    isbn    = Console.lerTextoObrigatorio("ISBN do livro");
            String    nomeUsu = Console.lerTextoObrigatorio("Nome do usuario");
            LocalDate data    = lerData("Data do emprestimo (dd/MM/yyyy) [ENTER = hoje]", LocalDate.now());

            Emprestimo emp = service.realizarEmprestimo(isbn, nomeUsu, data);

            Console.sucesso("Emprestimo realizado com sucesso!");
            Console.info("ID do emprestimo : " + emp.getId());
            Console.info("Livro            : " + emp.getLivro().getTitulo());
            Console.info("Usuario          : " + emp.getNomeUsuario());
            Console.info("Data emprestimo  : " + emp.getDataEmprestimo().format(FMT));
            Console.info("Prazo devolucao  : " + emp.getDataEmprestimo().plusDays(14).format(FMT));

        } catch (IllegalArgumentException | IllegalStateException e) {
            Console.erro(e.getMessage());
        }
        Console.pausar();
    }

    // ── 2: Registrar devolucao ───────────────────────────────────────────────

    private void registrarDevolucao() {
        Console.titulo("REGISTRAR DEVOLUCAO");

        List<Emprestimo> ativos = service.listarEmprestimosAtivos();
        if (ativos.isEmpty()) {
            Console.aviso("Nao ha emprestimos ativos no momento.");
            Console.pausar();
            return;
        }

        Console.subtitulo("Emprestimos ativos");
        imprimirCabecalho();
        ativos.forEach(this::imprimirLinha);
        Console.separador();
        System.out.println();

        try {
            String    id   = Console.lerTextoObrigatorio("ID do emprestimo (ex: EMP-0001)");
            LocalDate data = lerData("Data da devolucao (dd/MM/yyyy) [ENTER = hoje]", LocalDate.now());

            service.registrarDevolucao(id, data);

            Console.sucesso("Devolucao registrada com sucesso!");
            Console.info("Emprestimo : " + id);
            Console.info("Devolvido  : " + data.format(FMT));

        } catch (IllegalArgumentException | IllegalStateException e) {
            Console.erro(e.getMessage());
        }
        Console.pausar();
    }

    // ── 3: Listar ativos ────────────────────────────────────────────────────

    private void listarAtivos() {
        Console.titulo("EMPRESTIMOS ATIVOS");
        List<Emprestimo> lista = service.listarEmprestimosAtivos();
        if (lista.isEmpty()) {
            Console.aviso("Nenhum emprestimo ativo no momento.");
        } else {
            imprimirCabecalho();
            lista.forEach(this::imprimirLinha);
            Console.separador();
            Console.info("Total: " + lista.size() + " emprestimo(s) ativo(s).");
        }
        Console.pausar();
    }

    // ── 4: Listar em atraso ─────────────────────────────────────────────────

    private void listarEmAtraso() {
        Console.titulo("EMPRESTIMOS EM ATRASO");
        LocalDate hoje = LocalDate.now();
        List<Emprestimo> lista = service.listarEmAtraso(hoje);
        if (lista.isEmpty()) {
            Console.sucesso("Nenhum emprestimo em atraso. Tudo em dia!");
        } else {
            Console.info("Data de referencia: " + hoje.format(FMT));
            System.out.println();
            Console.separador();
            System.out.printf("  %-10s %-24s %-18s %-12s %s%n",
                    "ID", "LIVRO", "USUARIO", "EMPRESTADO EM", "DIAS ATRASO");
            Console.separador();
            lista.forEach(e -> {
                long   dias    = e.calcularDiasAtraso(hoje);
                String titulo  = e.getLivro().getTitulo();
                String tituloC = titulo.length() > 22 ? titulo.substring(0, 22) + ".." : titulo;
                String usuC    = e.getNomeUsuario().length() > 16
                        ? e.getNomeUsuario().substring(0, 16) + ".." : e.getNomeUsuario();
                System.out.printf("  %-10s %-24s %-18s %-12s %d dia(s)%n",
                        e.getId(), tituloC, usuC, e.getDataEmprestimo().format(FMT), dias);
            });
            Console.separador();
            Console.aviso(lista.size() + " emprestimo(s) em atraso!");
        }
        Console.pausar();
    }

    // ── 5: Historico por usuario ────────────────────────────────────────────

    private void historicoUsuario() {
        Console.titulo("HISTORICO DE EMPRESTIMOS POR USUARIO");

        String nomeConsulta;
        // LEITOR só pode ver o próprio histórico
        if ("LEITOR".equals(usuario.getPerfil())) {
            nomeConsulta = usuario.getNome();
            Console.info("Exibindo seu proprio historico: " + nomeConsulta);
        } else {
            nomeConsulta = Console.lerTextoObrigatorio("Nome do usuario");
        }

        List<Emprestimo> lista = service.historicoUsuario(nomeConsulta);
        if (lista.isEmpty()) {
            Console.aviso("Nenhum emprestimo encontrado para: " + nomeConsulta);
        } else {
            Console.info("Usuario: " + nomeConsulta + " | Total: " + lista.size() + " emprestimo(s)");
            Console.separador();
            System.out.printf("  %-10s %-26s %-12s %-12s %s%n",
                    "ID", "LIVRO", "EMPRESTADO", "DEVOLVIDO", "STATUS");
            Console.separador();
            lista.forEach(e -> {
                String titulo  = e.getLivro().getTitulo();
                String tituloC = titulo.length() > 24 ? titulo.substring(0, 24) + ".." : titulo;
                String devData = e.getDataDevolucao() != null
                        ? e.getDataDevolucao().format(FMT) : "---";
                System.out.printf("  %-10s %-26s %-12s %-12s %s%n",
                        e.getId(), tituloC,
                        e.getDataEmprestimo().format(FMT), devData,
                        e.isDevolvido() ? "Devolvido" : "Ativo");
            });
            Console.separador();
        }
        Console.pausar();
    }

    // ── Utilitarios ─────────────────────────────────────────────────────────

    private LocalDate lerData(String prompt, LocalDate padrao) {
        while (true) {
            String entrada = Console.lerTexto(prompt);
            if (entrada.isBlank()) return padrao;
            try {
                return LocalDate.parse(entrada, FMT);
            } catch (DateTimeParseException e) {
                Console.erro("Formato invalido. Use dd/MM/yyyy (ex: 25/06/2025).");
            }
        }
    }

    private void imprimirCabecalho() {
        System.out.printf("  %-10s %-24s %-18s %-12s %s%n",
                "ID", "LIVRO", "USUARIO", "EMPRESTADO EM", "PRAZO");
        Console.separador();
    }

    private void imprimirLinha(Emprestimo e) {
        String titulo  = e.getLivro().getTitulo();
        String tituloC = titulo.length() > 22 ? titulo.substring(0, 22) + ".." : titulo;
        String usuC    = e.getNomeUsuario().length() > 16
                ? e.getNomeUsuario().substring(0, 16) + ".." : e.getNomeUsuario();
        System.out.printf("  %-10s %-24s %-18s %-12s %s%n",
                e.getId(), tituloC, usuC,
                e.getDataEmprestimo().format(FMT),
                e.getDataEmprestimo().plusDays(14).format(FMT));
    }
}