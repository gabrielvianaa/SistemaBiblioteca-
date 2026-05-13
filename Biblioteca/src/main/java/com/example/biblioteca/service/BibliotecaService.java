package com.example.biblioteca.service;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.repository.LivroRepository;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class BibliotecaService {
    private final LivroRepository repository;
    private final Map<String, Emprestimo> emprestimos = new LinkedHashMap<>();
    private int contador = 1;

    public BibliotecaService(LivroRepository repository) {
        this.repository = repository;
    }

    public void cadastrarLivro(Livro livro) { repository.adicionar(livro); }

    public Optional<Livro> buscarLivro(String isbn) { return repository.buscarPorIsbn(isbn); }

    public List<Livro> buscarPorAutor(String autor) { return repository.buscarPorAutor(autor); }

    public List<Livro> listarDisponiveis() { return repository.listarDisponiveis(); }

    public Emprestimo realizarEmprestimo(String isbn, String usuario, LocalDate data) {
        Livro livro = repository.buscarPorIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Livro nao encontrado: " + isbn));
        if (!livro.isDisponivel())
            throw new IllegalStateException("Livro indisponivel: " + livro.getTitulo());
        String id = "EMP-" + String.format("%04d", contador++);
        Emprestimo emp = new Emprestimo(id, livro, usuario, data);
        livro.setDisponivel(false);
        emprestimos.put(id, emp);
        return emp;
    }

    public void registrarDevolucao(String id, LocalDate data) {
        Emprestimo emp = emprestimos.get(id);
        if (emp == null) throw new IllegalArgumentException("Emprestimo nao encontrado: " + id);
        emp.registrarDevolucao(data);
    }

    public List<Emprestimo> listarEmAtraso(LocalDate hoje) {
        return emprestimos.values().stream()
                .filter(e -> !e.isDevolvido() && e.calcularDiasAtraso(hoje) > 0)
                .collect(Collectors.toList());
    }

    public List<Emprestimo> historicoUsuario(String usuario) {
        return emprestimos.values().stream()
                .filter(e -> e.getNomeUsuario().equalsIgnoreCase(usuario))
                .collect(Collectors.toList());
    }

    public int totalLivros() { return repository.total(); }
    public int totalEmprestimosAtivos() {
        return (int) emprestimos.values().stream().filter(e -> !e.isDevolvido()).count();
    }
}