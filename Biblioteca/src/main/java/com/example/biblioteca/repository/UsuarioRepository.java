package com.example.biblioteca.repository;

import com.example.biblioteca.model.Usuario;
import java.sql.*;
import java.util.Optional;

/**
 * Repositorio de usuarios com persistencia em SQLite.
 * Responsavel por cadastro e autenticacao (login/senha).
 *
 * SEGURANCA: Em producao, a senha deve ser armazenada como hash (ex: BCrypt).
 * Neste projeto academico a senha e armazenada em texto simples para simplicidade.
 */
public class UsuarioRepository {

    private final Connection conn;

    public UsuarioRepository(Connection conn) {
        this.conn = conn;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /** Cadastra um novo usuario. Lanca excecao se o login ja existir. */
    public void cadastrar(Usuario usuario) {
        if (buscarPorLogin(usuario.getLogin()).isPresent())
            throw new IllegalArgumentException("Login ja existe: " + usuario.getLogin());

        String sql = "INSERT INTO usuarios (nome, login, senha, perfil) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getNome());
            ps.setString(2, usuario.getLogin());
            ps.setString(3, usuario.getSenha());
            ps.setString(4, usuario.getPerfil());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar usuario: " + e.getMessage(), e);
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    /** Busca usuario pelo login. */
    public Optional<Usuario> buscarPorLogin(String login) {
        String sql = "SELECT * FROM usuarios WHERE login = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuario: " + e.getMessage(), e);
        }
    }

    /**
     * Autentica um usuario pelo login e senha.
     * Retorna o Usuario se as credenciais forem validas, ou vazio caso contrario.
     */
    public Optional<Usuario> autenticar(String login, String senha) {
        String sql = "SELECT * FROM usuarios WHERE login = ? AND senha = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, senha);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao autenticar: " + e.getMessage(), e);
        }
    }

    /** Conta o total de usuarios cadastrados. */
    public int total() {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar usuarios: " + e.getMessage(), e);
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /** Atualiza a senha de um usuario. */
    public void atualizarSenha(String login, String novaSenha) {
        String sql = "UPDATE usuarios SET senha = ? WHERE login = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, novaSenha);
            ps.setString(2, login);
            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new IllegalArgumentException("Usuario nao encontrado: " + login);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar senha: " + e.getMessage(), e);
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /** Remove um usuario pelo login. */
    public void remover(String login) {
        String sql = "DELETE FROM usuarios WHERE login = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, login);
            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new IllegalArgumentException("Usuario nao encontrado: " + login);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover usuario: " + e.getMessage(), e);
        }
    }

    // ── Utilitario ────────────────────────────────────────────────────────────

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
            rs.getInt("id"),
            rs.getString("nome"),
            rs.getString("login"),
            rs.getString("senha"),
            rs.getString("perfil")
        );
    }
}
