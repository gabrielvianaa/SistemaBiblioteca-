package com.example.biblioteca;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.repository.LivroRepository;
import com.example.biblioteca.service.BibliotecaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class BibliotecaServiceTest {

    private BibliotecaService service;
    private static final LocalDate HOJE = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        service = new BibliotecaService(new LivroRepository());
        service.cadastrarLivro(new Livro("978-85-333-0001-1", "Dom Casmurro", "Machado de Assis", 1899));
        service.cadastrarLivro(new Livro("978-85-333-0002-2", "O Cortico", "Aluisio Azevedo", 1890));
    }

    @Test
    void deveCadastrarLivro() {
        assertEquals(2, service.totalLivros());
    }

    @Test
    void deveRealizarEmprestimo() {
        Emprestimo emp = service.realizarEmprestimo("978-85-333-0001-1", "Ana", HOJE);
        assertNotNull(emp);
        assertFalse(emp.getLivro().isDisponivel());
    }

    @Test
    void naoDeveEmprestarLivroIndisponivel() {
        service.realizarEmprestimo("978-85-333-0001-1", "Ana", HOJE);
        assertThrows(IllegalStateException.class, () ->
                service.realizarEmprestimo("978-85-333-0001-1", "Bruno", HOJE));
    }

    @Test
    void deveRegistrarDevolucao() {
        Emprestimo emp = service.realizarEmprestimo("978-85-333-0001-1", "Ana", HOJE);
        service.registrarDevolucao(emp.getId(), HOJE.plusDays(5));
        assertTrue(emp.isDevolvido());
        assertTrue(emp.getLivro().isDisponivel());
    }
}