// =============================================================
// Testes E2E – Sistema de Biblioteca (Playwright)
// Framework: Playwright v1.44+  |  Node.js 20+
//
// SETUP:
//   npm init playwright@latest
//   npm install @playwright/test
//   npx playwright install
//
// EXECUTAR:
//   npx playwright test
//   npx playwright test --headed        (com interface visual)
//   npx playwright test --reporter=html (gera relatório HTML)
// =============================================================

const { test, expect } = require('@playwright/test');

// URL base – ajuste conforme ambiente
const BASE_URL = 'http://localhost:3000';

// ─────────────────────────────────────────────────────────────
// HELPER: realiza login antes de cada teste
// ─────────────────────────────────────────────────────────────
async function login(page) {
  await page.goto(`${BASE_URL}/login`);
  await page.fill('[data-testid="input-usuario"]', 'admin');
  await page.fill('[data-testid="input-senha"]', 'senha123');
  await page.click('[data-testid="btn-login"]');
  await expect(page).toHaveURL(`${BASE_URL}/dashboard`);
}

// =============================================================
// CT-E01 – Fluxo: Login e Cadastro de Livro
// =============================================================
test('CT-E01 – Deve fazer login e cadastrar um novo livro', async ({ page }) => {
  // ── Passo 1: Login ──────────────────────────────────────────
  await page.goto(`${BASE_URL}/login`);
  await expect(page).toHaveTitle(/Biblioteca/);

  await page.fill('[data-testid="input-usuario"]', 'admin');
  await page.fill('[data-testid="input-senha"]', 'senha123');
  await page.click('[data-testid="btn-login"]');

  // ── Passo 2: Verificar redirecionamento ────────────────────
  await expect(page).toHaveURL(`${BASE_URL}/dashboard`);
  await expect(page.locator('[data-testid="nav-livros"]')).toBeVisible();

  // ── Passo 3: Navegar para cadastro ────────────────────────
  await page.click('[data-testid="nav-livros"]');
  await page.click('[data-testid="btn-cadastrar-livro"]');
  await expect(page).toHaveURL(`${BASE_URL}/livros/novo`);

  // ── Passo 4: Preencher formulário ─────────────────────────
  await page.fill('[data-testid="input-isbn"]', '978-85-333-0099-9');
  await page.fill('[data-testid="input-titulo"]', 'Livro E2E Teste');
  await page.fill('[data-testid="input-autor"]', 'Autor Playwright');
  await page.fill('[data-testid="input-ano"]', '2025');

  // ── Passo 5: Salvar ────────────────────────────────────────
  await page.click('[data-testid="btn-salvar-livro"]');

  // ── Verificações ───────────────────────────────────────────
  await expect(page.locator('[data-testid="alerta-sucesso"]'))
    .toContainText('Livro cadastrado com sucesso');

  await page.goto(`${BASE_URL}/livros`);
  await expect(page.locator('text=Livro E2E Teste')).toBeVisible();
  await expect(page.locator('text=978-85-333-0099-9')).toBeVisible();
});

// =============================================================
// CT-E02 – Fluxo: Realizar Empréstimo
// =============================================================
test('CT-E02 – Deve realizar empréstimo e atualizar status do livro', async ({ page }) => {
  await login(page);

  // ── Navegar para livros ────────────────────────────────────
  await page.click('[data-testid="nav-livros"]');

  // ── Localizar livro disponível e clicar em "Emprestar" ────
  const linhaLivro = page.locator('[data-testid="linha-livro"]').filter({ hasText: 'Dom Casmurro' });
  await expect(linhaLivro).toBeVisible();
  await expect(linhaLivro.locator('[data-testid="status-livro"]')).toHaveText('Disponível');

  await linhaLivro.locator('[data-testid="btn-emprestar"]').click();

  // ── Preencher dados do empréstimo ─────────────────────────
  await expect(page.locator('[data-testid="modal-emprestimo"]')).toBeVisible();
  await page.fill('[data-testid="input-nome-usuario"]', 'Ana Silva');
  await page.fill('[data-testid="input-data-emprestimo"]', '2025-06-01');
  await page.click('[data-testid="btn-confirmar-emprestimo"]');

  // ── Verificações ───────────────────────────────────────────
  await expect(page.locator('[data-testid="alerta-sucesso"]'))
    .toContainText('Empréstimo realizado');

  // Status deve mudar para "Indisponível"
  await expect(linhaLivro.locator('[data-testid="status-livro"]')).toHaveText('Indisponível');

  // Botão "Emprestar" deve estar desabilitado
  await expect(linhaLivro.locator('[data-testid="btn-emprestar"]')).toBeDisabled();
});

// =============================================================
// CT-E03 – Fluxo: Registrar Devolução
// =============================================================
test('CT-E03 – Deve registrar devolução e liberar o livro', async ({ page }) => {
  await login(page);

  // ── Navegar para empréstimos ativos ───────────────────────
  await page.click('[data-testid="nav-emprestimos"]');
  await expect(page).toHaveURL(`${BASE_URL}/emprestimos`);

  // ── Localizar empréstimo de Ana Silva ─────────────────────
  const linhaEmp = page.locator('[data-testid="linha-emprestimo"]').filter({ hasText: 'Ana Silva' });
  await expect(linhaEmp).toBeVisible();
  await expect(linhaEmp.locator('[data-testid="status-emprestimo"]')).toHaveText('Ativo');

  // ── Clicar em Devolver ────────────────────────────────────
  await linhaEmp.locator('[data-testid="btn-devolver"]').click();

  // ── Preencher data de devolução ───────────────────────────
  await expect(page.locator('[data-testid="modal-devolucao"]')).toBeVisible();
  await page.fill('[data-testid="input-data-devolucao"]', '2025-06-10');
  await page.click('[data-testid="btn-confirmar-devolucao"]');

  // ── Verificações ───────────────────────────────────────────
  await expect(page.locator('[data-testid="alerta-sucesso"]'))
    .toContainText('Devolução registrada');

  // Empréstimo some da lista de ativos
  await expect(linhaEmp).not.toBeVisible();

  // Confirmar que livro voltou a disponível
  await page.click('[data-testid="nav-livros"]');
  const linhaLivro = page.locator('[data-testid="linha-livro"]').filter({ hasText: 'Dom Casmurro' });
  await expect(linhaLivro.locator('[data-testid="status-livro"]')).toHaveText('Disponível');
});

// =============================================================
// CT-E04 – Login com credenciais inválidas
// =============================================================
test('CT-E04 – Deve exibir erro com credenciais inválidas', async ({ page }) => {
  await page.goto(`${BASE_URL}/login`);
  await page.fill('[data-testid="input-usuario"]', 'admin');
  await page.fill('[data-testid="input-senha"]', 'senhaErrada');
  await page.click('[data-testid="btn-login"]');

  // Deve permanecer na tela de login
  await expect(page).toHaveURL(`${BASE_URL}/login`);
  await expect(page.locator('[data-testid="erro-login"]')).toContainText('Credenciais inválidas');
});

// =============================================================
// CT-E05 – Tentativa de empréstimo de livro indisponível
// =============================================================
test('CT-E05 – Deve bloquear empréstimo de livro indisponível', async ({ page }) => {
  await login(page);
  await page.click('[data-testid="nav-livros"]');

  const linhaLivro = page.locator('[data-testid="linha-livro"]').filter({ hasText: 'Dom Casmurro' });

  // Botão deve estar desabilitado se livro está indisponível
  await expect(linhaLivro.locator('[data-testid="status-livro"]')).toHaveText('Indisponível');
  await expect(linhaLivro.locator('[data-testid="btn-emprestar"]')).toBeDisabled();
});
