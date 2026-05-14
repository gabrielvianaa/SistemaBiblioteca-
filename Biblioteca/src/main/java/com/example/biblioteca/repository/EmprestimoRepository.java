package com.example.biblioteca.repository;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;


public class EmprestimoRepository {

    private final Connection conn;
    private final LivroRepositoryDB livroRepo;

    public EmprestimoRepository(Connection conn, LivroRepositoryDB livroRepo) {
        this.conn      = conn;
        this.livroRepo = livroRepo;
    }

    public void salvar(Emprestimo emp) {
        String sql = """
            INSERT INTO emprestimos
                (id, isbn, nome_usuario, data_emprestimo, data_devolucao, devolvido)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, emp.getId());
            ps.setString(2, emp.getLivro().getIsbn());
            ps.setString(3, emp.getNomeUsuario());
            ps.setString(4, emp.getDataEmprestimo().toString());
            ps.setString(5, emp.getDataDevolucao() != null
                             ? emp.getDataDevolucao().toString() : null);
            ps.setInt   (6, emp.isDevolvido() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar emprestimo: " + e.getMessage(), e);
        }
    }

    public Optional<Emprestimo> buscarPorId(String id) {
        String sql = "SELECT * FROM emprestimos WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar emprestimo: " + e.getMessage(), e);
        }
    }

    public List<Emprestimo> listarAtivos() {
        String sql = "SELECT * FROM emprestimos WHERE devolvido = 0 ORDER BY data_emprestimo";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar ativos: " + e.getMessage(), e);
        }
    }

    public List<Emprestimo> listarTodos() {
        String sql = "SELECT * FROM emprestimos ORDER BY data_emprestimo DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar emprestimos: " + e.getMessage(), e);
        }
    }

    public List<Emprestimo> listarEmAtraso(LocalDate hoje) {
        String prazo = hoje.minusDays(14).toString();
        String sql   = """
            SELECT * FROM emprestimos
            WHERE devolvido = 0
              AND data_emprestimo < ?
            ORDER BY data_emprestimo
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prazo);
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar em atraso: " + e.getMessage(), e);
        }
    }

    public List<Emprestimo> historicoUsuario(String usuario) {
        String sql = "SELECT * FROM emprestimos WHERE nome_usuario = ? COLLATE NOCASE ORDER BY data_emprestimo DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario);
            return executarListagem(ps);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar historico: " + e.getMessage(), e);
        }
    }

    public int totalAtivos() {
        String sql = "SELECT COUNT(*) FROM emprestimos WHERE devolvido = 0";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao contar ativos: " + e.getMessage(), e);
        }
    }

    public int proximoContador() {
        String sql = "SELECT COUNT(*) FROM emprestimos";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.getInt(1) + 1;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter contador: " + e.getMessage(), e);
        }
    }


    public void registrarDevolucao(String id, LocalDate dataDevolucao) {
        String sql = "UPDATE emprestimos SET data_devolucao = ?, devolvido = 1 WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataDevolucao.toString());
            ps.setString(2, id);
            int rows = ps.executeUpdate();
            if (rows == 0)
                throw new IllegalArgumentException("Emprestimo nao encontrado: " + id);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar devolucao: " + e.getMessage(), e);
        }
    }


    private Emprestimo mapear(ResultSet rs) throws SQLException {
        String isbn = rs.getString("isbn");
        Livro livro = livroRepo.buscarPorIsbn(isbn)
            .orElseThrow(() -> new RuntimeException("Livro nao encontrado no banco: " + isbn));

        Emprestimo emp = new Emprestimo(
            rs.getString("id"),
            livro,
            rs.getString("nome_usuario"),
            LocalDate.parse(rs.getString("data_emprestimo"))
        );

        // Reconstitui estado de devolucao sem passar pela logica de negocio
        String dataDev = rs.getString("data_devolucao");
        if (rs.getInt("devolvido") == 1 && dataDev != null) {
            emp.registrarDevolucao(LocalDate.parse(dataDev));
        }

        return emp;
    }

    private List<Emprestimo> executarListagem(PreparedStatement ps) throws SQLException {
        List<Emprestimo> lista = new ArrayList<>();
        ResultSet rs = ps.executeQuery();
        while (rs.next()) lista.add(mapear(rs));
        return lista;
    }
}
