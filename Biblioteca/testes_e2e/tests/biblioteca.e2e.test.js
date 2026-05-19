const { test, expect } = require('@playwright/test');

const BASE       = 'http://localhost:8080';
const ISBN_TESTE = '978-00-E2E-0001-1';

// Data de hoje no formato YYYY-MM-DD (usada para devoluções)
const HOJE = new Date().toISOString().split('T')[0];

// ── Limpa estado de forma robusta ─────────────────────────────────────────────
async function limparEstado(request) {
  // 1. Busca empréstimos ativos
  const resAtivos = await request.get(`${BASE}/api/emprestimos`);
  if (resAtivos.ok()) {
    const lista = await resAtivos.json();
    for (const emp of lista) {
      // Usa a data de hoje como devolução (nunca anterior ao empréstimo)
      await request.patch(`${BASE}/api/emprestimos/${emp.id}/devolver`, {
        data: { dataDevolucao: HOJE }
      });
    }
  }
  // 2. Remove o livro de teste (FK liberada após devolver)
  await request.delete(`${BASE}/api/livros/${ISBN_TESTE}`);
}

// ════════════════════════════════════════════════════════════════════════════
// CT-E01 — Fluxo completo: Login → Cadastro → Consulta
// ════════════════════════════════════════════════════════════════════════════
test('CT-E01 — Login valido e cadastro de livro', async ({ request }) => {
  await limparEstado(request);

  // Passo 1: autenticar
  const resLogin = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'admin', senha: 'admin123' }
  });
  expect(resLogin.status()).toBe(200);
  const usuario = await resLogin.json();
  expect(usuario.perfil).toBe('ADMIN');
  console.log(`  Login OK: ${usuario.nome} [${usuario.perfil}]`);

  // Passo 2: cadastrar livro
  const resCadastro = await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E Playwright', autor: 'Autor E2E', anoPub: 2025 }
  });
  expect(resCadastro.status()).toBe(201);
  const livro = await resCadastro.json();
  expect(livro.isbn).toBe(ISBN_TESTE);
  expect(livro.disponivel).toBe(true);
  console.log(`  Livro cadastrado: ${livro.titulo}`);

  // Passo 3: confirmar na listagem
  const todos = await (await request.get(`${BASE}/api/livros`)).json();
  const encontrado = todos.find(l => l.isbn === ISBN_TESTE);
  expect(encontrado).toBeTruthy();
  console.log(`  Livro aparece na listagem: OK (${todos.length} livros)`);

  // Passo 4: confirmar nos disponíveis
  const disponiveis = await (await request.get(`${BASE}/api/livros?disponivel=true`)).json();
  expect(disponiveis.some(l => l.isbn === ISBN_TESTE)).toBe(true);
  console.log(`  Livro aparece em disponiveis: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E02 — Fluxo completo: Empréstimo → Verificar indisponibilidade
// ════════════════════════════════════════════════════════════════════════════
test('CT-E02 — Realizar emprestimo e verificar indisponibilidade', async ({ request }) => {
  await limparEstado(request);

  // Criar livro limpo
  const resCad = await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E', autor: 'Autor E2E', anoPub: 2025 }
  });
  expect(resCad.status()).toBe(201);

  // Realizar empréstimo com data de hoje
  const resEmp = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Maria Leitora', data: HOJE }
  });
  expect(resEmp.status()).toBe(201);
  const emp = await resEmp.json();
  expect(emp.id).toMatch(/^EMP-\d{4}$/);
  expect(emp.livro.disponivel).toBe(false);
  console.log(`  Emprestimo criado: ${emp.id} | prazo: ${emp.prazo}`);

  // Confirmar livro indisponível
  const livroAtual = await (await request.get(`${BASE}/api/livros/${ISBN_TESTE}`)).json();
  expect(livroAtual.disponivel).toBe(false);
  console.log(`  Livro indisponivel apos emprestimo: OK`);

  // Não aparece nos disponíveis
  const disponiveis = await (await request.get(`${BASE}/api/livros?disponivel=true`)).json();
  expect(disponiveis.some(l => l.isbn === ISBN_TESTE)).toBe(false);
  console.log(`  Livro ausente na lista de disponiveis: OK`);

  // Aparece nos ativos
  const ativos = await (await request.get(`${BASE}/api/emprestimos`)).json();
  expect(ativos.some(e => e.id === emp.id)).toBe(true);
  console.log(`  Emprestimo aparece em ativos: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E03 — Fluxo completo: Devolução → Livro volta a disponível
// ════════════════════════════════════════════════════════════════════════════
test('CT-E03 — Registrar devolucao e liberar livro', async ({ request }) => {
  await limparEstado(request);

  // Preparar
  await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E', autor: 'Autor E2E', anoPub: 2025 }
  });
  const resEmp = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Carlos Leitor', data: HOJE }
  });
  expect(resEmp.status()).toBe(201);
  const emp = await resEmp.json();
  console.log(`  Emprestimo criado: ${emp.id}`);

  // Devolver com data de hoje
  const resDev = await request.patch(`${BASE}/api/emprestimos/${emp.id}/devolver`, {
    data: { dataDevolucao: HOJE }
  });
  expect(resDev.status()).toBe(200);
  const devolvido = await resDev.json();
  expect(devolvido.devolvido).toBe(true);
  console.log(`  Devolucao registrada: OK`);

  // Livro disponível novamente
  const livro = await (await request.get(`${BASE}/api/livros/${ISBN_TESTE}`)).json();
  expect(livro.disponivel).toBe(true);
  console.log(`  Livro disponivel apos devolucao: OK`);

  // Não aparece mais em ativos
  const ativos = await (await request.get(`${BASE}/api/emprestimos`)).json();
  expect(ativos.some(e => e.id === emp.id)).toBe(false);
  console.log(`  Emprestimo removido dos ativos: OK`);

  // Aparece no histórico
  const historico = await (await request.get(
    `${BASE}/api/emprestimos/usuario/Carlos Leitor`
  )).json();
  expect(historico.some(e => e.id === emp.id)).toBe(true);
  console.log(`  Emprestimo aparece no historico: OK`);
});

// ════════════════════════════════════════════════════════════════════════════
// CT-E04 — Login com credenciais inválidas → bloqueio
// ════════════════════════════════════════════════════════════════════════════
test('CT-E04 — Login com credenciais invalidas retorna 401', async ({ request }) => {
  const res1 = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'admin', senha: 'senhaErrada' }
  });
  expect(res1.status()).toBe(401);
  expect(await res1.json()).toHaveProperty('erro');
  console.log(`  Senha errada bloqueada: OK`);

  const res2 = await request.post(`${BASE}/api/auth/login`, {
    data: { login: 'naoexiste', senha: 'qualquer' }
  });
  expect(res2.status()).toBe(401);
  console.log(`  Usuario inexistente bloqueado: OK`);

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

  // Criar livro e emprestar
  await request.post(`${BASE}/api/livros`, {
    data: { isbn: ISBN_TESTE, titulo: 'Livro E2E', autor: 'Autor E2E', anoPub: 2025 }
  });
  const res1 = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Ana Silva', data: HOJE }
  });
  expect(res1.status()).toBe(201);
  console.log(`  Primeiro emprestimo: OK`);

  // Tentar emprestar de novo → 409
  const res2 = await request.post(`${BASE}/api/emprestimos`, {
    data: { isbn: ISBN_TESTE, usuario: 'Bruno Costa', data: HOJE }
  });
  expect(res2.status()).toBe(409);
  const erro = await res2.json();
  expect(erro.erro.toLowerCase()).toContain('indisponivel');
  console.log(`  Segundo emprestimo bloqueado (409): OK`);

  // Livro ainda com Ana Silva
  const ativos = await (await request.get(`${BASE}/api/emprestimos`)).json();
  const empAtivo = ativos.find(e => e.livro && e.livro.isbn === ISBN_TESTE);
  expect(empAtivo).toBeTruthy();
  expect(empAtivo.nomeUsuario).toBe('Ana Silva');
  console.log(`  Livro permanece com Ana Silva: OK`);
});