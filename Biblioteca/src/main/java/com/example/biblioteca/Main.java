package com.example.biblioteca;

import com.example.biblioteca.api.ApiServer;
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

public class Main {

    public static void main(String[] args) throws Exception {

        boolean modoApi = temArg(args, "--api");
        boolean modoCli = temArg(args, "--cli");

        boolean rodarApi = modoApi || !modoCli;
        boolean rodarCli = modoCli || !modoApi;

        try (Connection conn = DatabaseConnection.novaConexao("jdbc:sqlite:biblioteca.db")) {

            LivroRepositoryDB    livroRepo   = new LivroRepositoryDB(conn);
            EmprestimoRepository empRepo     = new EmprestimoRepository(conn, livroRepo);
            UsuarioRepository    usuarioRepo = new UsuarioRepository(conn);
            BibliotecaServiceDB  service     = new BibliotecaServiceDB(livroRepo, empRepo, usuarioRepo);

            popularDadosIniciais(service, usuarioRepo, livroRepo);

            ApiServer api = null;
            if (rodarApi) {
                api = new ApiServer(service);
                api.iniciar(8080);
            }

            if (rodarCli) {
                var usuarioLogado = new TelaLogin(service).autenticar();
                if (usuarioLogado != null) {
                    new MenuPrincipal(service, conn, usuarioLogado).iniciar();
                }
            } else if (rodarApi) {
                System.out.println("[API] Pressione Ctrl+C para encerrar o servidor.");
                Thread.currentThread().join();
            }

            if (api != null) api.parar();

        } catch (Exception e) {
            System.err.println("\n[ERRO CRITICO] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean temArg(String[] args, String flag) {
        for (String a : args) if (a.equalsIgnoreCase(flag)) return true;
        return false;
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
            service.cadastrarLivro(new Livro("978-85-333-0001-1", "Dom Casmurro",     "Machado de Assis", 1899));
            service.cadastrarLivro(new Livro("978-85-333-0002-2", "O Cortico",         "Aluisio Azevedo",  1890));
            service.cadastrarLivro(new Livro("978-85-333-0003-3", "Quincas Borba",     "Machado de Assis", 1891));
            service.cadastrarLivro(new Livro("978-85-333-0004-4", "A Moreninha",       "Joaquim Macedo",   1844));
            service.cadastrarLivro(new Livro("978-85-333-0005-5", "Memorias Postumas", "Machado de Assis", 1881));
            service.cadastrarLivro(new Livro("978-85-333-0006-6", "Iracema",           "Jose de Alencar",  1865));
            service.cadastrarLivro(new Livro("978-85-333-0007-7", "O Guarani",         "Jose de Alencar",  1857));
            System.out.println("[SETUP] Acervo inicial criado com 7 livros.");
        }
    }
}
