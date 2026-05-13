package com.example.biblioteca.cli;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.service.BibliotecaServiceDB;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Menu principal do sistema com controle de perfil.
 *
 * ADMIN         — acesso total
 * BIBLIOTECARIO — livros + empréstimos (sem gerenciar usuários)
 * LEITOR        — apenas consultas de livros e próprio histórico
 */
public class MenuPrincipal {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BibliotecaServiceDB service;
    private final Connection          conn;
    private final Usuario             usuarioLogado;

    public MenuPrincipal(BibliotecaServiceDB service, Connection conn, Usuario usuarioLogado) {
        this.service       = service;
        this.conn          = conn;
        this.usuarioLogado = usuarioLogado;
    }

    public void iniciar() {
        boolean sair = false;
        while (!sair) {
            exibirMenu();
            int op = lerOpcaoPermitida();
            switch (op) {
                // Passa o usuário logado — cada menu aplica suas próprias restrições
                case 1 -> new MenuLivros(service, usuarioLogado).exibir();
                case 2 -> new MenuEmprestimos(service, usuarioLogado).exibir();
                case 3 -> {
                    if (isAdmin()) new MenuUsuarios(conn).exibir();
                    else          Console.erro("Acesso negado. Apenas ADMIN pode gerenciar usuarios.");
                }
                case 4 -> exibirDashboard();
                case 0 -> {
                    if (Console.confirmar("Deseja sair do sistema?")) {
                        Console.info("\nAte logo, " + usuarioLogado.getNome() + "!\n");
                        sair = true;
                    }
                }
            }
        }
    }

    // ── Menu principal ───────────────────────────────────────────────────────

    private void exibirMenu() {
        Console.limpar();
        Console.titulo("MENU PRINCIPAL  |  " + usuarioLogado.getNome()
                + "  [" + usuarioLogado.getPerfil() + "]");

        Console.opcao(1, "Gerenciar Livros        (Listar / Buscar" + (podeEditar() ? " / Cadastrar / Editar / Remover" : " — somente consulta") + ")");
        Console.opcao(2, "Gerenciar Emprestimos   (" + (podeOperar() ? "Emprestar / Devolver / " : "") + "Listar / Atrasos / Historico)");

        if (isAdmin()) {
            Console.opcao(3, "Gerenciar Usuarios      (Cadastrar / Listar / Alterar / Remover)");
        } else {
            System.out.println("  [3] Gerenciar Usuarios  [apenas ADMIN]");
        }

        Console.opcao(4, "Dashboard               (Resumo do sistema)");
        Console.opcao(0, "Sair do sistema");
        System.out.println();
    }

    private int lerOpcaoPermitida() {
        while (true) {
            int op = Console.lerOpcao(0, 4);
            if (op == 3 && !isAdmin()) {
                Console.erro("Acesso negado. Apenas ADMIN pode acessar o gerenciamento de usuarios.");
            } else {
                return op;
            }
        }
    }

    // ── Dashboard ───────────────────────────────────────────────────────────

    private void exibirDashboard() {
        Console.titulo("DASHBOARD DO SISTEMA");

        int totalLivros   = service.totalLivros();
        int disponiveis   = service.listarDisponiveis().size();
        int emprestados   = totalLivros - disponiveis;
        int ativos        = service.totalEmprestimosAtivos();
        int totalUsuarios = service.totalUsuarios();

        List<Emprestimo> atrasados = service.listarEmAtraso(LocalDate.now());

        Console.subtitulo("Acervo");
        Console.info(String.format("Total de livros    : %d", totalLivros));
        Console.info(String.format("Livros disponiveis : %d", disponiveis));
        Console.info(String.format("Livros emprestados : %d", emprestados));

        Console.subtitulo("Emprestimos");
        Console.info(String.format("Emprestimos ativos  : %d", ativos));
        Console.info(String.format("Em atraso hoje      : %d", atrasados.size()));
        Console.info(String.format("Usuarios cadastrados: %d", totalUsuarios));

        if (!atrasados.isEmpty()) {
            Console.subtitulo("Atencao — Emprestimos em atraso");
            atrasados.forEach(e ->
                    Console.aviso(String.format("[%s] %s — %s (%d dias)",
                            e.getId(), e.getLivro().getTitulo(),
                            e.getNomeUsuario(), e.calcularDiasAtraso(LocalDate.now())))
            );
        }

        Console.subtitulo("Ultimos livros disponiveis");
        service.listarDisponiveis().stream().limit(5)
                .forEach(l -> Console.info("  -> [" + l.getIsbn() + "] " + l.getTitulo()));

        Console.pausar();
    }

    // ── Helpers de perfil ───────────────────────────────────────────────────

    private boolean isAdmin() {
        return "ADMIN".equals(usuarioLogado.getPerfil());
    }

    private boolean podeEditar() {
        return isAdmin() || "BIBLIOTECARIO".equals(usuarioLogado.getPerfil());
    }

    private boolean podeOperar() {
        return isAdmin() || "BIBLIOTECARIO".equals(usuarioLogado.getPerfil());
    }
}