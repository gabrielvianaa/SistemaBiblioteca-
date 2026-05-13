package com.example.biblioteca.model;

import com.example.biblioteca.model.Livro;

import java.time.LocalDate;

public class Emprestimo {
    private final String id;
    private final Livro livro;
    private final String nomeUsuario;
    private final LocalDate dataEmprestimo;
    private LocalDate dataDevolucao;
    private boolean devolvido;

    public Emprestimo(String id, Livro livro, String nomeUsuario, LocalDate dataEmprestimo) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("ID invalido");
        if (livro == null) throw new IllegalArgumentException("Livro nulo");
        if (nomeUsuario == null || nomeUsuario.isBlank()) throw new IllegalArgumentException("Nome invalido");
        if (dataEmprestimo == null) throw new IllegalArgumentException("Data invalida");
        this.id = id;
        this.livro = livro;
        this.nomeUsuario = nomeUsuario;
        this.dataEmprestimo = dataEmprestimo;
        this.devolvido = false;
    }

    public String getId() { return id; }
    public Livro getLivro() { return livro; }
    public String getNomeUsuario() { return nomeUsuario; }
    public LocalDate getDataEmprestimo() { return dataEmprestimo; }
    public LocalDate getDataDevolucao() { return dataDevolucao; }
    public boolean isDevolvido() { return devolvido; }

    public void registrarDevolucao(LocalDate data) {
        if (devolvido) throw new IllegalStateException("Ja devolvido");
        if (data.isBefore(dataEmprestimo)) throw new IllegalArgumentException("Data invalida");
        this.dataDevolucao = data;
        this.devolvido = true;
        this.livro.setDisponivel(true);
    }

    public long calcularDiasAtraso(LocalDate hoje) {
        LocalDate prazo = dataEmprestimo.plusDays(14);
        if (!hoje.isAfter(prazo)) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(prazo, hoje);
    }
}