package com.example.biblioteca.service;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.repository.EmprestimoRepository;
import com.example.biblioteca.repository.LivroRepositoryDB;
import com.example.biblioteca.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Servico de negocio com persistencia em SQLite.
 *
 * Coordena os tres repositorios (Livro, Emprestimo, Usuario) e
 * implementa todas as regras de negocio da biblioteca.
 *
 * Esta classe nao conhece SQL — apenas orquestra os repositorios.
 */
public class BibliotecaServiceDB {

    private final LivroRepositoryDB   livroRepo;
    private final EmprestimoRepository empRepo;
    private final UsuarioRepository    usuarioRepo;

    public BibliotecaServiceDB(LivroRepositoryDB livroRepo,
                                EmprestimoRepository empRepo,
                                UsuarioRepository usuarioRepo) {
        this.livroRepo   = livroRepo;
        this.empRepo     = empRepo;
        this.usuarioRepo = usuarioRepo;
    }

    // ════════════════════════════════════════════════════════════════
    // LIVROS
    // ════════════════════════════════════════════════════════════════

    /** Cadastra um novo livro no acervo. */
    public void cadastrarLivro(Livro livro) {
        livroRepo.adicionar(livro);
    }

    /** Busca livro pelo ISBN. */
    public Optional<Livro> buscarLivro(String isbn) {
        return livroRepo.buscarPorIsbn(isbn);
    }

    /** Busca livros por trecho do nome do autor. */
    public List<Livro> buscarPorAutor(String autor) {
        return livroRepo.buscarPorAutor(autor);
    }

    /** Lista todos os livros disponiveis para emprestimo. */
    public List<Livro> listarDisponiveis() {
        return livroRepo.listarDisponiveis();
    }

    /** Lista todos os livros do acervo. */
    public List<Livro> listarTodosLivros() {
        return livroRepo.listarTodos();
    }

    /**
     * Atualiza titulo, autor e ano de um livro existente.
     * O ISBN nao pode ser alterado.
     */
    public void atualizarLivro(Livro livro) {
        livroRepo.atualizar(livro);
    }

    /**
     * Remove um livro do acervo.
     * Lanca excecao se houver emprestimos ativos para ele.
     */
    public void removerLivro(String isbn) {
        livroRepo.remover(isbn);
    }

    /** Total de livros no acervo. */
    public int totalLivros() {
        return livroRepo.total();
    }

    // ════════════════════════════════════════════════════════════════
    // EMPRESTIMOS
    // ════════════════════════════════════════════════════════════════

    /**
     * Registra um novo emprestimo.
     *
     * Regras:
     *   1. O livro deve existir no acervo
     *   2. O livro deve estar disponivel
     *   3. Gera ID sequencial no formato EMP-XXXX
     *   4. Persiste o emprestimo E atualiza disponibilidade do livro
     */
    public Emprestimo realizarEmprestimo(String isbn, String usuario, LocalDate data) {
        Livro livro = livroRepo.buscarPorIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Livro nao encontrado: " + isbn));

        if (!livro.isDisponivel())
            throw new IllegalStateException("Livro indisponivel: " + livro.getTitulo());

        String id  = "EMP-" + String.format("%04d", empRepo.proximoContador());
        Emprestimo emp = new Emprestimo(id, livro, usuario, data);

        // Persiste emprestimo e atualiza status do livro no banco
        empRepo.salvar(emp);
        livroRepo.atualizarDisponibilidade(isbn, false);
        livro.setDisponivel(false);   // sincroniza objeto em memoria

        return emp;
    }

    /**
     * Registra a devolucao de um emprestimo.
     *
     * Regras:
     *   1. O emprestimo deve existir
     *   2. Nao pode ja estar devolvido
     *   3. Data de devolucao nao pode ser anterior a data de emprestimo
     *   4. Libera o livro no banco
     */
    public void registrarDevolucao(String idEmprestimo, LocalDate dataDevolucao) {
        Emprestimo emp = empRepo.buscarPorId(idEmprestimo)
            .orElseThrow(() -> new IllegalArgumentException(
                "Emprestimo nao encontrado: " + idEmprestimo));

        // Delega validacao ao modelo de dominio
        emp.registrarDevolucao(dataDevolucao);

        // Persiste no banco
        empRepo.registrarDevolucao(idEmprestimo, dataDevolucao);
        livroRepo.atualizarDisponibilidade(emp.getLivro().getIsbn(), true);
    }

    /** Lista emprestimos ativos com atraso na data informada. */
    public List<Emprestimo> listarEmAtraso(LocalDate hoje) {
        return empRepo.listarEmAtraso(hoje);
    }

    /** Historico completo de emprestimos de um usuario. */
    public List<Emprestimo> historicoUsuario(String usuario) {
        return empRepo.historicoUsuario(usuario);
    }

    /** Lista todos os emprestimos ativos (nao devolvidos). */
    public List<Emprestimo> listarEmprestimosAtivos() {
        return empRepo.listarAtivos();
    }

    /** Total de emprestimos ativos no momento. */
    public int totalEmprestimosAtivos() {
        return empRepo.totalAtivos();
    }

    // ════════════════════════════════════════════════════════════════
    // USUARIOS / AUTENTICACAO
    // ════════════════════════════════════════════════════════════════

    /** Cadastra um novo usuario no sistema. */
    public void cadastrarUsuario(Usuario usuario) {
        usuarioRepo.cadastrar(usuario);
    }

    /**
     * Autentica um usuario.
     * Retorna o Usuario se as credenciais forem validas.
     * Lanca IllegalArgumentException se as credenciais forem invalidas.
     */
    public Usuario login(String loginStr, String senha) {
        return usuarioRepo.autenticar(loginStr, senha)
            .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
    }

    /** Atualiza a senha de um usuario. */
    public void atualizarSenha(String login, String novaSenha) {
        usuarioRepo.atualizarSenha(login, novaSenha);
    }

    /** Total de usuarios cadastrados. */
    public int totalUsuarios() {
        return usuarioRepo.total();
    }
}
