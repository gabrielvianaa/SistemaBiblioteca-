# 📚 Sistema de Biblioteca

> Trabalho Final — Disciplina de Teste de Software | Engenharia de Software

Sistema de gerenciamento de acervo de livros e empréstimos com **interface de linha de comando (CLI)**, **API REST** e **banco de dados SQLite** persistente. Cobre os três níveis da pirâmide de testes: unitários (JUnit 5), API (Postman/Newman) e E2E (Playwright).

---

## 📋 Sumário

- [Sobre o Sistema](#sobre-o-sistema)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Banco de Dados](#banco-de-dados)
- [API REST](#api-rest)
- [Interface CLI](#interface-cli)
- [Como Executar no IntelliJ](#como-executar-no-intellij)
- [Modos de Execução](#modos-de-execução)
- [Usuários Padrão](#usuários-padrão)
- [Controle de Acesso por Perfil](#controle-de-acesso-por-perfil)
- [Testes](#testes)
- [Documentação do Trabalho](#documentação-do-trabalho)
- [Integrantes](#integrantes)

---

## Sobre o Sistema

O **Sistema de Biblioteca** permite o gerenciamento completo de um acervo de livros e o controle de empréstimos e devoluções. Possui:

- Autenticação com três perfis de acesso: **ADMIN**, **BIBLIOTECARIO** e **LEITOR**
- CRUD completo de livros e usuários
- Registro de empréstimos com prazo automático de 14 dias e ID sequencial (`EMP-XXXX`)
- Registro de devoluções com data
- Alertas de atraso com cálculo de dias
- Histórico de empréstimos por usuário
- Dashboard com resumo geral
- **API REST** com 13 endpoints (Javalin, porta 8080)
- **CLI interativa** com menus hierárquicos e controle de perfil

Todos os dados são persistidos em um arquivo **SQLite** local (`biblioteca.db`) criado automaticamente na primeira execução.

---

## Tecnologias

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 17 |
| Banco de dados | SQLite (JDBC) | 3.45.3.0 |
| Servidor HTTP (API) | Javalin | 6.1.3 |
| Serialização JSON | Jackson | 2.17.0 |
| Build | Maven | 3.9.6 |
| Testes unitários | JUnit 5 | 5.10.1 |
| Testes de API | Postman / Newman | v11 |
| Testes E2E | Playwright | 1.44.x |
| CI/CD | GitLab CI | — |

---

## Estrutura do Projeto

```
Biblioteca/
├── src/
│   ├── main/java/com/example/biblioteca/
│   │   ├── Main.java                          # Ponto de entrada (CLI + API)
│   │   ├── api/
│   │   │   └── ApiServer.java                 # Servidor REST Javalin (porta 8080)
│   │   ├── cli/
│   │   │   ├── Console.java                   # Utilitários de I/O do terminal
│   │   │   ├── TelaLogin.java                 # Autenticação (até 3 tentativas)
│   │   │   ├── MenuPrincipal.java             # Menu raiz com controle de perfil
│   │   │   ├── MenuLivros.java                # CRUD de livros com controle de perfil
│   │   │   ├── MenuEmprestimos.java           # Empréstimos com controle de perfil
│   │   │   └── MenuUsuarios.java              # CRUD de usuários (apenas ADMIN)
│   │   ├── db/
│   │   │   └── DatabaseConnection.java        # Conexão SQLite + criação do schema
│   │   ├── model/
│   │   │   ├── Livro.java
│   │   │   ├── Emprestimo.java
│   │   │   └── Usuario.java
│   │   ├── repository/
│   │   │   ├── LivroRepository.java           # CRUD em memória (usado pelos testes unitários)
│   │   │   ├── LivroRepositoryDB.java         # CRUD de livros no SQLite
│   │   │   ├── EmprestimoRepository.java      # CRUD de empréstimos no SQLite
│   │   │   └── UsuarioRepository.java         # CRUD de usuários no SQLite
│   │   └── service/
│   │       ├── BibliotecaService.java         # Serviço em memória (usado pelos testes unitários)
│   │       └── BibliotecaServiceDB.java       # Serviço principal com banco SQLite
│   └── test/java/com/example/biblioteca/
│       ├── LivroTest.java                     #  3 testes unitários
│       ├── BibliotecaServiceTest.java         #  4 testes unitários
│       ├── EmprestimoTest.java                # 10 testes unitários
│       ├── BibliotecaServiceExtraTest.java    #  9 testes unitários
│       ├── LivroRepositoryTest.java           #  7 testes unitários
│       ├── LivroRepositoryDBTest.java         # 12 testes de integração (SQLite)
│       ├── EmprestimoRepositoryTest.java      # 10 testes de integração (SQLite)
│       ├── UsuarioRepositoryTest.java         # 11 testes de integração (SQLite)
│       └── BibliotecaServiceDBTest.java       # 15 testes de integração (SQLite)
|   └── testes_api/
│       └── Biblioteca_API_Tests.postman_collection.json
|   └── testes_e2e/
│       ├── biblioteca.e2e.test.js
│       └── playwright.config.js
├── docs/
│   ├── 01_Documento_de_Visao_e_Historias_de_Usuario.docx
│   ├── 02_Plano_de_Testes.docx
│   ├── 03_Casos_de_Teste.docx
│   └── 04_Relatorio_Final.docx
├── pom.xml
├── .gitlab-ci.yml
├── .gitignore
└── README.md
```

---

## Banco de Dados

O arquivo `biblioteca.db` é criado automaticamente na raiz do projeto na primeira execução.

```sql
CREATE TABLE livros (
    isbn        TEXT PRIMARY KEY,
    titulo      TEXT NOT NULL,
    autor       TEXT NOT NULL,
    ano_pub     INTEGER NOT NULL,
    disponivel  INTEGER NOT NULL DEFAULT 1   -- 0 = emprestado, 1 = disponível
);

CREATE TABLE usuarios (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    nome    TEXT NOT NULL,
    login   TEXT NOT NULL UNIQUE,
    senha   TEXT NOT NULL,
    perfil  TEXT NOT NULL DEFAULT 'LEITOR'  -- ADMIN | BIBLIOTECARIO | LEITOR
);

CREATE TABLE emprestimos (
    id               TEXT PRIMARY KEY,        -- formato: EMP-0001
    isbn             TEXT NOT NULL,
    nome_usuario     TEXT NOT NULL,
    data_emprestimo  TEXT NOT NULL,
    data_devolucao   TEXT,                    -- NULL enquanto ativo
    devolvido        INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (isbn) REFERENCES livros(isbn) ON DELETE RESTRICT
);
```

**Configurações aplicadas automaticamente:**
- `autoCommit = true` — evita `SQLITE_BUSY`
- `PRAGMA journal_mode=WAL` — melhor concorrência
- `PRAGMA busy_timeout=3000` — aguarda até 3s antes de falhar
- `PRAGMA foreign_keys=ON` — impede remover livro com empréstimo ativo

---

## API REST

A API sobe automaticamente na porta **8080** ao executar o sistema.

**Base URL:** `http://localhost:8080`

### Autenticação

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `POST` | `/api/auth/login` | Autentica usuário | 200 / 401 |

```json
// Body
{ "login": "admin", "senha": "admin123" }

// Resposta 200
{ "nome": "Administrador", "login": "admin", "perfil": "ADMIN" }
```

### Livros

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `GET` | `/api/livros` | Lista todos os livros | 200 |
| `GET` | `/api/livros?disponivel=true` | Somente disponíveis | 200 |
| `GET` | `/api/livros?autor=Machado` | Busca por autor (parcial) | 200 |
| `GET` | `/api/livros/{isbn}` | Busca por ISBN | 200 / 404 |
| `POST` | `/api/livros` | Cadastra novo livro | 201 / 400 / 409 |
| `PUT` | `/api/livros/{isbn}` | Edita título, autor e ano | 200 / 404 |
| `DELETE` | `/api/livros/{isbn}` | Remove livro | 204 / 404 / 409 |

```json
// POST /api/livros — body
{ "isbn": "978-85-333-0001-1", "titulo": "Dom Casmurro", "autor": "Machado de Assis", "anoPub": 1899 }

// Resposta 201
{ "isbn": "978-85-333-0001-1", "titulo": "Dom Casmurro", "autor": "Machado de Assis", "anoPub": 1899, "disponivel": true }
```

### Empréstimos

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `GET` | `/api/emprestimos` | Lista empréstimos ativos | 200 |
| `GET` | `/api/emprestimos/atraso` | Lista em atraso | 200 |
| `GET` | `/api/emprestimos/usuario/{nome}` | Histórico por usuário | 200 |
| `POST` | `/api/emprestimos` | Realiza empréstimo | 201 / 400 / 409 |
| `PATCH` | `/api/emprestimos/{id}/devolver` | Registra devolução | 200 / 400 |

```json
// POST /api/emprestimos — body
{ "isbn": "978-85-333-0001-1", "usuario": "Ana Silva", "data": "2025-06-01" }

// Resposta 201
{
  "id": "EMP-0001",
  "livro": { "isbn": "...", "titulo": "Dom Casmurro", "disponivel": false },
  "nomeUsuario": "Ana Silva",
  "dataEmprestimo": "2025-06-01",
  "prazo": "2025-06-15",
  "devolvido": false
}

// PATCH /api/emprestimos/EMP-0001/devolver — body
{ "dataDevolucao": "2025-06-10" }
```

---

## Interface CLI

Ao executar o sistema, além da API, a CLI interativa é iniciada no terminal.

```
  ========================================================
  |          SISTEMA DE BIBLIOTECA  v1.0                |
  |         Gerenciamento de Acervo e Emprestimos       |
  ========================================================

  Login: admin
  Senha: ****

  [MENU PRINCIPAL | Administrador [ADMIN]]
  [1] Gerenciar Livros        (Listar / Buscar / Cadastrar / Editar / Remover)
  [2] Gerenciar Emprestimos   (Emprestar / Devolver / Listar / Atrasos / Historico)
  [3] Gerenciar Usuarios      (Cadastrar / Listar / Alterar / Remover)
  [4] Dashboard               (Resumo do sistema)
  [0] Sair do sistema
```

### Controle de acesso nos menus

As opções são **filtradas e bloqueadas** conforme o perfil. Se um LEITOR digitar o número de uma opção restrita, recebe `[ERRO] Acesso negado`.

---

## Como Executar no IntelliJ

> Não é necessário `mvn` no terminal do sistema. Tudo é feito dentro do IntelliJ.

**1. Abrir o projeto**

`File → Open` → selecionar a pasta que contém o `pom.xml` (`Biblioteca/Biblioteca`).
Quando aparecer o popup *"Maven project detected"*, clicar em **Load** ou **Trust Project**.

**2. Sincronizar dependências**

`View → Tool Windows → Maven` → clicar no botão **↻ Reload All Maven Projects**.
O IntelliJ baixará automaticamente: `sqlite-jdbc`, `javalin`, `jackson-databind` e `slf4j`.

**3. Verificar o SDK**

`File → Project Structure → Project` → confirmar **Java 17** ou superior.
Se não tiver: `Add SDK → Download JDK → Eclipse Temurin 17`.

**4. Executar o sistema**

Abrir `Main.java` e clicar no **triângulo verde** na margem esquerda, ou `Shift + F10`.

**5. Executar os testes**

Painel Maven → `Lifecycle` → duplo clique em **test**.
Ou clicar com botão direito em `src/test/java` → **Run 'All Tests'**.

---

## Modos de Execução

O `Main.java` aceita argumentos que controlam o que é iniciado:

| Comando | Comportamento |
|---------|--------------|
| `java -jar biblioteca-app.jar` | Sobe a **API REST** (porta 8080) **e** a **CLI** |
| `java -jar biblioteca-app.jar --api` | Sobe **apenas a API** — ideal para testes com Postman |
| `java -jar biblioteca-app.jar --cli` | Inicia **apenas a CLI** — sem servidor HTTP |

**No IntelliJ**, para usar argumentos:
`Run → Edit Configurations → Program Arguments → --api`

---

## Usuários Padrão

Criados automaticamente na **primeira execução** (banco vazio):

| Login | Senha | Perfil |
|-------|-------|--------|
| `admin` | `admin123` | ADMIN |
| `maria` | `maria123` | BIBLIOTECARIO |
| `joao` | `joao123` | LEITOR |

---

## Controle de Acesso por Perfil

| Funcionalidade | ADMIN | BIBLIOTECARIO | LEITOR |
|---------------|:-----:|:-------------:|:------:|
| Consultar e buscar livros | ✅ | ✅ | ✅ |
| Cadastrar, editar, remover livros | ✅ | ✅ | ❌ |
| Realizar empréstimos e devoluções | ✅ | ✅ | ❌ |
| Ver empréstimos ativos e atrasos | ✅ | ✅ | ✅ |
| Ver histórico de empréstimos | ✅ (qualquer) | ✅ (qualquer) | ✅ (próprio) |
| Gerenciar usuários (CRUD) | ✅ | ❌ | ❌ |
| Dashboard | ✅ | ✅ | ✅ |
| Todos os endpoints da API | ✅ | ✅ | ✅ |

---

## Testes

### Visão geral

| Arquivo | Tipo | Testes | Resultado esperado |
|---------|------|:------:|-------------------|
| `LivroTest` | Unitário | 3 | ✅ PASS |
| `BibliotecaServiceTest` | Unitário | 4 | ✅ PASS |
| `EmprestimoTest` | Unitário | 10 | ✅ PASS |
| `BibliotecaServiceExtraTest` | Unitário | 9 | ✅ PASS |
| `LivroRepositoryTest` | Unitário | 7 | ✅ PASS |
| `BibliotecaFalhaTest` | **Proposital** ⚠️ | 3 | ❌ FAIL (esperado) |
| `LivroRepositoryDBTest` | Integração | 12 | ✅ PASS |
| `EmprestimoRepositoryTest` | Integração | 10 | ✅ PASS |
| `UsuarioRepositoryTest` | Integração | 11 | ✅ PASS |
| `BibliotecaServiceDBTest` | Integração | 15 | ✅ PASS |
| **Total** | | **84** | **81 PASS / 3 FAIL proposital** |

> ⚠️ `BibliotecaFalhaTest` contém **3 testes propositalmente incorretos** para demonstrar como o JUnit reporta falhas. Eles sempre falham — isso é esperado e documentado.

### Testes de integração com banco em memória

Os testes de integração usam `jdbc:sqlite::memory:` — banco criado e destruído dentro da JVM para cada classe de teste. Garante isolamento total sem criar arquivos em disco.

### Executar somente os testes corretos

No painel Maven → duplo clique em `test`. Ou via terminal:

```bash
mvn test -Dtest="LivroTest,BibliotecaServiceTest,EmprestimoTest,BibliotecaServiceExtraTest,LivroRepositoryTest,LivroRepositoryDBTest,EmprestimoRepositoryTest,UsuarioRepositoryTest,BibliotecaServiceDBTest"
```

Saída esperada:

```
Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Testes de API (Postman / Newman)

**Pré-requisito:** sistema rodando com `--api` ou sem argumento (porta 8080 ativa).

```bash
# Instalar Newman (apenas uma vez)
npm install -g newman

# Executar a collection
newman run testes_api/Biblioteca_API_Tests.postman_collection.json
```

Ou importar no Postman e clicar em **Run Collection**.

Resultado esperado: todos os requests com status verde.

### Testes E2E (Playwright)

```bash
cd testes_e2e
npm install @playwright/test
npx playwright install chromium
npx playwright test --config=playwright.config.js
```

Resultado esperado: `5 passed`

---

## CI/CD

O arquivo `.gitlab-ci.yml` define três estágios automáticos:

```
build  →  test  →  deploy (apenas branch master)
```

Os relatórios XML do Surefire (`target/surefire-reports/TEST-*.xml`) são publicados automaticamente como artefatos de teste no GitLab.

---

## Documentação do Trabalho

Todos os documentos estão na pasta `docs/`:

| Arquivo | Conteúdo |
|---------|----------|
| `01_Documento_de_Visao_e_Historias_de_Usuario.docx` | Visão do sistema, requisitos funcionais/não-funcionais e 5 histórias de usuário com critérios de aceitação e cenários BDD (Gherkin) |
| `02_Plano_de_Testes.docx` | Objetivo, escopo, ferramentas, pirâmide de testes, estratégia, papéis e cronograma |
| `03_Casos_de_Teste.docx` | Todos os casos de teste com ID, descrição, passos, entradas e saídas esperadas |
| `04_Relatorio_Final.docx` | Relatório completo com introdução, planejamento, resultados, 16 caixas de evidência para prints e roteiro passo a passo de como executar cada tipo de teste |

---

## Integrantes

| Nome | Responsabilidade |
|------|-----------------|
| Aluno 1 | Testes unitários — `Livro`, `Emprestimo` e `LivroRepository` |
| Aluno 2 | Testes de integração — `LivroRepositoryDB`, `EmprestimoRepository`, `UsuarioRepository`, `BibliotecaServiceDB` |
| Aluno 3 | Testes de API — Postman collection + Newman |
| Aluno 4 | Testes E2E — Playwright + Relatório Final |
