package com.example.biblioteca.model;

/**
 * Entidade Usuario.
 *
 * Perfis disponiveis:
 *   ADMIN         — acesso total, pode gerenciar usuarios
 *   BIBLIOTECARIO — gerencia livros e emprestimos
 *   LEITOR        — apenas consulta e solicita emprestimos
 */
public class Usuario {

    private final int    id;
    private final String nome;
    private final String login;
    private       String senha;
    private final String perfil;

    /** Construtor completo — usado ao recuperar do banco. */
    public Usuario(int id, String nome, String login, String senha, String perfil) {
        validar(nome, login, senha, perfil);
        this.id     = id;
        this.nome   = nome;
        this.login  = login;
        this.senha  = senha;
        this.perfil = perfil;
    }

    /** Construtor sem ID — usado ao cadastrar novo usuario (ID gerado pelo banco). */
    public Usuario(String nome, String login, String senha, String perfil) {
        this(0, nome, login, senha, perfil);
    }

    private void validar(String nome, String login, String senha, String perfil) {
        if (nome  == null || nome.isBlank())  throw new IllegalArgumentException("Nome invalido");
        if (login == null || login.isBlank()) throw new IllegalArgumentException("Login invalido");
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("Senha invalida");
        if (!perfil.equals("ADMIN") && !perfil.equals("BIBLIOTECARIO") && !perfil.equals("LEITOR"))
            throw new IllegalArgumentException("Perfil invalido: " + perfil);
    }

    public int    getId()     { return id; }
    public String getNome()   { return nome; }
    public String getLogin()  { return login; }
    public String getSenha()  { return senha; }
    public String getPerfil() { return perfil; }

    public void setSenha(String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank())
            throw new IllegalArgumentException("Senha invalida");
        this.senha = novaSenha;
    }

    @Override
    public String toString() {
        return String.format("Usuario{id=%d, nome='%s', login='%s', perfil='%s'}",
                             id, nome, login, perfil);
    }
}
