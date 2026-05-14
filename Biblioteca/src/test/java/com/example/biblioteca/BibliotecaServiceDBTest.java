package com.example.biblioteca;

import com.example.biblioteca.db.DatabaseConnection;
import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.repository.EmprestimoRepository;
import com.example.biblioteca.repository.LivroRepositoryDB;
import com.example.biblioteca.repository.UsuarioRepository;
import com.example.biblioteca.service.BibliotecaServiceDB;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BibliotecaServiceDBTest {

    private static Connection            conn;
    private static BibliotecaServiceDB   service;
    private static LivroRepositoryDB     livroRepo;
    private static EmprestimoRepository  empRepo;

    private static final LocalDate HOJE   = LocalDate.of(2025, 6, 15);
    private static final LocalDate ANTIGO = LocalDate.of(2025, 5, 1);

    @BeforeAll
    static void iniciar() throws Exception {
        conn      = DatabaseConnection.novaConexao("jdbc:sqlite::memory:");
        livroRepo = new LivroRepositoryDB(conn);
        empRepo   = new EmprestimoRepository(conn, livroRepo);
        UsuarioRepository usuRepo = new UsuarioRepository(conn);
        service   = new BibliotecaServiceDB(livroRepo, empRepo, usuRepo);
    }

    @AfterAll
    static void fechar() throws Exception {
        if (conn != null) conn.close();
    }

    @BeforeEach
    void limpar() throws Exception {
        conn.createStatement().execute("DELETE FROM emprestimos");
        conn.createStatement().execute("DELETE FROM livros");
        conn.createStatement().execute("DELETE FROM usuarios");
        // Popular dados base
        service.cadastrarLivro(new Livro("978-85-333-0001-1", "Dom Casmurro",   "Machado de Assis", 1899));
        service.cadastrarLivro(new Livro("978-85-333-0002-2", "O Cortico",       "Aluisio Azevedo",  1890));
        service.cadastrarLivro(new Livro("978-85-333-0003-3", "Quincas Borba",   "Machado de Assis", 1891));
        service.cadastrarUsuario(new Usuario("Admin", "admin", "admin123", "ADMIN"));
        service.cadastrarUsuario(new Usuario("Leitor", "leitor", "leitor123", "LEITOR"));
    }

    @Test
    @Order(1)
    void deveAutenticarUsuarioValido() {
        Usuario u = service.login("admin", "admin123");
        assertEquals("Admin", u.getNome());
        assertEquals("ADMIN", u.getPerfil());
    }

    @Test
    @Order(2)
    void deveRejeitarCredenciaisInvalidas() {
        assertThrows(IllegalArgumentException.class,
            () -> service.login("admin", "errada"));
    }

    @Test
    @Order(3)
    void deveCadastrarEListarLivros() {
        assertEquals(3, service.totalLivros());
        List<Livro> todos = service.listarTodosLivros();
        assertEquals(3, todos.size());
    }

    @Test
    @Order(4)
    void deveBuscarLivroPorIsbn() {
        assertTrue(service.buscarLivro("978-85-333-0001-1").isPresent());
        assertFalse(service.buscarLivro("000-nao-existe").isPresent());
    }

    @Test
    @Order(5)
    void deveBuscarPorAutor() {
        List<Livro> machado = service.buscarPorAutor("Machado");
        assertEquals(2, machado.size());
    }

    @Test
    @Order(6)
    void deveAtualizarLivro() {
        Livro atualizado = new Livro("978-85-333-0001-1", "Dom Casmurro Ed. 2", "Machado de Assis", 2020);
        service.atualizarLivro(atualizado);
        Livro salvo = service.buscarLivro("978-85-333-0001-1").orElseThrow();
        assertEquals("Dom Casmurro Ed. 2", salvo.getTitulo());
        assertEquals(2020, salvo.getAnoPub());
    }

    @Test
    @Order(7)
    void deveRemoverLivroSemEmprestimo() {
        service.removerLivro("978-85-333-0003-3");
        assertEquals(2, service.totalLivros());
    }

    @Test
    @Order(8)
    void deveRealizarEmprestimoEPersistir() {
        Emprestimo emp = service.realizarEmprestimo("978-85-333-0001-1", "Ana", HOJE);
        assertNotNull(emp);
        assertTrue(emp.getId().matches("EMP-\\d{4}"));

        Livro livro = service.buscarLivro("978-85-333-0001-1").orElseThrow();
        assertFalse(livro.isDisponivel());

        assertEquals(1, service.totalEmprestimosAtivos());
    }

    @Test
    @Order(9)
    void deveImpedirEmprestimoDuploComBanco() {
        service.realizarEmprestimo("978-85-333-0001-1", "Ana", HOJE);
        assertThrows(IllegalStateException.class,
            () -> service.realizarEmprestimo("978-85-333-0001-1", "Bruno", HOJE));
    }

    @Test
    @Order(10)
    void deveLancarExcecaoParaIsbnNaoEncontrado() {
        assertThrows(IllegalArgumentException.class,
            () -> service.realizarEmprestimo("000-invalido", "Ana", HOJE));
    }

    @Test
    @Order(11)
    void deveRegistrarDevolucaoELiberarLivroNoBanco() {
        Emprestimo emp = service.realizarEmprestimo("978-85-333-0001-1", "Carla", HOJE);
        assertFalse(service.buscarLivro("978-85-333-0001-1").orElseThrow().isDisponivel());

        service.registrarDevolucao(emp.getId(), HOJE.plusDays(7));

        assertTrue(service.buscarLivro("978-85-333-0001-1").orElseThrow().isDisponivel());
        assertEquals(0, service.totalEmprestimosAtivos());
    }

    @Test
    @Order(12)
    void deveListarEmprestimosEmAtraso() {
        service.realizarEmprestimo("978-85-333-0001-1", "Diego", ANTIGO);
        service.realizarEmprestimo("978-85-333-0002-2", "Elisa", HOJE);

        List<Emprestimo> atrasados = service.listarEmAtraso(HOJE);
        assertEquals(1, atrasados.size());
        assertEquals("Diego", atrasados.get(0).getNomeUsuario());
    }

    @Test
    @Order(13)
    void deveRetornarHistoricoDoUsuario() {
        Emprestimo e1 = service.realizarEmprestimo("978-85-333-0001-1", "Fabio", HOJE);
        service.registrarDevolucao(e1.getId(), HOJE.plusDays(5));
        service.realizarEmprestimo("978-85-333-0002-2", "Fabio", HOJE.plusDays(6));

        List<Emprestimo> historico = service.historicoUsuario("Fabio");
        assertEquals(2, historico.size());
    }

    @Test
    @Order(14)
    void deveListarSomenteDisponiveisAposEmprestimo() {
        service.realizarEmprestimo("978-85-333-0001-1", "Gabi", HOJE);
        List<Livro> disponiveis = service.listarDisponiveis();
        assertEquals(2, disponiveis.size());
        assertTrue(disponiveis.stream().noneMatch(l -> l.getIsbn().equals("978-85-333-0001-1")));
    }
}
