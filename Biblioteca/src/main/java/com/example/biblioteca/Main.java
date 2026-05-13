package com.example.biblioteca;

import com.example.biblioteca.cli.MenuPrincipal;
import com.example.biblioteca.cli.TelaLogin;
import com.example.biblioteca.db.DatabaseConnection;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.repository.EmprestimoRepository;
import com.example.biblioteca.repository.LivroRepositoryDB;
import com.example.biblioteca.repository.UsuarioRepository;
import com.example.biblioteca.service.BibliotecaServiceDB;

import java.sql.Connection;

/**
 * Ponto de entrada da aplicacao.
 *
 * Fluxo:
 *   1. Conecta ao SQLite (cria banco e tabelas se necessario)
 *   2. Popula dados iniciais na primeira execucao
 *   3. Exibe tela de login
 *   4. Inicia o menu principal da CLI
 *   5. Fecha a conexao ao sair (try-with-resources)
 */
public class Main {

    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.novaConexao("jdbc:sqlite:biblioteca.db")) {

            // ── Repositorios e servico ────────────────────────────────────
            LivroRepositoryDB    livroRepo   = new LivroRepositoryDB(conn);
            EmprestimoRepository empRepo     = new EmprestimoRepository(conn, livroRepo);
            UsuarioRepository    usuarioRepo = new UsuarioRepository(conn);
            BibliotecaServiceDB  service     = new BibliotecaServiceDB(livroRepo, empRepo, usuarioRepo);

            // ── Carga inicial (somente banco vazio) ───────────────────────
            popularDadosIniciais(service, usuarioRepo, livroRepo);

            // ── Login ─────────────────────────────────────────────────────
            var usuarioLogado = new TelaLogin(service).autenticar();
            if (usuarioLogado == null) return; // max tentativas atingido

            // ── Menu principal ────────────────────────────────────────────
            new MenuPrincipal(service, conn, usuarioLogado).iniciar();

        } catch (Exception e) {
            System.err.println("\n[ERRO CRITICO] " + e.getMessage());
            e.printStackTrace();
        }
        // conexao fechada automaticamente — sem SQLITE_BUSY
    }

    private static void popularDadosIniciais(BibliotecaServiceDB service,
                                              UsuarioRepository usuarioRepo,
                                              LivroRepositoryDB livroRepo) {
        if (usuarioRepo.total() == 0) {
            service.cadastrarUsuario(new Usuario("Administrador",      "admin", "admin123", "ADMIN"));
            service.cadastrarUsuario(new Usuario("Maria Bibliotecaria","maria", "maria123", "BIBLIOTECARIO"));
            service.cadastrarUsuario(new Usuario("Joao Leitor",        "joao",  "joao123",  "LEITOR"));
            System.out.println("[SETUP] Usuarios iniciais criados.");
        }

        if (livroRepo.total() == 0) {
            service.cadastrarLivro(new Livro("978-85-333-0001-1", "Dom Casmurro",      "Machado de Assis", 1899));
            service.cadastrarLivro(new Livro("978-85-333-0002-2", "O Cortico",          "Aluisio Azevedo",  1890));
            service.cadastrarLivro(new Livro("978-85-333-0003-3", "Quincas Borba",      "Machado de Assis", 1891));
            service.cadastrarLivro(new Livro("978-85-333-0004-4", "A Moreninha",        "Joaquim Macedo",   1844));
            service.cadastrarLivro(new Livro("978-85-333-0005-5", "Memorias Postumas",  "Machado de Assis", 1881));
            service.cadastrarLivro(new Livro("978-85-333-0006-6", "Iracema",            "Jose de Alencar",  1865));
            service.cadastrarLivro(new Livro("978-85-333-0007-7", "O Guarani",          "Jose de Alencar",  1857));
            System.out.println("[SETUP] Acervo inicial criado com 7 livros.");
        }
    }
}
