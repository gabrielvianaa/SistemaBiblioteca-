package com.example.biblioteca;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class EmprestimoTest {

    private Livro livro;
    private static final LocalDate DATA_EMP = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        livro = new Livro("978-85-333-0001-1", "Dom Casmurro", "Machado de Assis", 1899);
    }

    @Test
    void deveCriarEmprestimoValido() {
        Emprestimo emp = new Emprestimo("EMP-0001", livro, "Ana", DATA_EMP);
        assertEquals("EMP-0001", emp.getId());
        assertEquals("Ana", emp.getNomeUsuario());
        assertEquals(DATA_EMP, emp.getDataEmprestimo());
        assertFalse(emp.isDevolvido());
        assertNull(emp.getDataDevolucao());
    }

    @Test
    void deveLancarExcecaoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo(null, livro, "Ana", DATA_EMP));
    }

    @Test
    void deveLancarExcecaoIdVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo("", livro, "Ana", DATA_EMP));
    }

    @Test
    void deveLancarExcecaoLivroNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo("EMP-0001", null, "Ana", DATA_EMP));
    }

    @Test
    void deveLancarExcecaoUsuarioVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo("EMP-0001", livro, "", DATA_EMP));
    }

    @Test
    void deveCalcularDiasDeAtrasoCorretamente() {
        // Empréstimo em 2025-05-01 → prazo 2025-05-15 → hoje 2025-06-01 = 17 dias de atraso
        Livro outro = new Livro("978-85-333-0002-2", "O Cortico", "Aluisio Azevedo", 1890);
        Emprestimo emp = new Emprestimo("EMP-0002", outro, "Bruno", LocalDate.of(2025, 5, 1));
        assertEquals(17, emp.calcularDiasAtraso(LocalDate.of(2025, 6, 1)));
    }

    @Test
    void naoDeveCalcularAtrasoNoPrazo() {
        Emprestimo emp = new Emprestimo("EMP-0003", livro, "Carla", DATA_EMP);
        assertEquals(0, emp.calcularDiasAtraso(DATA_EMP.plusDays(10)));
    }

    @Test
    void deveRegistrarDevolucao() {
        Emprestimo emp = new Emprestimo("EMP-0004", livro, "Diego", DATA_EMP);
        emp.registrarDevolucao(DATA_EMP.plusDays(5));
        assertTrue(emp.isDevolvido());
        assertEquals(DATA_EMP.plusDays(5), emp.getDataDevolucao());
        assertTrue(livro.isDisponivel());
    }

    @Test
    void deveLancarExcecaoDataDevolucaoAnteriorAoEmprestimo() {
        Emprestimo emp = new Emprestimo("EMP-0005", livro, "Elisa", DATA_EMP);
        assertThrows(IllegalArgumentException.class,
                () -> emp.registrarDevolucao(DATA_EMP.minusDays(1)));
    }

    @Test
    void deveLancarExcecaoSegundaDevolucao() {
        Emprestimo emp = new Emprestimo("EMP-0006", livro, "Fabio", DATA_EMP);
        emp.registrarDevolucao(DATA_EMP.plusDays(3));
        assertThrows(IllegalStateException.class,
                () -> emp.registrarDevolucao(DATA_EMP.plusDays(5)));
    }
}
