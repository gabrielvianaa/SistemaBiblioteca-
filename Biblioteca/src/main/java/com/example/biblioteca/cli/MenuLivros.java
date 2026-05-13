package com.example.biblioteca.cli;

import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.service.BibliotecaServiceDB;

import java.util.List;

/**
 * Menu CLI de CRUD de Livros com controle de perfil.
 *
 * LEITOR        — apenas consulta (listar, buscar, disponíveis)
 * BIBLIOTECARIO — consulta + cadastrar + editar + remover
 * ADMIN         — tudo que BIBLIOTECARIO faz
 */
public class MenuLivros {

    private final BibliotecaServiceDB service;
    private final Usuario             usuario;

    public MenuLivros(BibliotecaServiceDB service, Usuario usuario) {
        this.service = service;
        this.usuario = usuario;
    }

    // ── helpers de perfil ─────────────────────────────────────────────────────

    private boolean isLeitor() {
        return "LEITOR".equals(usuario.getPerfil());
    }

    private boolean podeEditar() {
        return "ADMIN".equals(usuario.getPerfil())
                || "BIBLIOTECARIO".equals(usuario.getPerfil());
    }

    // ── menu ──────────────────────────────────────────────────────────────────

    public void exibir() {
        boolean voltar = false;
        while (!voltar) {
            Console.titulo("GERENCIAMENTO DE LIVROS  [" + usuario.getPerfil() + "]");

            // Consultas — todos os perfis
            Console.opcao(1, "Listar todos os livros");
            Console.opcao(2, "Buscar livro por ISBN");
            Console.opcao(3, "Buscar livros por autor");
            Console.opcao(4, "Listar livros disponiveis");

            // Escrita — apenas BIBLIOTECARIO e ADMIN
            if (podeEditar()) {
                Console.opcao(5, "Cadastrar novo livro");
                Console.opcao(6, "Editar livro");
                Console.opcao(7, "Remover livro");
            } else {
                System.out.println("  [5] Cadastrar novo livro  [apenas BIBLIOTECARIO/ADMIN]");
                System.out.println("  [6] Editar livro          [apenas BIBLIOTECARIO/ADMIN]");
                System.out.println("  [7] Remover livro         [apenas BIBLIOTECARIO/ADMIN]");
            }

            Console.opcao(0, "Voltar ao menu principal");
            System.out.println();

            int op = Console.lerOpcao(0, 7);

            // Bloqueia 5, 6 e 7 para LEITOR mesmo que ele tente digitar o número
            if ((op == 5 || op == 6 || op == 7) && !podeEditar()) {
                Console.erro("Acesso negado. Apenas BIBLIOTECARIO ou ADMIN podem realizar esta operacao.");
                Console.pausar();
                continue;
            }

            switch (op) {
                case 1 -> listarTodos();
                case 2 -> buscarPorIsbn();
                case 3 -> buscarPorAutor();
                case 4 -> listarDisponiveis();
                case 5 -> cadastrar();
                case 6 -> editar();
                case 7 -> remover();
                case 0 -> voltar = true;
            }
        }
    }

    // ── C: Cadastrar ─────────────────────────────────────────────────────────

    private void cadastrar() {
        Console.titulo("CADASTRAR NOVO LIVRO");
        try {
            String isbn   = Console.lerTextoObrigatorio("ISBN (ex: 978-85-333-0001-1)");
            String titulo = Console.lerTextoObrigatorio("Titulo");
            String autor  = Console.lerTextoObrigatorio("Autor");
            int    ano    = Console.lerInteiro("Ano de publicacao (ex: 1899)");

            service.cadastrarLivro(new Livro(isbn, titulo, autor, ano));
            Console.sucesso("Livro cadastrado com sucesso!");
            Console.info("ISBN   : " + isbn);
            Console.info("Titulo : " + titulo);
            Console.info("Autor  : " + autor);
            Console.info("Ano    : " + ano);
        } catch (IllegalArgumentException e) {
            Console.erro(e.getMessage());
        }
        Console.pausar();
    }

    // ── R: Listar / Buscar ───────────────────────────────────────────────────

    private void listarTodos() {
        Console.titulo("TODOS OS LIVROS DO ACERVO");
        List<Livro> livros = service.listarTodosLivros();
        if (livros.isEmpty()) {
            Console.aviso("Nenhum livro cadastrado.");
        } else {
            imprimirCabecalho();
            livros.forEach(this::imprimirLinha);
            Console.separador();
            Console.info("Total: " + livros.size() + " livro(s).");
        }
        Console.pausar();
    }

    private void buscarPorIsbn() {
        Console.titulo("BUSCAR LIVRO POR ISBN");
        String isbn = Console.lerTextoObrigatorio("ISBN");
        service.buscarLivro(isbn).ifPresentOrElse(
                livro -> {
                    Console.subtitulo("Livro encontrado");
                    imprimirCabecalho();
                    imprimirLinha(livro);
                },
                () -> Console.aviso("Nenhum livro encontrado com o ISBN: " + isbn)
        );
        Console.pausar();
    }

    private void buscarPorAutor() {
        Console.titulo("BUSCAR LIVROS POR AUTOR");
        String autor = Console.lerTextoObrigatorio("Nome do autor (parcial aceito)");
        List<Livro> livros = service.buscarPorAutor(autor);
        if (livros.isEmpty()) {
            Console.aviso("Nenhum livro encontrado para autor: " + autor);
        } else {
            imprimirCabecalho();
            livros.forEach(this::imprimirLinha);
            Console.info("Total: " + livros.size() + " livro(s) encontrado(s).");
        }
        Console.pausar();
    }

    private void listarDisponiveis() {
        Console.titulo("LIVROS DISPONIVEIS PARA EMPRESTIMO");
        List<Livro> livros = service.listarDisponiveis();
        if (livros.isEmpty()) {
            Console.aviso("Nenhum livro disponivel no momento.");
        } else {
            imprimirCabecalho();
            livros.forEach(this::imprimirLinha);
            Console.info("Total: " + livros.size() + " livro(s) disponivel(is).");
        }
        Console.pausar();
    }

    // ── U: Editar ────────────────────────────────────────────────────────────

    private void editar() {
        Console.titulo("EDITAR LIVRO");
        String isbn = Console.lerTextoObrigatorio("ISBN do livro a editar");

        service.buscarLivro(isbn).ifPresentOrElse(livroAtual -> {
            Console.subtitulo("Dados atuais");
            Console.info("Titulo : " + livroAtual.getTitulo());
            Console.info("Autor  : " + livroAtual.getAutor());
            Console.info("Ano    : " + livroAtual.getAnoPub());
            Console.info("(Deixe em branco para manter o valor atual)");
            System.out.println();

            String novoTitulo = Console.lerTexto("Novo titulo");
            String novoAutor  = Console.lerTexto("Novo autor");
            String novoAnoStr = Console.lerTexto("Novo ano");

            String titulo = novoTitulo.isBlank() ? livroAtual.getTitulo() : novoTitulo;
            String autor  = novoAutor.isBlank()  ? livroAtual.getAutor()  : novoAutor;
            int    ano    = livroAtual.getAnoPub();
            if (!novoAnoStr.isBlank()) {
                try {
                    ano = Integer.parseInt(novoAnoStr);
                } catch (NumberFormatException e) {
                    Console.erro("Ano invalido. Mantendo valor atual: " + ano);
                }
            }

            try {
                service.atualizarLivro(new Livro(isbn, titulo, autor, ano));
                Console.sucesso("Livro atualizado com sucesso!");
            } catch (IllegalArgumentException e) {
                Console.erro(e.getMessage());
            }
        }, () -> Console.aviso("Livro nao encontrado: " + isbn));

        Console.pausar();
    }

    // ── D: Remover ───────────────────────────────────────────────────────────

    private void remover() {
        Console.titulo("REMOVER LIVRO");
        String isbn = Console.lerTextoObrigatorio("ISBN do livro a remover");

        service.buscarLivro(isbn).ifPresentOrElse(livro -> {
            Console.info("Titulo : " + livro.getTitulo());
            Console.info("Autor  : " + livro.getAutor());
            System.out.println();

            if (Console.confirmar("Confirma a remocao deste livro?")) {
                try {
                    service.removerLivro(isbn);
                    Console.sucesso("Livro removido com sucesso!");
                } catch (IllegalArgumentException | IllegalStateException e) {
                    Console.erro(e.getMessage());
                }
            } else {
                Console.aviso("Operacao cancelada.");
            }
        }, () -> Console.aviso("Livro nao encontrado: " + isbn));

        Console.pausar();
    }

    // ── Layout ───────────────────────────────────────────────────────────────

    private void imprimirCabecalho() {
        Console.separador();
        System.out.printf("  %-20s %-25s %-20s %-4s %-12s%n",
                "ISBN", "TITULO", "AUTOR", "ANO", "STATUS");
        Console.separador();
    }

    private void imprimirLinha(Livro l) {
        String isbnC   = l.getIsbn().length()   > 18 ? l.getIsbn().substring(0, 18)   + ".." : l.getIsbn();
        String tituloC = l.getTitulo().length()  > 23 ? l.getTitulo().substring(0, 23)  + ".." : l.getTitulo();
        String autorC  = l.getAutor().length()   > 18 ? l.getAutor().substring(0, 18)   + ".." : l.getAutor();
        System.out.printf("  %-20s %-25s %-20s %-4d %-12s%n",
                isbnC, tituloC, autorC, l.getAnoPub(),
                l.isDisponivel() ? "DISPONIVEL" : "EMPRESTADO");
    }
}