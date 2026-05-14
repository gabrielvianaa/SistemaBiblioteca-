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

    // CT-U14 — Criar empréstimo com dados válidos
    @Test
    void deveCriarEmprestimoValido() {
        Emprestimo emp = new Emprestimo("EMP-0001", livro, "Ana", DATA_EMP);
        assertEquals("EMP-0001", emp.getId());
        assertEquals("Ana", emp.getNomeUsuario());
        assertEquals(DATA_EMP, emp.getDataEmprestimo());
        assertFalse(emp.isDevolvido());
        assertNull(emp.getDataDevolucao());
    }

    // CT-U15 — Lançar exceção com ID nulo
    @Test
    void deveLancarExcecaoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo(null, livro, "Ana", DATA_EMP));
    }

    // CT-U16 — Lançar exceção com ID vazio
    @Test
    void deveLancarExcecaoIdVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo("", livro, "Ana", DATA_EMP));
    }

    // CT-U17 — Lançar exceção com livro nulo
    @Test
    void deveLancarExcecaoLivroNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo("EMP-0001", null, "Ana", DATA_EMP));
    }

    // CT-U18 — Lançar exceção com usuário vazio
    @Test
    void deveLancarExcecaoUsuarioVazio() {
        assertThrows(IllegalArgumentException.class,
                () -> new Emprestimo("EMP-0001", livro, "", DATA_EMP));
    }

    // CT-U08 — Calcular dias de atraso corretamente (prazo = 14 dias)
    @Test
    void deveCalcularDiasDeAtrasoCorretamente() {
        // Empréstimo em 2025-05-01 → prazo 2025-05-15 → hoje 2025-06-01 = 17 dias de atraso
        Livro outro = new Livro("978-85-333-0002-2", "O Cortico", "Aluisio Azevedo", 1890);
        Emprestimo emp = new Emprestimo("EMP-0002", outro, "Bruno", LocalDate.of(2025, 5, 1));
        assertEquals(17, emp.calcularDiasAtraso(LocalDate.of(2025, 6, 1)));
    }

    // CT-U09 — Sem atraso quando dentro do prazo
    @Test
    void naoDeveCalcularAtrasoNoPrazo() {
        Emprestimo emp = new Emprestimo("EMP-0003", livro, "Carla", DATA_EMP);
        // 10 dias após o empréstimo ainda está dentro do prazo de 14 dias
        assertEquals(0, emp.calcularDiasAtraso(DATA_EMP.plusDays(10)));
    }

    // CT-U07 — Registrar devolução com sucesso
    @Test
    void deveRegistrarDevolucao() {
        Emprestimo emp = new Emprestimo("EMP-0004", livro, "Diego", DATA_EMP);
        emp.registrarDevolucao(DATA_EMP.plusDays(5));
        assertTrue(emp.isDevolvido());
        assertEquals(DATA_EMP.plusDays(5), emp.getDataDevolucao());
        assertTrue(livro.isDisponivel()); // livro liberado ao devolver
    }

    // Devolução com data anterior ao empréstimo deve lançar exceção
    @Test
    void deveLancarExcecaoDataDevolucaoAnteriorAoEmprestimo() {
        Emprestimo emp = new Emprestimo("EMP-0005", livro, "Elisa", DATA_EMP);
        assertThrows(IllegalArgumentException.class,
                () -> emp.registrarDevolucao(DATA_EMP.minusDays(1)));
    }

    // Segunda devolução deve lançar exceção
    @Test
    void deveLancarExcecaoSegundaDevolucao() {
        Emprestimo emp = new Emprestimo("EMP-0006", livro, "Fabio", DATA_EMP);
        emp.registrarDevolucao(DATA_EMP.plusDays(3));
        assertThrows(IllegalStateException.class,
                () -> emp.registrarDevolucao(DATA_EMP.plusDays(5)));
    }
}
