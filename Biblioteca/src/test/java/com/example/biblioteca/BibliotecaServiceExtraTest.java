package com.example.biblioteca;

import com.example.biblioteca.model.Emprestimo;
import com.example.biblioteca.model.Livro;
import com.example.biblioteca.repository.LivroRepository;
import com.example.biblioteca.service.BibliotecaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BibliotecaServiceExtraTest {

    private BibliotecaService service;
    private static final LocalDate BASE = LocalDate.of(2025, 6, 1);

    @BeforeEach
    void setUp() {
        service = new BibliotecaService(new LivroRepository());
        service.cadastrarLivro(new Livro("978-85-333-0001-1", "Dom Casmurro",   "Machado de Assis", 1899));
        service.cadastrarLivro(new Livro("978-85-333-0002-2", "Quincas Borba",  "Machado de Assis", 1891));
        service.cadastrarLivro(new Livro("978-85-333-0003-3", "O Cortico",      "Aluisio Azevedo",  1890));
    }

    // CT-U10 — Busca por autor (case-insensitive)
    @Test
    void deveBuscarPorAutorCaseInsensitive() {
        List<Livro> resultado = service.buscarPorAutor("machado");
        assertEquals(2, resultado.size());
    }

    // Busca por autor sem resultado retorna lista vazia
    @Test
    void deveBuscarPorAutorSemResultado() {
        List<Livro> resultado = service.buscarPorAutor("Autor Inexistente");
        assertTrue(resultado.isEmpty());
    }

    // CT-U17 — Listar disponíveis após empréstimo
    @Test
    void deveListarDisponiveisAposEmprestimo() {
        service.realizarEmprestimo("978-85-333-0001-1", "Ana", BASE);
        List<Livro> disponiveis = service.listarDisponiveis();
        assertEquals(2, disponiveis.size());
        assertTrue(disponiveis.stream()
                .noneMatch(l -> l.getIsbn().equals("978-85-333-0001-1")));
    }

    // CT-U18 — Listar empréstimos em atraso
    @Test
    void deveListarEmprestimosEmAtraso() {
        // Empréstimo de 30 dias atrás → mais de 14 dias → em atraso
        service.realizarEmprestimo("978-85-333-0001-1", "Bruno", BASE.minusDays(30));
        List<Emprestimo> atrasados = service.listarEmAtraso(BASE);
        assertEquals(1, atrasados.size());
        assertTrue(atrasados.get(0).calcularDiasAtraso(BASE) > 0);
    }

    // CT-U19 — Empréstimo dentro do prazo não aparece na lista de atraso
    @Test
    void naoDeveListarEmprestimoNoPrazo() {
        service.realizarEmprestimo("978-85-333-0001-1", "Carla", BASE.minusDays(5));
        List<Emprestimo> atrasados = service.listarEmAtraso(BASE);
        assertTrue(atrasados.isEmpty());
    }

    // CT-U20 — Empréstimo devolvido não aparece na lista de atraso
    @Test
    void emprestimoDevolvido_naoApareceNoAtraso() {
        Emprestimo emp = service.realizarEmprestimo(
                "978-85-333-0001-1", "Diego", BASE.minusDays(30));
        service.registrarDevolucao(emp.getId(), BASE);
        List<Emprestimo> atrasados = service.listarEmAtraso(BASE);
        assertTrue(atrasados.isEmpty());
    }

    // CT-U21 — Histórico de empréstimos por usuário
    @Test
    void deveRetornarHistoricoDoUsuario() {
        service.realizarEmprestimo("978-85-333-0001-1", "Elisa", BASE);
        service.realizarEmprestimo("978-85-333-0002-2", "Elisa", BASE);
        service.realizarEmprestimo("978-85-333-0003-3", "Fabio", BASE);

        List<Emprestimo> historico = service.historicoUsuario("Elisa");
        assertEquals(2, historico.size());
    }

    // CT-U22 — ISBN não encontrado lança exceção
    @Test
    void deveLancarExcecaoIsbnNaoEncontrado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.realizarEmprestimo("000-inexistente", "Ana", BASE));
    }

    // CT-U23 — Contador de empréstimos ativos
    @Test
    void deveContarEmprestimosAtivos() {
        service.realizarEmprestimo("978-85-333-0001-1", "Ana",   BASE);
        service.realizarEmprestimo("978-85-333-0002-2", "Bruno", BASE);
        assertEquals(2, service.totalEmprestimosAtivos());
    }
}
