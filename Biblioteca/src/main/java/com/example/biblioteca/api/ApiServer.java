package com.example.biblioteca.api;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.service.BibliotecaServiceDB;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Servidor HTTP da API REST do Sistema de Biblioteca.
 *
 * Porta: 8080
 * Base URL: http://localhost:8080
 *
 * Endpoints disponíveis:
 *
 *   AUTH
 *     POST   /api/auth/login
 *
 *   LIVROS
 *     GET    /api/livros
 *     GET    /api/livros/{isbn}
 *     GET    /api/livros?autor=xxx
 *     POST   /api/livros
 *     PUT    /api/livros/{isbn}
 *     DELETE /api/livros/{isbn}
 *
 *   EMPRÉSTIMOS
 *     GET    /api/emprestimos
 *     GET    /api/emprestimos/atraso
 *     GET    /api/emprestimos/usuario/{nome}
 *     POST   /api/emprestimos
 *     PATCH  /api/emprestimos/{id}/devolver
 */
public class ApiServer {

    private final BibliotecaServiceDB service;
    private Javalin app;

    public ApiServer(BibliotecaServiceDB service) {
        this.service = service;
    }

    public void iniciar(int porta) {
        app = Javalin.create(config -> {
            // Permite requisições de qualquer origem (útil para Postman)
            config.bundledPlugins.enableCors(cors ->
                cors.addRule(rule -> rule.anyHost())
            );
        });

        registrarRotas();
        app.start(porta);
        System.out.println("[API] Servidor rodando em http://localhost:" + porta);
    }

    public void parar() {
        if (app != null) app.stop();
    }

    private void registrarRotas() {

        // ── Tratamento global de erros ────────────────────────────────────
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(Map.of("erro", e.getMessage()));
        });
        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(409).json(Map.of("erro", e.getMessage()));
        });
        app.exception(Exception.class, (e, ctx) -> {
            ctx.status(500).json(Map.of("erro", "Erro interno: " + e.getMessage()));
        });

        app.post("/api/auth/login", this::login);

        app.get ("/api/livros",         this::listarOuBuscarLivros);
        app.get ("/api/livros/{isbn}",  this::buscarLivroPorIsbn);
        app.post("/api/livros",         this::cadastrarLivro);
        app.put ("/api/livros/{isbn}",  this::atualizarLivro);
        app.delete("/api/livros/{isbn}",this::removerLivro);

        app.get  ("/api/emprestimos",                  this::listarEmprestimos);
        app.get  ("/api/emprestimos/atraso",           this::listarEmAtraso);
        app.get  ("/api/emprestimos/usuario/{nome}",   this::historicoUsuario);
        app.post ("/api/emprestimos",                  this::realizarEmprestimo);
        app.patch("/api/emprestimos/{id}/devolver",    this::registrarDevolucao);
    }

    private void login(Context ctx) {
        var body  = ctx.bodyAsClass(Map.class);
        String login = getString(body, "login");
        String senha = getString(body, "senha");

        try {
            Usuario u = service.login(login, senha);
            ctx.status(200).json(Map.of(
                "nome",   u.getNome(),
                "login",  u.getLogin(),
                "perfil", u.getPerfil()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(401).json(Map.of("erro", "Credenciais invalidas"));
        }
    }

    private void listarOuBuscarLivros(Context ctx) {
        String autor      = ctx.queryParam("autor");
        String disponivel = ctx.queryParam("disponivel");

        if (autor != null && !autor.isBlank()) {
            ctx.status(200).json(service.buscarPorAutor(autor));
        } else if ("true".equalsIgnoreCase(disponivel)) {
            ctx.status(200).json(service.listarDisponiveis());
        } else {
            ctx.status(200).json(service.listarTodosLivros());
        }
    }

    private void buscarLivroPorIsbn(Context ctx) {
        String isbn = ctx.pathParam("isbn");
        service.buscarLivro(isbn)
            .ifPresentOrElse(
                livro -> ctx.status(200).json(livroParaMap(livro)),
                ()    -> ctx.status(404).json(Map.of("erro", "Livro nao encontrado: " + isbn))
            );
    }


    private void cadastrarLivro(Context ctx) {
        var body = ctx.bodyAsClass(Map.class);
        String isbn   = getString(body, "isbn");
        String titulo = getString(body, "titulo");
        String autor  = getString(body, "autor");
        int    ano    = getInt(body, "anoPub");

        try {
            Livro livro = new Livro(isbn, titulo, autor, ano);
            service.cadastrarLivro(livro);
            ctx.status(201).json(livroParaMap(livro));
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().contains("ISBN ja existe") ? 409 : 400;
            ctx.status(status).json(Map.of("erro", e.getMessage()));
        }
    }

    private void atualizarLivro(Context ctx) {
        String isbn = ctx.pathParam("isbn");
        var body    = ctx.bodyAsClass(Map.class);

        Livro atual = service.buscarLivro(isbn)
            .orElse(null);
        if (atual == null) {
            ctx.status(404).json(Map.of("erro", "Livro nao encontrado: " + isbn));
            return;
        }

        String titulo = body.containsKey("titulo") ? getString(body,"titulo") : atual.getTitulo();
        String autor  = body.containsKey("autor")  ? getString(body,"autor")  : atual.getAutor();
        int    ano    = body.containsKey("anoPub")  ? getInt(body,"anoPub")   : atual.getAnoPub();

        Livro atualizado = new Livro(isbn, titulo, autor, ano);
        service.atualizarLivro(atualizado);
        ctx.status(200).json(livroParaMap(atualizado));
    }


    private void removerLivro(Context ctx) {
        String isbn = ctx.pathParam("isbn");
        if (service.buscarLivro(isbn).isEmpty()) {
            ctx.status(404).json(Map.of("erro", "Livro nao encontrado: " + isbn));
            return;
        }
        try {
            service.removerLivro(isbn);
            ctx.status(204);
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of("erro", e.getMessage()));
        }
    }


    private void listarEmprestimos(Context ctx) {
        ctx.status(200).json(
            service.listarEmprestimosAtivos()
                   .stream()
                   .map(this::emprestimoParaMap)
                   .toList()
        );
    }


    private void listarEmAtraso(Context ctx) {
        LocalDate hoje = parseData(ctx.queryParam("hoje"), LocalDate.now());
        ctx.status(200).json(
            service.listarEmAtraso(hoje)
                   .stream()
                   .map(e -> {
                       var m = new java.util.LinkedHashMap<>(emprestimoParaMap(e));
                       m.put("diasAtraso", e.calcularDiasAtraso(hoje));
                       return m;
                   })
                   .toList()
        );
    }


    private void historicoUsuario(Context ctx) {
        String nome = ctx.pathParam("nome");
        ctx.status(200).json(
            service.historicoUsuario(nome)
                   .stream()
                   .map(this::emprestimoParaMap)
                   .toList()
        );
    }


    private void realizarEmprestimo(Context ctx) {
        var body    = ctx.bodyAsClass(Map.class);
        String isbn = getString(body, "isbn");
        String usu  = getString(body, "usuario");
        LocalDate data = parseData((String) body.get("data"), LocalDate.now());

        try {
            Emprestimo emp = service.realizarEmprestimo(isbn, usu, data);
            ctx.status(201).json(emprestimoParaMap(emp));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of("erro", e.getMessage()));
        }
    }


    private void registrarDevolucao(Context ctx) {
        String id   = ctx.pathParam("id");
        var body    = ctx.bodyAsClass(Map.class);
        LocalDate data = parseData((String) body.get("dataDevolucao"), LocalDate.now());

        try {
            service.registrarDevolucao(id, data);
            // Retorna confirmação
            ctx.status(200).json(Map.of(
                "id",         id,
                "devolvido",  true,
                "dataDevolucao", data.toString()
            ));
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(Map.of("erro", e.getMessage()));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of("erro", e.getMessage()));
        }
    }


    private Map<String, Object> livroParaMap(Livro l) {
        return Map.of(
            "isbn",       l.getIsbn(),
            "titulo",     l.getTitulo(),
            "autor",      l.getAutor(),
            "anoPub",     l.getAnoPub(),
            "disponivel", l.isDisponivel()
        );
    }

    private Map<String, Object> emprestimoParaMap(Emprestimo e) {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("id",              e.getId());
        map.put("livro",           livroParaMap(e.getLivro()));
        map.put("nomeUsuario",     e.getNomeUsuario());
        map.put("dataEmprestimo",  e.getDataEmprestimo().toString());
        map.put("dataDevolucao",   e.getDataDevolucao() != null
                                       ? e.getDataDevolucao().toString() : null);
        map.put("devolvido",       e.isDevolvido());
        map.put("prazo",           e.getDataEmprestimo().plusDays(14).toString());
        return map;
    }


    private String getString(Map body, String campo) {
        Object val = body.get(campo);
        if (val == null || val.toString().isBlank())
            throw new IllegalArgumentException("Campo obrigatorio ausente ou vazio: " + campo);
        return val.toString().trim();
    }

    private int getInt(Map body, String campo) {
        Object val = body.get(campo);
        if (val == null)
            throw new IllegalArgumentException("Campo obrigatorio ausente: " + campo);
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Campo '" + campo + "' deve ser um numero inteiro.");
        }
    }

    private LocalDate parseData(String texto, LocalDate padrao) {
        if (texto == null || texto.isBlank()) return padrao;
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data invalida. Use o formato YYYY-MM-DD. Recebido: " + texto);
        }
    }
}
