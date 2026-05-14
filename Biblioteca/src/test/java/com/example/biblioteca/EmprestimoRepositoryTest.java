package com.example.biblioteca;

import com.example.biblioteca.db.DatabaseConnection;
import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.repository.EmprestimoRepository;
import com.example.biblioteca.repository.LivroRepositoryDB;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integracao do EmprestimoRepository.
 * Usa SQLite em memoria para isolamento total.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EmprestimoRepositoryTest {

    private static Connection            conn;
    private static LivroRepositoryDB     livroRepo;
    private static EmprestimoRepository  empRepo;

    private static final LocalDate HOJE   = LocalDate.of(2025, 6, 15);
    private static final LocalDate ANTIGO = LocalDate.of(2025, 5, 1); // 45 dias antes — em atraso

    private static final Livro L1 = new Livro("978-85-333-0001-1", "Dom Casmurro",   "Machado de Assis", 1899);
    private static final Livro L2 = new Livro("978-85-333-0002-2", "O Cortico",       "Aluisio Azevedo",  1890);

    @BeforeAll
    static void iniciar() throws Exception {
        conn      = DatabaseConnection.novaConexao("jdbc:sqlite::memory:");
        livroRepo = new LivroRepositoryDB(conn);
        empRepo   = new EmprestimoRepository(conn, livroRepo);
        // Inserir livros necessarios para os testes
        livroRepo.adicionar(L1);
        livroRepo.adicionar(L2);
    }

    @AfterAll
    static void fechar() throws Exception {
        if (conn != null) conn.close();
    }

    @BeforeEach
    void limpar() throws Exception {
        conn.createStatement().execute("DELETE FROM emprestimos");
        // Restaurar disponibilidade dos livros
        livroRepo.atualizarDisponibilidade("978-85-333-0001-1", true);
        livroRepo.atualizarDisponibilidade("978-85-333-0002-2", true);
    }

    private Emprestimo criarEmprestimo(String isbn, String usuario, LocalDate data) {
        Livro livro = livroRepo.buscarPorIsbn(isbn).orElseThrow();
        int counter = empRepo.proximoContador();
        Emprestimo emp = new Emprestimo("EMP-" + String.format("%04d", counter), livro, usuario, data);
        empRepo.salvar(emp);
        livroRepo.atualizarDisponibilidade(isbn, false);
        return emp;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void deveSalvarEmprestimoNoBanco() {
        Emprestimo emp = criarEmprestimo("978-85-333-0001-1", "Ana", HOJE);
        assertTrue(empRepo.buscarPorId(emp.getId()).isPresent());
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    void deveBuscarEmprestimoPorId() {
        Emprestimo emp = criarEmprestimo("978-85-333-0001-1", "Bruno", HOJE);
        Emprestimo recuperado = empRepo.buscarPorId(emp.getId()).orElseThrow();
        assertEquals(emp.getId(), recuperado.getId());
        assertEquals("Bruno", recuperado.getNomeUsuario());
        assertEquals("978-85-333-0001-1", recuperado.getLivro().getIsbn());
    }

    @Test
    @Order(3)
    void deveListarEmprestimosAtivos() {
        criarEmprestimo("978-85-333-0001-1", "Carla", HOJE);
        criarEmprestimo("978-85-333-0002-2", "Diego", HOJE);
        List<Emprestimo> ativos = empRepo.listarAtivos();
        assertEquals(2, ativos.size());
    }

    @Test
    @Order(4)
    void deveListarEmprestimosEmAtraso() {
        // Emprestimo antigo — em atraso
        criarEmprestimo("978-85-333-0001-1", "Elisa", ANTIGO);
        // Emprestimo recente — dentro do prazo
        livroRepo.atualizarDisponibilidade("978-85-333-0002-2", true);
        criarEmprestimo("978-85-333-0002-2", "Fabio", HOJE);

        List<Emprestimo> atrasados = empRepo.listarEmAtraso(HOJE);
        assertEquals(1, atrasados.size());
        assertEquals("Elisa", atrasados.get(0).getNomeUsuario());
    }

    @Test
    @Order(5)
    void deveListarHistoricoDoUsuario() {
        criarEmprestimo("978-85-333-0001-1", "Gabi", HOJE);

        // Devolver L1 para poder emprestar de novo
        empRepo.registrarDevolucao(empRepo.listarAtivos().get(0).getId(), HOJE.plusDays(3));
        livroRepo.atualizarDisponibilidade("978-85-333-0001-1", true);

        criarEmprestimo("978-85-333-0001-1", "Gabi", HOJE.plusDays(5));

        List<Emprestimo> historico = empRepo.historicoUsuario("gabi"); // minusculo
        assertEquals(2, historico.size());
    }

    @Test
    @Order(6)
    void deveContarEmprestimosAtivos() {
        criarEmprestimo("978-85-333-0001-1", "Helio", HOJE);
        criarEmprestimo("978-85-333-0002-2", "Iara",  HOJE);
        assertEquals(2, empRepo.totalAtivos());
    }

    // ── UPDATE (devolucao) ────────────────────────────────────────────────────

    @Test
    @Order(7)
    void deveRegistrarDevolucaoNoBanco() {
        Emprestimo emp = criarEmprestimo("978-85-333-0001-1", "Julia", HOJE);
        empRepo.registrarDevolucao(emp.getId(), HOJE.plusDays(5));

        Emprestimo recuperado = empRepo.buscarPorId(emp.getId()).orElseThrow();
        assertTrue(recuperado.isDevolvido());
        assertEquals(HOJE.plusDays(5), recuperado.getDataDevolucao());
    }

    @Test
    @Order(8)
    void emprestimoDevolvido_naoAparece_naListaDeAtivos() {
        Emprestimo emp = criarEmprestimo("978-85-333-0001-1", "Lucas", HOJE);
        assertEquals(1, empRepo.totalAtivos());

        empRepo.registrarDevolucao(emp.getId(), HOJE.plusDays(3));
        assertEquals(0, empRepo.totalAtivos());
    }

    @Test
    @Order(9)
    void deveLancarExcecaoAoDevolver_EmprestimoInexistente() {
        assertThrows(IllegalArgumentException.class,
            () -> empRepo.registrarDevolucao("EMP-9999", HOJE));
    }
}
