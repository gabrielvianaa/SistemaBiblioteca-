package com.example.biblioteca.repository;

import com.example.biblioteca.model.Livro;
import java.sql.*;
import java.util.*;

public class LivroRepositoryDB {

    private final Connection conn;

    public LivroRepositoryDB(Connection conn) {
        this.conn = conn;
    }

    public void adicionar(Livro livro) {
        // Verifica duplicata antes de inserir (mensagem de erro clara)
        if (buscarPorIsbn(livro.getIsbn()).isPresent())
            throw new IllegalArgumentException("ISBN ja existe: " + livro.getIsbn());

        String sql = "INSERT INTO livros (isbn, titulo, autor, ano_pub, disponivel) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, livro.getIsbn());
            ps.setString(2, livro.getTitulo());
            ps.setString(3, livro.getAutor());
            ps.setInt   (4, livro.getAnoPub());
            ps.setInt   (5, livro.isDisponivel() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao cadastrar livro: " + e.getMessage(), e);
        }
    }

    public Optional<Livro> buscarPorIsbn(String isbn) {
        String sql = "SELECT * FROM livros WHERE isbn = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapearLivro(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar livro: " + e.getMessage(), e);
        }
    }

    public List<Livro> buscarPorAutor(String autor) {
        String sql = "SELECT * FROM livros WHERE autor LIKE ? COLLATE NOCASE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + autor + "%");
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar por autor: " + e.getMessage(), e);
        }
    }

    public List<Livro> listarDisponiveis() {
        String sql = "SELECT * FROM livros WHERE disponivel = 1 ORDER BY titulo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar disponiveis: " + e.getMessage(), e);
        }
    }

    public List<Livro> listarTodos() {
        String sql = "SELECT * FROM livros ORDER BY titulo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar livros: " + e.getMessage(), e);
        }
    }

    public int total() {
        String sql = "SELECT COUNT(*) FROM livros";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar livros: " + e.getMessage(), e);
        }
    }

    public void atualizarDisponibilidade(String isbn, boolean disponivel) {
        String sql = "UPDATE livros SET disponivel = ? WHERE isbn = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt   (1, disponivel ? 1 : 0);
            ps.setString(2, isbn);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar disponibilidade: " + e.getMessage(), e);
        }
    }

    public void atualizar(Livro livro) {
        if (buscarPorIsbn(livro.getIsbn()).isEmpty())
            throw new IllegalArgumentException("Livro nao encontrado: " + livro.getIsbn());

        String sql = "UPDATE livros SET titulo = ?, autor = ?, ano_pub = ? WHERE isbn = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, livro.getTitulo());
            ps.setString(2, livro.getAutor());
            ps.setInt   (3, livro.getAnoPub());
            ps.setString(4, livro.getIsbn());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar livro: " + e.getMessage(), e);
        }
    }

    public void remover(String isbn) {
        if (buscarPorIsbn(isbn).isEmpty())
            throw new IllegalArgumentException("Livro nao encontrado: " + isbn);

        String sql = "DELETE FROM livros WHERE isbn = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Chave estrangeira impediu a remocao (emprestimo ativo)
            if (e.getMessage().contains("FOREIGN KEY"))
                throw new IllegalStateException(
                    "Nao e possivel remover livro com emprestimo ativo: " + isbn);
            throw new RuntimeException("Erro ao remover livro: " + e.getMessage(), e);
        }
    }

    private Livro mapearLivro(ResultSet rs) throws SQLException {
        Livro l = new Livro(
            rs.getString("isbn"),
            rs.getString("titulo"),
            rs.getString("autor"),
            rs.getInt("ano_pub")
        );
        l.setDisponivel(rs.getInt("disponivel") == 1);
        return l;
    }

    private List<Livro> executarListagem(PreparedStatement ps) throws SQLException {
        List<Livro> lista = new ArrayList<>();
        ResultSet rs = ps.executeQuery();
        while (rs.next()) lista.add(mapearLivro(rs));
        return lista;
    }
}
