/**
 * Testes E2E — Sistema de Biblioteca
 * Framework: Playwright (usando APIRequestContext para testar a API REST)
 *
 * Estes testes simulam fluxos completos de um usuário real,
 * cobrindo múltiplos endpoints em sequência — da autenticação
 * até a devolução de livros.
 *
 * PRÉ-REQUISITO: API rodando em http://localhost:8080
 *   No IntelliJ: Main.java com argumento --api (ou sem argumento)
 *
 * EXECUTAR:
 *   cd testes_e2e
 *   npx playwright test
 *   npx playwright test --reporter=html  (gera relatório visual)
 */

const { test, expect } = require('@playwright/test');

const BASE = 'http://localhost:8080';

// ── ISBN exclusivo para os testes E2E (evita conflito com Postman) ────────────
const ISBN_TESTE = '978-00-E2E-0001-1';

// ── Helper: limpa estado antes dos testes ────────────────────────────────────
async function limparEstado(request) {
  // Devolve todos os empréstimos ativos
  const ativos = await request.get(`${BASE}/api/emprestimos`);
  if (ativos.ok()) {
    const lista = await ativos.json();
    for (const emp of lista) {
      await request.patch(`${BASE}/api/emprestimos/${emp.id}/devolver`, {
        data: { dataDevolucao: emp.dataEmprestimo }
      });
    }
  }
  // Remove livro de teste se existir
  await request.delete(`${BASE}/api/livros/${ISBN_TESTE}`);
}

// ════════════════════════════════════════════════════════════════════════════
// CT-E01 — Fluxo completo: Login → Cadastro → Consulta
// ════════════════════════════════════════════════════════════════════════════
test('CT-E01 — Login valido e cadastro de livro', async ({ request }) => {

  // ── Passo 1: Autenticar como admin ───────────────────────────────────────
  const resLogin = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'admin', senha: 'admin123' }
  });

  expect(resLogin.status()).toBe(200);
  const usuario = await resLogin.json();
  expect(usuario.perfil).toBe('ADMIN');
  expect(usuario.nome).toBeTruthy();
  console.log(`  Login OK: ${usuario.nome} [${usuario.perfil}]`);

  // ── Passo 2: Remover livro de teste se já existir ────────────────────────
  await request.delete(`${BASE}/api/livros/${ISBN_TESTE}`);

  // ── Passo 3: Cadastrar novo livro ────────────────────────────────────────
  const resCadastro = await request.post(`${BASE}/api/livros`, {
    data: {
      isbn:   ISBN_TESTE,
      titulo: 'Livro E2E Playwright',
      autor:  'Autor E2E',
      anoPub: 2025
    }
  });

  expect(resCadastro.status()).toBe(201);
  const livro = await resCadastro.json();
  expect(livro.isbn).toBe(ISBN_TESTE);
  expect(livro.disponivel).toBe(true);
  console.log(`  Livro cadastrado: ${livro.titulo} | disponivel: ${livro.disponivel}`);

  // ── Passo 4: Confirmar que o livro aparece na listagem ───────────────────
  const resListagem = await request.get(`${BASE}/api/livros`);
  expect(resListagem.status()).toBe(200);
  const todos = await resListagem.json();
  const encontrado = todos.find(l => l.isbn === ISBN_TESTE);
  expect(encontrado).toBeTruthy();
  expect(encontrado.titulo).toBe('Livro E2E Playwright');
  console.log(`  Livro aparece na listagem: OK (${todos.length} livros no total)`);

  // ── Passo 5: Confirmar livro disponível no filtro ────────────────────────
  const resDisp = await request.get(`${BASE}/api/livros?disponivel=true`);
  expect(resDisp.status()).toBe(200);
  const disponiveis = await resDisp.json();
  expect(disponiveis.some(l => l.isbn === ISBN_TESTE)).toBe(true);
  console.log(`  Livro aparece em disponiveis: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E02 — Fluxo completo: Empréstimo → Verificar indisponibilidade
// ════════════════════════════════════════════════════════════════════════════
test('CT-E02 — Realizar emprestimo e verificar indisponibilidade', async ({ request }) => {
  await limparEstado(request);

  // ── Passo 1: Garantir livro de teste existe e está disponível ────────────
  await request.delete(`${BASE}/api/livros/${ISBN_TESTE}`);
  const resCad = await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E', autor: 'Autor E2E', anoPub: 2025 }
  });
  expect(resCad.status()).toBe(201);

  // ── Passo 2: Realizar empréstimo ─────────────────────────────────────────
  const resEmp = await request.post(`${BASE}/api/emprestimos`, {
    data: {
      isbn:    ISBN_TESTE,
      usuario: 'Maria Leitora',
      data:    '2025-06-01'
    }
  });

  expect(resEmp.status()).toBe(201);
  const emp = await resEmp.json();
  expect(emp.id).toMatch(/^EMP-\d{4}$/);
  expect(emp.livro.disponivel).toBe(false);
  expect(emp.prazo).toBeTruthy();
  console.log(`  Emprestimo criado: ${emp.id} | prazo: ${emp.prazo}`);

  // ── Passo 3: Confirmar livro indisponível ─────────────────────────────────
  const resLivro = await request.get(`${BASE}/api/livros/${ISBN_TESTE}`);
  expect(resLivro.status()).toBe(200);
  const livroAtual = await resLivro.json();
  expect(livroAtual.disponivel).toBe(false);
  console.log(`  Livro indisponivel apos emprestimo: OK`);

  // ── Passo 4: Confirmar que NÃO aparece no filtro de disponíveis ───────────
  const resDisp = await request.get(`${BASE}/api/livros?disponivel=true`);
  const disponiveis = await resDisp.json();
  expect(disponiveis.some(l => l.isbn === ISBN_TESTE)).toBe(false);
  console.log(`  Livro ausente na lista de disponiveis: OK`);

  // ── Passo 5: Confirmar que aparece na lista de ativos ─────────────────────
  const resAtivos = await request.get(`${BASE}/api/emprestimos`);
  const ativos = await resAtivos.json();
  expect(ativos.some(e => e.id === emp.id)).toBe(true);
  console.log(`  Emprestimo aparece em ativos: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E03 — Fluxo completo: Devolução → Livro volta a disponível
// ════════════════════════════════════════════════════════════════════════════
test('CT-E03 — Registrar devolucao e liberar livro', async ({ request }) => {
  await limparEstado(request);

  // ── Passo 1: Preparar livro e fazer empréstimo ───────────────────────────
  await request.delete(`${BASE}/api/livros/${ISBN_TESTE}`);
  await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E', autor: 'Autor E2E', anoPub: 2025 }
  });

  const resEmp = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Carlos Leitor', data: '2025-06-01' }
  });
  expect(resEmp.status()).toBe(201);
  const emp = await resEmp.json();
  console.log(`  Emprestimo criado: ${emp.id}`);

  // ── Passo 2: Registrar devolução ─────────────────────────────────────────
  const resDev = await request.patch(
    `${BASE}/api/emprestimos/${emp.id}/devolver`, {
    data: { dataDevolucao: '2025-06-10' }
  });

  expect(resDev.status()).toBe(200);
  const devolvido = await resDev.json();
  expect(devolvido.devolvido).toBe(true);
  expect(devolvido.dataDevolucao).toBe('2025-06-10');
  console.log(`  Devolucao registrada: devolvido=${devolvido.devolvido}`);

  // ── Passo 3: Confirmar livro disponível novamente ─────────────────────────
  const resLivro = await request.get(`${BASE}/api/livros/${ISBN_TESTE}`);
  const livro = await resLivro.json();
  expect(livro.disponivel).toBe(true);
  console.log(`  Livro disponivel apos devolucao: OK`);

  // ── Passo 4: Confirmar que NÃO aparece mais em ativos ─────────────────────
  const resAtivos = await request.get(`${BASE}/api/emprestimos`);
  const ativos = await resAtivos.json();
  expect(ativos.some(e => e.id === emp.id)).toBe(false);
  console.log(`  Emprestimo removido da lista de ativos: OK`);

  // ── Passo 5: Confirmar aparece no histórico do usuário ────────────────────
  const resHist = await request.get(
    `${BASE}/api/emprestimos/usuario/Carlos Leitor`
  );
  const historico = await resHist.json();
  expect(historico.some(e => e.id === emp.id)).toBe(true);
  console.log(`  Emprestimo aparece no historico: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E04 — Login com credenciais inválidas → bloqueio
// ════════════════════════════════════════════════════════════════════════════
test('CT-E04 — Login com credenciais invalidas retorna 401', async ({ request }) => {

  // ── Passo 1: Tentar login com senha errada ───────────────────────────────
  const res1 = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'admin', senha: 'senhaErrada' }
  });
  expect(res1.status()).toBe(401);
  const corpo1 = await res1.json();
  expect(corpo1).toHaveProperty('erro');
  console.log(`  Login com senha errada bloqueado: ${corpo1.erro}`);

  // ── Passo 2: Tentar login com usuário inexistente ────────────────────────
  const res2 = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'naoexiste', senha: 'qualquer' }
  });
  expect(res2.status()).toBe(401);
  console.log(`  Login com usuario inexistente bloqueado: OK`);

  // ── Passo 3: Confirmar que login válido ainda funciona ───────────────────
  const res3 = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'admin', senha: 'admin123' }
  });
  expect(res3.status()).toBe(200);
  console.log(`  Login valido continua funcionando: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E05 — Regras de negócio: bloquear empréstimo de livro indisponível
// ════════════════════════════════════════════════════════════════════════════
test('CT-E05 — Bloquear emprestimo de livro indisponivel', async ({ request }) => {
  await limparEstado(request);

  // ── Passo 1: Criar livro e emprestar ─────────────────────────────────────
  await request.delete(`${BASE}/api/livros/${ISBN_TESTE}`);
  await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E', autor: 'Autor E2E', anoPub: 2025 }
  });

  const res1 = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Ana Silva', data: '2025-06-01' }
  });
  expect(res1.status()).toBe(201);
  console.log(`  Primeiro emprestimo: OK`);

  // ── Passo 2: Tentar emprestar o mesmo livro novamente → deve falhar ───────
  const res2 = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Bruno Costa', data: '2025-06-01' }
  });
  expect(res2.status()).toBe(409);
  const erro = await res2.json();
  expect(erro).toHaveProperty('erro');
  expect(erro.erro.toLowerCase()).toContain('indisponivel');
  console.log(`  Segundo emprestimo bloqueado (409): ${erro.erro}`);

  // ── Passo 3: Verificar que o livro permanece com o primeiro usuário ───────
  const resAtivos = await request.get(`${BASE}/api/emprestimos`);
  const ativos = await resAtivos.json();
  const empAtivo = ativos.find(e => e.livro.isbn === ISBN_TESTE);
  expect(empAtivo).toBeTruthy();
  expect(empAtivo.nomeUsuario).toBe('Ana Silva');
  console.log(`  Livro permanece com Ana Silva: OK`);
});