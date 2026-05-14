package com.example.biblioteca;

import com.example.biblioteca.model.Livro;
import com.example.biblioteca.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LivroRepositoryTest {

    private LivroRepository repo;

    @BeforeEach
    void setUp() {
        repo = new LivroRepository();
        repo.adicionar(new Livro("978-85-333-0001-1", "Dom Casmurro",  "Machado de Assis", 1899));
        repo.adicionar(new Livro("978-85-333-0002-2", "O Cortico",     "Aluisio Azevedo",  1890));
    }

    @Test
    void deveAdicionarEContarLivros() {
        assertEquals(2, repo.total());
    }

    @Test
    void deveLancarExcecaoIsbnDuplicado() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.adicionar(new Livro("978-85-333-0001-1", "Outro", "Autor", 2000)));
    }

    @Test
    void deveBuscarPorIsbnExistente() {
        Optional<Livro> resultado = repo.buscarPorIsbn("978-85-333-0001-1");
        assertTrue(resultado.isPresent());
        assertEquals("Dom Casmurro", resultado.get().getTitulo());
    }

    @Test
    void deveRetornarVazioParaIsbnInexistente() {
        Optional<Livro> resultado = repo.buscarPorIsbn("000-nao-existe");
        assertFalse(resultado.isPresent());
    }

    @Test
    void deveListarTodosOsLivros() {
        List<Livro> todos = repo.listarTodos();
        assertEquals(2, todos.size());
    }

    @Test
    void deveBuscarPorAutorCaseInsensitive() {
        List<Livro> resultado = repo.buscarPorAutor("machado");
        assertEquals(1, resultado.size());
        assertEquals("Dom Casmurro", resultado.get(0).getTitulo());
    }

    @Test
    void deveListarSomenteDisponiveis() {
        repo.buscarPorIsbn("978-85-333-0001-1")
                .ifPresent(l -> l.setDisponivel(false));

        List<Livro> disponiveis = repo.listarDisponiveis();
        assertEquals(1, disponiveis.size());
        assertEquals("978-85-333-0002-2", disponiveis.get(0).getIsbn());
    }
}
