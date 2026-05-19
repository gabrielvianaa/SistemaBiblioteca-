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

    public void cadastrarLivro(Livro livro) {
        livroRepo.adicionar(livro);
    }

    public Optional<Livro> buscarLivro(String isbn) {
        return livroRepo.buscarPorIsbn(isbn);
    }

    public List<Livro> buscarPorAutor(String autor) {
        return livroRepo.buscarPorAutor(autor);
    }

    public List<Livro> listarDisponiveis() {
        return livroRepo.listarDisponiveis();
    }

    public List<Livro> listarTodosLivros() {
        return livroRepo.listarTodos();
    }

    public void atualizarLivro(Livro livro) {
        livroRepo.atualizar(livro);
    }

    public void removerLivro(String isbn) {
        livroRepo.remover(isbn);
    }

    public int totalLivros() {
        return livroRepo.total();
    }

    public Emprestimo realizarEmprestimo(String isbn, String usuario, LocalDate data) {
        Livro livro = livroRepo.buscarPorIsbn(isbn)
            .orElseThrow(() -> new IllegalArgumentException("Livro nao encontrado: " + isbn));

        if (!livro.isDisponivel())
            throw new IllegalStateException("Livro indisponivel: " + livro.getTitulo());

        String id  = "EMP-" + String.format("%04d", empRepo.proximoContador());
        Emprestimo emp = new Emprestimo(id, livro, usuario, data);

        empRepo.salvar(emp);
        livroRepo.atualizarDisponibilidade(isbn, false);
        livro.setDisponivel(false);   // sincroniza objeto em memoria

        return emp;
    }

    public void registrarDevolucao(String idEmprestimo, LocalDate dataDevolucao) {
        Emprestimo emp = empRepo.buscarPorId(idEmprestimo)
            .orElseThrow(() -> new IllegalArgumentException(
                "Emprestimo nao encontrado: " + idEmprestimo));

        emp.registrarDevolucao(dataDevolucao);

        empRepo.registrarDevolucao(idEmprestimo, dataDevolucao);
        livroRepo.atualizarDisponibilidade(emp.getLivro().getIsbn(), true);
    }

    public List<Emprestimo> listarEmAtraso(LocalDate hoje) {
        return empRepo.listarEmAtraso(hoje);
    }

    public List<Emprestimo> historicoUsuario(String usuario) {
        return empRepo.historicoUsuario(usuario);
    }

    public List<Emprestimo> listarEmprestimosAtivos() {
        return empRepo.listarAtivos();
    }

    public int totalEmprestimosAtivos() {
        return empRepo.totalAtivos();
    }
    public void cadastrarUsuario(Usuario usuario) {
        usuarioRepo.cadastrar(usuario);
    }

    public Usuario login(String loginStr, String senha) {
        return usuarioRepo.autenticar(loginStr, senha)
            .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));
    }

    public void atualizarSenha(String login, String novaSenha) {
        usuarioRepo.atualizarSenha(login, novaSenha);
    }

    public int totalUsuarios() {
        return usuarioRepo.total();
    }
}
