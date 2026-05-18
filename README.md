# 📚 Sistema de Biblioteca

> Trabalho Final — Disciplina de Teste de Software | Engenharia de Software

Sistema de gerenciamento de acervo de livros e empréstimos com **interface de linha de comando (CLI)**, **API REST** e **banco de dados SQLite** persistente. Cobre os três níveis da pirâmide de testes: unitários e de integração (JUnit 5), API (Postman/Newman) e E2E (Playwright).

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
- CRUD completo de livros e usuários com controle de acesso por perfil
- Registro de empréstimos com prazo automático de 14 dias e ID sequencial (`EMP-XXXX`)
- Registro de devoluções com data e liberação automática do livro
- Alertas de atraso com cálculo de dias
- Histórico de empréstimos por usuário (LEITOR vê apenas o próprio)
- Dashboard com resumo geral e alertas
- **API REST** com 13 endpoints (Javalin, porta 8080)
- **CLI interativa** com menus hierárquicos e controle de perfil em todos os submenus

Todos os dados são persistidos em um arquivo **SQLite** local (`biblioteca.db`) criado automaticamente na primeira execução.

---

## Tecnologias

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 17 |
| Banco de dados | SQLite (JDBC) | 3.45.3.0 |
| Servidor HTTP (API REST) | Javalin | 6.1.3 |
| Serialização JSON | Jackson | 2.17.0 |
| Log | SLF4J Simple | 2.0.12 |
| Build | Maven | 3.9.6 |
| Testes unitários / integração | JUnit 5 | 5.10.1 |
| Testes de API | Postman / Newman | v11 |
| Testes E2E | Playwright | 1.44.x |
| CI/CD | GitLab CI | — |
| IDE recomendada | IntelliJ IDEA | 2024+ |

> **Node.js 20+** é necessário para rodar o Playwright e o Newman. Baixe em https://nodejs.org (versão LTS).

---

## Estrutura do Projeto

```
Biblioteca/
├── src/
│   ├── main/java/com/example/biblioteca/
│   │   ├── Main.java                          # Ponto de entrada — CLI + API
│   │   ├── api/
│   │   │   └── ApiServer.java                 # Servidor REST Javalin (porta 8080)
│   │   ├── cli/
│   │   │   ├── Console.java                   # Utilitários de I/O do terminal
│   │   │   ├── TelaLogin.java                 # Autenticação (até 3 tentativas)
│   │   │   ├── MenuPrincipal.java             # Menu raiz — passa Usuario aos submenus
│   │   │   ├── MenuLivros.java                # CRUD de livros com controle de perfil
│   │   │   ├── MenuEmprestimos.java           # Empréstimos com controle de perfil
│   │   │   └── MenuUsuarios.java              # CRUD de usuários (apenas ADMIN)
│   │   ├── db/
│   │   │   └── DatabaseConnection.java        # Conexão SQLite + schema automático
│   │   ├── model/
│   │   │   ├── Livro.java
│   │   │   ├── Emprestimo.java
│   │   │   └── Usuario.java
│   │   ├── repository/
│   │   │   ├── LivroRepository.java           # CRUD em memória (testes unitários)
│   │   │   ├── LivroRepositoryDB.java         # CRUD de livros no SQLite
│   │   │   ├── EmprestimoRepository.java      # CRUD de empréstimos no SQLite
│   │   │   └── UsuarioRepository.java         # CRUD de usuários no SQLite
│   │   └── service/
│   │       ├── BibliotecaService.java         # Serviço em memória (testes unitários)
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
├── testes_api/
│   └── Biblioteca_API_Tests.postman_collection.json
├── testes_e2e/
│   ├── package.json
│   ├── playwright.config.js
│   └── tests/
│       └── biblioteca.e2e.test.js
├── docs/
│   ├── documentoVisaoHistoria.docx
│   ├── planoDeTeste.docx
│   ├── cadoDeTeste.docx
│   └── relatorioFinal.docx
├── pom.xml
├── .gitignore
└── README.md
```

---

## Banco de Dados

O arquivo `biblioteca.db` é criado automaticamente na raiz do projeto na primeira execução. O `.gitignore` já está configurado para não versionar este arquivo.

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

**Configurações SQLite aplicadas automaticamente:**

| PRAGMA | Valor | Motivo |
|--------|-------|--------|
| `autoCommit` | `true` | Evita `SQLITE_BUSY` |
| `journal_mode` | `WAL` | Melhor concorrência |
| `busy_timeout` | `3000` | Aguarda até 3s antes de falhar |
| `foreign_keys` | `ON` | Impede remover livro com empréstimo ativo |

---

## API REST

A API sobe automaticamente na porta **8080** ao executar o sistema.

**Base URL:** `http://localhost:8080`

### Autenticação

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `POST` | `/api/auth/login` | Autentica usuário | 200 / 401 |

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

### Empréstimos

| Método | Endpoint | Descrição | Status |
|--------|----------|-----------|--------|
| `GET` | `/api/emprestimos` | Lista empréstimos ativos | 200 |
| `GET` | `/api/emprestimos/atraso` | Lista em atraso | 200 |
| `GET` | `/api/emprestimos/usuario/{nome}` | Histórico por usuário | 200 |
| `POST` | `/api/emprestimos` | Realiza empréstimo | 201 / 400 / 409 |
| `PATCH` | `/api/emprestimos/{id}/devolver` | Registra devolução | 200 / 400 |

---

## Interface CLI

Ao executar o sistema a CLI é iniciada no terminal após o servidor API subir.

### Controle de acesso por perfil nos menus

As opções bloqueadas aparecem com o texto `[apenas BIBLIOTECARIO/ADMIN]`. Se o usuário digitar o número mesmo assim, recebe `[ERRO] Acesso negado`.

| Menu | Opções | ADMIN | BIBLIOTECARIO | LEITOR |
|------|--------|:-----:|:-------------:|:------:|
| Livros | Listar, buscar | ✅ | ✅ | ✅ |
| Livros | Cadastrar, editar, remover | ✅ | ✅ | ❌ |
| Empréstimos | Realizar, devolver | ✅ | ✅ | ❌ |
| Empréstimos | Listar ativos, atrasos | ✅ | ✅ | ✅ |
| Empréstimos | Histórico | ✅ qualquer | ✅ qualquer | ✅ próprio |
| Usuários | CRUD completo | ✅ | ❌ | ❌ |
| Dashboard | Resumo geral | ✅ | ✅ | ✅ |

---

## Como Executar no IntelliJ

> Não é necessário `mvn` no terminal do sistema. Tudo é feito dentro do IntelliJ.

**1. Abrir o projeto**

`File → Open` → selecionar a pasta que contém o `pom.xml` (`Biblioteca/Biblioteca`).
Quando aparecer o popup *"Maven project detected"*, clicar em **Load**.

**2. Sincronizar dependências**

`View → Tool Windows → Maven` → clicar no botão **↻ Reload All Maven Projects**.
O IntelliJ baixará automaticamente: `sqlite-jdbc`, `javalin`, `jackson-databind` e `slf4j`.

**3. Verificar o SDK**

`File → Project Structure → Project` → confirmar **Java 17** ou superior.
Se não tiver: `Add SDK → Download JDK → Eclipse Temurin 17`.

**4. Executar o sistema**

Abrir `Main.java` e clicar no triângulo verde ou `Shift + F10`.

**5. Executar os testes JUnit**

Painel Maven → `Lifecycle` → duplo clique em **test**.

**6. Silenciar warnings de inicialização** *(opcional)*

`Run → Edit Configurations → VM options`:
```
-Dorg.slf4j.simpleLogger.defaultLogLevel=warn --enable-native-access=ALL-UNNAMED
```

---

## Modos de Execução

Configure em `Run → Edit Configurations → Program arguments`:

| Argumento | Comportamento |
|-----------|--------------|
| *(sem argumento)* | Sobe a **API REST** (porta 8080) **e** a **CLI** |
| `--api` | Sobe **apenas a API** — ideal para testes com Postman e Playwright |
| `--cli` | Inicia **apenas a CLI** — sem servidor HTTP |

> Para os testes de API (Postman) e E2E (Playwright), o sistema deve estar rodando com `--api` ou sem argumento.

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
| Dashboard completo | ✅ | ✅ | ✅ |
| Todos os endpoints da API | ✅ | ✅ | ✅ |

---

## Testes

### Resumo geral

| Arquivo | Tipo | Testes | Resultado esperado |
|---------|------|:------:|-------------------|
| `LivroTest` | Unitário | 3 | ✅ PASS |
| `BibliotecaServiceTest` | Unitário | 4 | ✅ PASS |
| `EmprestimoTest` | Unitário | 10 | ✅ PASS |
| `BibliotecaServiceExtraTest` | Unitário | 9 | ✅ PASS |
| `LivroRepositoryTest` | Unitário | 7 | ✅ PASS |
| `LivroRepositoryDBTest` | Integração | 12 | ✅ PASS |
| `EmprestimoRepositoryTest` | Integração | 10 | ✅ PASS |
| `UsuarioRepositoryTest` | Integração | 11 | ✅ PASS |
| `BibliotecaServiceDBTest` | Integração | 15 | ✅ PASS |
| **Total JUnit** | | **84** | **81 PASS / 3 FAIL proposital** |
| **Postman collection** | API | 15 requests | ✅ PASS |
| **Playwright** | E2E | 5 cenários | ✅ PASS |

> ⚠️ `BibliotecaFalhaTest` contém **3 testes propositalmente incorretos** para demonstrar como o JUnit reporta falhas. Eles sempre falham — isso é esperado e documentado no trabalho.

### Testes JUnit — executar no IntelliJ

Painel Maven → `Lifecycle` → duplo clique em **test**. Saída esperada:

```
Tests run: 81, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> Os testes de integração usam `jdbc:sqlite::memory:` — banco criado e destruído na JVM, sem arquivo em disco.

### Testes de API — Postman

**Pré-requisito:** sistema rodando com `--api` (porta 8080 ativa).

A collection possui um grupo **SETUP** que limpa o estado anterior automaticamente antes de cada execução. Importe o arquivo e execute pelo Runner na ordem:

1. SETUP — Limpar estado anterior
2. AUTH
3. LIVROS
4. EMPRESTIMOS

Ou via Newman (linha de comando):

```bash
npm install -g newman
newman run testes_api/Biblioteca_API_Tests.postman_collection.json
```

### Testes E2E — Playwright

**Pré-requisitos:**
- Node.js 20+ instalado ([nodejs.org](https://nodejs.org))
- Sistema rodando com `--api` (porta 8080 ativa)

**Executar pelo terminal integrado do IntelliJ:**

Clique com botão direito na pasta `testes_e2e` no painel Project → **Open In → Terminal**

```bash
npm install
npx playwright install chromium
npx playwright test --reporter=html
npx playwright show-report
```

O relatório HTML abre automaticamente no navegador. Os 5 cenários cobrem:

| Teste | Fluxo |
|-------|-------|
| CT-E01 | Login → Cadastrar livro → Confirmar na listagem |
| CT-E02 | Criar livro → Emprestar → Confirmar indisponível |
| CT-E03 | Emprestar → Devolver → Livro volta a disponível → Histórico |
| CT-E04 | Credenciais inválidas → 401 → Login válido → 200 |
| CT-E05 | Emprestar → Tentar emprestar de novo → 409 bloqueado |

---

## CI/CD

O arquivo `.gitlab-ci.yml` define três estágios automáticos executados a cada push:

```
build  →  test  →  deploy (apenas branch master)
```

Os relatórios XML do Surefire (`target/surefire-reports/TEST-*.xml`) são publicados automaticamente como artefatos de teste no GitLab.

---

## Problemas conhecidos e soluções

| Problema | Solução |
|----------|---------|
| `SQLITE_BUSY` ao rodar o Main | Já corrigido: `autoCommit=true` + `busy_timeout=3000` + `try-with-resources` |
| Warnings de SLF4J e SQLite no console | Adicionar em VM options: `-Dorg.slf4j.simpleLogger.defaultLogLevel=warn --enable-native-access=ALL-UNNAMED` |
| `npm` não reconhecido | Instalar Node.js 20+ em nodejs.org e reiniciar o IntelliJ |
| Testes E2E falhando com erro de conexão | Verificar se a API está rodando: acessar `http://localhost:8080/api/livros` no navegador |
| Postman CT-A07 falhando com 409 | A collection tem um grupo SETUP que devolve empréstimos anteriores — execute na ordem correta |

---

## Documentação do Trabalho

Todos os documentos estão na pasta `docs/`:

| Arquivo                       | Conteúdo |
|-------------------------------|----------|
| `documentoVisaoHistoria.docx` | Visão do sistema, requisitos e 5 histórias de usuário com BDD (Gherkin) |
| `planoDeTeste.docx`           | Objetivo, escopo, ferramentas, pirâmide de testes e cronograma |
| `casoDeTeste.docx`            | Todos os casos com ID, descrição, passos, entradas e saídas esperadas |
| `relatorioFinal.docx`         | Relatório completo com 16 caixas de evidência e roteiro passo a passo de execução dos testes |

---

## Integrantes

| Nome | Responsabilidade |
|------|-----------------|
| Aluno 1 | Testes unitários — `Livro`, `Emprestimo` e `LivroRepository` |
| Aluno 2 | Testes de integração — `LivroRepositoryDB`, `EmprestimoRepository`, `UsuarioRepository`, `BibliotecaServiceDB` |
| Aluno 3 | Testes de API — Postman collection + Newman |
| Aluno 4 | Testes E2E — Playwright + Relatório Final |
