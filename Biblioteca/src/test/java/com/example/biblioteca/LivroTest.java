package com.example.biblioteca;

import com.example.biblioteca.model.Livro;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LivroTest {

    @Test
    void deveCriarLivroValido() {
        Livro l = new Livro("978-85-333-0001-1", "Dom Casmurro", "Machado de Assis", 1899);
        assertEquals("Dom Casmurro", l.getTitulo());
        assertTrue(l.isDisponivel());
    }

    @Test
    void deveLancarExcecaoIsbnVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Livro("", "Titulo", "Autor", 2000));
    }

    @Test
    void deveLancarExcecaoAnoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Livro("123", "Titulo", "Autor", 99));
    }
}