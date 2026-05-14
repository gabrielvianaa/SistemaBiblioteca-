package com.example.biblioteca;

import com.example.biblioteca.db.DatabaseConnection;
import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integracao do UsuarioRepository.
 * Usa SQLite em memoria para isolamento total.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UsuarioRepositoryTest {

    private static Connection         conn;
    private static UsuarioRepository  repo;

    @BeforeAll
    static void iniciar() throws Exception {
        conn = DatabaseConnection.novaConexao("jdbc:sqlite::memory:");
        repo = new UsuarioRepository(conn);
    }

    @AfterAll
    static void fechar() throws Exception {
        if (conn != null) conn.close();
    }

    @BeforeEach
    void limpar() throws Exception {
        conn.createStatement().execute("DELETE FROM usuarios");
    }

    @Test
    @Order(1)
    void deveCadastrarUsuario() {
        repo.cadastrar(new Usuario("Admin", "admin", "senha123", "ADMIN"));
        assertEquals(1, repo.total());
    }

    @Test
    @Order(2)
    void deveLancarExcecaoLoginDuplicado() {
        repo.cadastrar(new Usuario("Admin", "admin", "senha123", "ADMIN"));
        assertThrows(IllegalArgumentException.class,
            () -> repo.cadastrar(new Usuario("Outro", "admin", "outra", "LEITOR")));
    }

    @Test
    @Order(3)
    void deveBuscarUsuarioPorLogin() {
        repo.cadastrar(new Usuario("Maria", "maria", "maria123", "BIBLIOTECARIO"));
        Optional<Usuario> resultado = repo.buscarPorLogin("maria");
        assertTrue(resultado.isPresent());
        assertEquals("Maria", resultado.get().getNome());
        assertEquals("BIBLIOTECARIO", resultado.get().getPerfil());
    }

    @Test
    @Order(4)
    void deveAutenticarComCredenciaisCorretas() {
        repo.cadastrar(new Usuario("Joao", "joao", "joao123", "LEITOR"));
        Optional<Usuario> resultado = repo.autenticar("joao", "joao123");
        assertTrue(resultado.isPresent());
        assertEquals("Joao", resultado.get().getNome());
    }

    @Test
    @Order(5)
    void deveRejeitarAutenticacaoComSenhaErrada() {
        repo.cadastrar(new Usuario("Joao", "joao", "joao123", "LEITOR"));
        Optional<Usuario> resultado = repo.autenticar("joao", "senhaErrada");
        assertFalse(resultado.isPresent());
    }

    @Test
    @Order(6)
    void deveRejeitarAutenticacaoLoginInexistente() {
        Optional<Usuario> resultado = repo.autenticar("naoexiste", "123");
        assertFalse(resultado.isPresent());
    }

    @Test
    @Order(7)
    void deveAtualizarSenha() {
        repo.cadastrar(new Usuario("Ana", "ana", "velha123", "LEITOR"));
        repo.atualizarSenha("ana", "nova456");

        // Senha velha deve falhar
        assertFalse(repo.autenticar("ana", "velha123").isPresent());
        // Senha nova deve funcionar
        assertTrue(repo.autenticar("ana", "nova456").isPresent());
    }


    @Test
    @Order(8)
    void deveRemoverUsuario() {
        repo.cadastrar(new Usuario("Bruno", "bruno", "bruno123", "LEITOR"));
        assertEquals(1, repo.total());
        repo.remover("bruno");
        assertEquals(0, repo.total());
    }

    @Test
    @Order(9)
    void deveLancarExcecaoAoRemoverLoginInexistente() {
        assertThrows(IllegalArgumentException.class,
            () -> repo.remover("naoexiste"));
    }

    @Test
    @Order(10)
    void deveLancarExcecaoPerfilInvalido() {
        assertThrows(IllegalArgumentException.class,
            () -> new Usuario("X", "x", "x123", "GERENTE")); // perfil invalido
    }
}
