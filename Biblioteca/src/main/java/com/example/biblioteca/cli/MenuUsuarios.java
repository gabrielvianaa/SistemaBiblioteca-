package com.example.biblioteca.cli;

import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.repository.UsuarioRepository;

import java.sql.Connection;
import java.util.List;
import java.sql.*;

/**
 * Menu CLI de CRUD completo de Usuarios.
 * Acesso restrito a perfil ADMIN.
 *
 * Operacoes:
 *   [C] Cadastrar usuario
 *   [R] Listar usuarios
 *   [U] Alterar senha
 *   [D] Remover usuario
 */
public class MenuUsuarios {

    private final UsuarioRepository repo;
    private final Connection        conn;

    public MenuUsuarios(Connection conn) {
        this.conn = conn;
        this.repo = new UsuarioRepository(conn);
    }

    public void exibir() {
        boolean voltar = false;
        while (!voltar) {
            Console.titulo("GERENCIAMENTO DE USUARIOS  [Apenas ADMIN]");
            Console.opcao(1, "Cadastrar novo usuario");
            Console.opcao(2, "Listar todos os usuarios");
            Console.opcao(3, "Alterar senha de usuario");
            Console.opcao(4, "Remover usuario");
            Console.opcao(0, "Voltar ao menu principal");
            System.out.println();

            switch (Console.lerOpcao(0, 4)) {
                case 1 -> cadastrar();
                case 2 -> listar();
                case 3 -> alterarSenha();
                case 4 -> remover();
                case 0 -> voltar = true;
            }
        }
    }

    // ── C: Cadastrar ──────────────────────────────────────────────────────────

    private void cadastrar() {
        Console.titulo("CADASTRAR NOVO USUARIO");
        try {
            String nome  = Console.lerTextoObrigatorio("Nome completo");
            String login = Console.lerTextoObrigatorio("Login");
            String senha = Console.lerTextoObrigatorio("Senha");

            Console.info("Perfis disponiveis: ADMIN | BIBLIOTECARIO | LEITOR");
            String perfil = Console.lerTextoObrigatorio("Perfil").toUpperCase();

            repo.cadastrar(new Usuario(nome, login, senha, perfil));

            Console.sucesso("Usuario cadastrado com sucesso!");
            Console.info("Nome   : " + nome);
            Console.info("Login  : " + login);
            Console.info("Perfil : " + perfil);

        } catch (IllegalArgumentException e) {
            Console.erro(e.getMessage());
        }
        Console.pausar();
    }

    // ── R: Listar ─────────────────────────────────────────────────────────────

    private void listar() {
        Console.titulo("USUARIOS CADASTRADOS");
        try {
            List<Usuario> lista = buscarTodos();
            if (lista.isEmpty()) {
                Console.aviso("Nenhum usuario cadastrado.");
            } else {
                Console.separador();
                System.out.printf("  %-4s %-24s %-16s %-14s%n",
                        "ID", "NOME", "LOGIN", "PERFIL");
                Console.separador();
                lista.forEach(u ->
                    System.out.printf("  %-4d %-24s %-16s %-14s%n",
                            u.getId(), u.getNome(), u.getLogin(), u.getPerfil())
                );
                Console.separador();
                Console.info("Total: " + lista.size() + " usuario(s).");
            }
        } catch (Exception e) {
            Console.erro("Erro ao listar usuarios: " + e.getMessage());
        }
        Console.pausar();
    }

    // ── U: Alterar senha ──────────────────────────────────────────────────────

    private void alterarSenha() {
        Console.titulo("ALTERAR SENHA DE USUARIO");
        try {
            String login    = Console.lerTextoObrigatorio("Login do usuario");
            String novaSenha = Console.lerTextoObrigatorio("Nova senha");

            repo.atualizarSenha(login, novaSenha);
            Console.sucesso("Senha alterada com sucesso para o login: " + login);

        } catch (IllegalArgumentException e) {
            Console.erro(e.getMessage());
        }
        Console.pausar();
    }

    // ── D: Remover ────────────────────────────────────────────────────────────

    private void remover() {
        Console.titulo("REMOVER USUARIO");
        try {
            String login = Console.lerTextoObrigatorio("Login do usuario a remover");

            repo.buscarPorLogin(login).ifPresentOrElse(u -> {
                Console.info("Nome   : " + u.getNome());
                Console.info("Perfil : " + u.getPerfil());
                System.out.println();

                if (Console.confirmar("Confirma a remocao deste usuario?")) {
                    repo.remover(login);
                    Console.sucesso("Usuario removido com sucesso!");
                } else {
                    Console.aviso("Operacao cancelada.");
                }
            }, () -> Console.aviso("Usuario nao encontrado: " + login));

        } catch (IllegalArgumentException e) {
            Console.erro(e.getMessage());
        }
        Console.pausar();
    }

    // ── Utilitario ────────────────────────────────────────────────────────────

    private List<Usuario> buscarTodos() throws SQLException {
        String sql = "SELECT * FROM usuarios ORDER BY perfil, nome";
        List<Usuario> lista = new java.util.ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Usuario(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("login"),
                    rs.getString("senha"),
                    rs.getString("perfil")
                ));
            }
        }
        return lista;
    }
}
