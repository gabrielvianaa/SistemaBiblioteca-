package com.example.biblioteca;

import com.example.biblioteca.db.DatabaseConnection;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.repository.LivroRepositoryDB;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LivroRepositoryDBTest {

    private static Connection conn;
    private static LivroRepositoryDB repo;

    private static final Livro L1 = new Livro("978-85-333-0001-1", "Dom Casmurro",     "Machado de Assis", 1899);
    private static final Livro L2 = new Livro("978-85-333-0002-2", "O Cortico",         "Aluisio Azevedo",  1890);
    private static final Livro L3 = new Livro("978-85-333-0003-3", "Quincas Borba",     "Machado de Assis", 1891);

    @BeforeAll
    static void iniciarBanco() throws Exception {
        conn = DatabaseConnection.novaConexao("jdbc:sqlite::memory:");
        repo = new LivroRepositoryDB(conn);
    }

    @AfterAll
    static void fecharBanco() throws Exception {
        if (conn != null) conn.close();
    }

    @BeforeEach
    void limparTabela() throws Exception {
        conn.createStatement().execute("DELETE FROM livros");
    }

    @Test
    @Order(1)
    void deveCadastrarLivroNoBanco() {
        repo.adicionar(L1);
        assertEquals(1, repo.total());
    }

    @Test
    @Order(2)
    void deveLancarExcecaoIsbnDuplicadoNoBanco() {
        repo.adicionar(L1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> repo.adicionar(new Livro("978-85-333-0001-1", "Outro", "Autor", 2000)));
        assertTrue(ex.getMessage().contains("ISBN ja existe"));
    }

    @Test
    @Order(3)
    void deveBuscarLivroPorIsbn() {
        repo.adicionar(L1);
        Optional<Livro> resultado = repo.buscarPorIsbn("978-85-333-0001-1");
        assertTrue(resultado.isPresent());
        assertEquals("Dom Casmurro", resultado.get().getTitulo());
        assertEquals("Machado de Assis", resultado.get().getAutor());
        assertEquals(1899, resultado.get().getAnoPub());
    }

    @Test
    @Order(4)
    void deveRetornarVazioParaIsbnInexistente() {
        Optional<Livro> resultado = repo.buscarPorIsbn("000-nao-existe");
        assertFalse(resultado.isPresent());
    }

    @Test
    @Order(5)
    void deveBuscarPorAutorCaseInsensitive() {
        repo.adicionar(L1);
        repo.adicionar(L2);
        repo.adicionar(L3);
        List<Livro> machado = repo.buscarPorAutor("machado");  // minusculo
        assertEquals(2, machado.size());
        List<Livro> aluisio = repo.buscarPorAutor("ALUISIO"); // maiusculo
        assertEquals(1, aluisio.size());
    }

    @Test
    @Order(6)
    void deveListarSomenteDisponiveis() {
        repo.adicionar(L1);
        repo.adicionar(L2);
        repo.atualizarDisponibilidade("978-85-333-0001-1", false); // emprestado
        List<Livro> disponiveis = repo.listarDisponiveis();
        assertEquals(1, disponiveis.size());
        assertEquals("978-85-333-0002-2", disponiveis.get(0).getIsbn());
    }

    @Test
    @Order(7)
    void deveListarTodosOsLivros() {
        repo.adicionar(L1);
        repo.adicionar(L2);
        repo.adicionar(L3);
        assertEquals(3, repo.listarTodos().size());
    }

    @Test
    @Order(8)
    void deveAtualizarDisponibilidade() {
        repo.adicionar(L1);
        repo.atualizarDisponibilidade("978-85-333-0001-1", false);
        Livro livro = repo.buscarPorIsbn("978-85-333-0001-1").orElseThrow();
        assertFalse(livro.isDisponivel());

        repo.atualizarDisponibilidade("978-85-333-0001-1", true);
        livro = repo.buscarPorIsbn("978-85-333-0001-1").orElseThrow();
        assertTrue(livro.isDisponivel());
    }

    @Test
    @Order(9)
    void deveAtualizarDadosDoLivro() {
        repo.adicionar(L1);
        Livro atualizado = new Livro("978-85-333-0001-1", "Dom Casmurro (Ed. Especial)", "Machado de Assis", 2000);
        repo.atualizar(atualizado);
        Livro salvo = repo.buscarPorIsbn("978-85-333-0001-1").orElseThrow();
        assertEquals("Dom Casmurro (Ed. Especial)", salvo.getTitulo());
        assertEquals(2000, salvo.getAnoPub());
    }

    @Test
    @Order(10)
    void deveRemoverLivro() {
        repo.adicionar(L1);
        assertEquals(1, repo.total());
        repo.remover("978-85-333-0001-1");
        assertEquals(0, repo.total());
        assertFalse(repo.buscarPorIsbn("978-85-333-0001-1").isPresent());
    }

    @Test
    @Order(11)
    void deveLancarExcecaoAoRemoverIsbnInexistente() {
        assertThrows(IllegalArgumentException.class,
            () -> repo.remover("000-nao-existe"));
    }
}
