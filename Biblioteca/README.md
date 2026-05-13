# 📚 Sistema de Biblioteca

> Sistema de Gerenciamento de Livros -> Trabalho Teste de Software

Sistema de gerenciamento de acervo de livros e empréstimos com interface de linha de comando (CLI), banco de dados SQLite persistente e cobertura completa de testes unitários e de integração.

---

## 📋 Sumário

- [Sobre o Sistema](#sobre-o-sistema)
- [Tecnologias](#tecnologias)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Banco de Dados](#banco-de-dados)
- [Interface CLI](#interface-cli)
- [Como Executar no IntelliJ](#como-executar-no-intellij)
- [Usuários Padrão](#usuários-padrão)
- [Funcionalidades por Perfil](#funcionalidades-por-perfil)
- [Testes](#testes)
- [Documentação do Trabalho](#documentação-do-trabalho)
- [Integrantes](#integrantes)

---

## Sobre o Sistema

O **Sistema de Biblioteca** permite o gerenciamento completo de um acervo de livros e o controle de empréstimos e devoluções. O sistema possui autenticação com três perfis de acesso distintos, CRUD completo de livros e usuários, registro de empréstimos com prazo automático de 14 dias, alertas de atraso e histórico por usuário.

Todos os dados são persistidos em um arquivo **SQLite** local (`biblioteca.db`) criado automaticamente na primeira execução.

---

## Tecnologias

| Camada | Tecnologia | Versão |
|--------|-----------|--------|
| Linguagem | Java | 17 |
| Banco de dados | SQLite (via JDBC) | 3.45.3.0 |
| Build | Maven | 3.9.6 |
| Testes unitários | JUnit 5 | 5.10.1 |
| Empacotamento | Maven Shade Plugin (fat JAR) | 3.5.1 |
| CI/CD | GitLab CI | — |

---

## Estrutura do Projeto

```
Biblioteca/
├── src/
│   ├── main/java/com/example/biblioteca/
│   │   ├── Main.java                          # Ponto de entrada
│   │   ├── cli/
│   │   │   ├── Console.java                   # Utilitários de I/O
│   │   │   ├── TelaLogin.java                 # Autenticação (até 3 tentativas)
│   │   │   ├── MenuPrincipal.java             # Menu raiz + controle de perfil
│   │   │   ├── MenuLivros.java                # CRUD completo de livros
│   │   │   ├── MenuEmprestimos.java           # Empréstimos e devoluções
│   │   │   └── MenuUsuarios.java              # CRUD de usuários (apenas ADMIN)
│   │   ├── db/
│   │   │   └── DatabaseConnection.java        # Conexão SQLite + criação do schema
│   │   ├── model/
│   │   │   ├── Livro.java                     # Entidade livro com validações
│   │   │   ├── Emprestimo.java                # Entidade empréstimo + cálculo de atraso
│   │   │   └── Usuario.java                   # Entidade usuário com perfil
│   │   ├── repository/
│   │   │   ├── LivroRepositoryDB.java         # CRUD de livros no SQLite
│   │   │   ├── EmprestimoRepository.java      # CRUD de empréstimos no SQLite
│   │   │   ├── UsuarioRepository.java         # CRUD de usuários no SQLite
│   │   │   └── LivroRepository.java           # Implementação em memória (usada nos testes unitários)
│   │   └── service/
│   │       ├── BibliotecaServiceDB.java        # Serviço principal com banco
│   │       └── BibliotecaService.java          # Serviço em memória (usado nos testes unitários)
│   └── test/java/com/example/biblioteca/
│       ├── LivroTest.java                     # Testes unitários da entidade Livro
│       ├── BibliotecaServiceTest.java         # Testes unitários do serviço em memória
│       ├── EmprestimoTest.java                # Testes unitários da entidade Emprestimo
│       ├── BibliotecaServiceExtraTest.java    # Testes extras do serviço (busca, atraso, histórico)
│       ├── LivroRepositoryTest.java           # Testes unitários do repositório em memória
│       ├── LivroRepositoryDBTest.java         # Testes de integração: LivroRepositoryDB + SQLite
│       ├── EmprestimoRepositoryTest.java      # Testes de integração: EmprestimoRepository + SQLite
│       ├── UsuarioRepositoryTest.java         # Testes de integração: UsuarioRepository + SQLite
│       └── BibliotecaServiceDBTest.java       # Testes de integração: serviço completo + SQLite
├── testes_api/
│   └── Biblioteca_API_Tests.postman_collection.json
├── testes_e2e/
│   ├── biblioteca.e2e.test.js
│   └── playwright.config.js
├── docs/
│   ├── 01_Documento_de_Visao_e_Historias_de_Usuario.docx
│   ├── 02_Plano_de_Testes.docx
│   ├── 03_Casos_de_Teste.docx
│   └── 04_Relatorio_Final.docx
├── pom.xml
├── .gitlab-ci.yml
└── README.md
└── .gitignore
```

---

## Banco de Dados

O arquivo `biblioteca.db` é criado automaticamente na raiz do projeto na primeira execução. O schema contém três tabelas:

```sql
-- Acervo de livros
CREATE TABLE livros (
    isbn        TEXT PRIMARY KEY,
    titulo      TEXT NOT NULL,
    autor       TEXT NOT NULL,
    ano_pub     INTEGER NOT NULL,
    disponivel  INTEGER NOT NULL DEFAULT 1   -- 0 = emprestado, 1 = disponível
);

-- Usuários do sistema
CREATE TABLE usuarios (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    nome    TEXT NOT NULL,
    login   TEXT NOT NULL UNIQUE,
    senha   TEXT NOT NULL,
    perfil  TEXT NOT NULL DEFAULT 'LEITOR'  -- ADMIN | BIBLIOTECARIO | LEITOR
);

-- Registro de empréstimos
CREATE TABLE emprestimos (
    id               TEXT PRIMARY KEY,        -- formato: EMP-0001
    isbn             TEXT NOT NULL,
    nome_usuario     TEXT NOT NULL,
    data_emprestimo  TEXT NOT NULL,           -- ISO: YYYY-MM-DD
    data_devolucao   TEXT,                    -- NULL se ainda ativo
    devolvido        INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (isbn) REFERENCES livros(isbn) ON DELETE RESTRICT
);
```

**Configurações de conexão aplicadas automaticamente:**
- `PRAGMA journal_mode=WAL` — melhor concorrência, evita `SQLITE_BUSY`
- `PRAGMA busy_timeout=3000` — aguarda até 3 segundos antes de falhar
- `PRAGMA foreign_keys=ON` — impede remover livro com empréstimo ativo
- `autoCommit=true` — sem transações implícitas pendentes

---

## Interface CLI

O sistema é operado inteiramente via terminal. Ao executar o `Main`, o fluxo é:

```
Execução do Main
      │
      ▼
Conecta ao SQLite  ──► Cria banco e tabelas (se não existirem)
      │
      ▼
Carga inicial  ──► Insere 3 usuários + 7 livros (apenas banco vazio)
      │
      ▼
Tela de Login  ──► Até 3 tentativas
      │
      ▼
Menu Principal  ──► Opções filtradas pelo perfil do usuário logado
      │
      ├──► [1] Gerenciar Livros
      ├──► [2] Gerenciar Empréstimos
      ├──► [3] Gerenciar Usuários  (somente ADMIN)
      ├──► [4] Dashboard
      └──► [0] Sair
```

### Menu de Livros

| Opção | Operação | Descrição |
|-------|----------|-----------|
| 1 | **Cadastrar** | ISBN, título, autor e ano obrigatórios |
| 2 | **Listar todos** | Tabela com ISBN, título, autor, ano e status |
| 3 | **Buscar por ISBN** | Retorna um livro exato ou avisa se não encontrado |
| 4 | **Buscar por autor** | Busca parcial e case-insensitive |
| 5 | **Listar disponíveis** | Somente livros com status `DISPONIVEL` |
| 6 | **Editar** | Altera título, autor e/ou ano; ENTER mantém valor atual |
| 7 | **Remover** | Confirmação obrigatória; bloqueado se houver empréstimo ativo |

### Menu de Empréstimos

| Opção | Operação | Descrição |
|-------|----------|-----------|
| 1 | **Realizar empréstimo** | Exibe livros disponíveis; ENTER na data usa a data de hoje |
| 2 | **Registrar devolução** | Exibe empréstimos ativos; ENTER na data usa a data de hoje |
| 3 | **Listar ativos** | Todos os empréstimos não devolvidos com prazo |
| 4 | **Listar em atraso** | Empréstimos vencidos com quantidade de dias de atraso |
| 5 | **Histórico por usuário** | Todos os empréstimos (ativos e devolvidos) de um usuário |

> O prazo padrão de devolução é de **14 dias** a partir da data do empréstimo.

### Menu de Usuários (apenas ADMIN)

| Opção | Operação | Descrição |
|-------|----------|-----------|
| 1 | **Cadastrar** | Nome, login, senha e perfil (`ADMIN`, `BIBLIOTECARIO` ou `LEITOR`) |
| 2 | **Listar** | Tabela com ID, nome, login e perfil de todos os usuários |
| 3 | **Alterar senha** | Atualiza a senha de qualquer usuário pelo login |
| 4 | **Remover** | Confirmação obrigatória antes de excluir |

### Dashboard

Exibe um resumo do sistema ao logar:
- Total de livros, disponíveis e emprestados
- Total de empréstimos ativos e em atraso
- Total de usuários cadastrados
- Alerta visual com detalhes de cada empréstimo em atraso
- Lista dos 5 primeiros livros disponíveis

---

## Como Executar no IntelliJ

> Não é necessário `mvn` no terminal do sistema. Tudo é feito dentro do IntelliJ.

**1. Abrir o projeto**

`File → Open` → selecione a pasta que contém o `pom.xml` (`Biblioteca/Biblioteca`).
Quando aparecer o popup *"Maven project detected"*, clique em **Load** ou **Trust Project**.

**2. Sincronizar dependências (baixar SQLite)**

`View → Tool Windows → Maven` → clique no botão **↻ Reload All Maven Projects**.
O IntelliJ baixará o `sqlite-jdbc` automaticamente.

**3. Verificar o SDK**

`File → Project Structure → Project` → confirme **Java 17** ou superior.
Se não tiver: `Add SDK → Download JDK → Eclipse Temurin 17`.

**4. Executar o sistema**

Abra `Main.java` e clique no **triângulo verde** na margem esquerda, ou use `Shift + F10`.

**5. Executar os testes**

Painel Maven → `Lifecycle` → duplo clique em **test**.
Ou clique com botão direito em `src/test/java` → **Run 'All Tests'**.

**6. Onde fica o banco**

O arquivo `biblioteca.db` aparece no painel **Project** do IntelliJ após a primeira execução.
Pode ser aberto com [DB Browser for SQLite](https://sqlitebrowser.org/) ou [DBeaver](https://dbeaver.io/).

---

## Usuários Padrão

Criados automaticamente na **primeira execução** (quando o banco está vazio):

| Login | Senha | Perfil |
|-------|-------|--------|
| `admin` | `admin123` | ADMIN |
| `maria` | `maria123` | BIBLIOTECARIO |
| `joao` | `joao123` | LEITOR |

---

## Funcionalidades por Perfil

| Funcionalidade | ADMIN | BIBLIOTECARIO | LEITOR |
|---------------|:-----:|:-------------:|:------:|
| Consultar livros | ✅ | ✅ | ✅ |
| Cadastrar / Editar / Remover livros | ✅ | ✅ | ❌ |
| Realizar empréstimo | ✅ | ✅ | ❌ |
| Registrar devolução | ✅ | ✅ | ❌ |
| Ver empréstimos ativos e atrasos | ✅ | ✅ | ✅ |
| Gerenciar usuários | ✅ | ❌ | ❌ |
| Dashboard completo | ✅ | ✅ | ✅ |

---

## Testes

### Visão geral

| Arquivo | Tipo | Testes | O que cobre |
|---------|------|:------:|-------------|
| `LivroTest` | Unitário | 3 | Validações da entidade `Livro` |
| `BibliotecaServiceTest` | Unitário | 4 | Serviço em memória (empréstimo, devolução) |
| `EmprestimoTest` | Unitário | 7 | Construção, devolução e cálculo de atraso |
| `BibliotecaServiceExtraTest` | Unitário | 7 | Busca por autor, atrasos, histórico |
| `LivroRepositoryTest` | Unitário | 5 | Repositório em memória |
| `LivroRepositoryDBTest` | Integração | 12 | CRUD de livros no SQLite |
| `EmprestimoRepositoryTest` | Integração | 10 | CRUD de empréstimos no SQLite |
| `UsuarioRepositoryTest` | Integração | 11 | CRUD + autenticação no SQLite |
| `BibliotecaServiceDBTest` | Integração | 15 | Serviço completo + banco real |
| `BibliotecaFalhaTest` | Proposital | 3 | Demonstração de testes incorretos ⚠️ |
| **Total** | | **77** | |

> ⚠️ `BibliotecaFalhaTest` contém **3 testes propositalmente incorretos** para demonstrar como o JUnit reporta falhas. Eles sempre falham — esse comportamento é esperado e documentado.

### Executar somente os testes corretos

No painel Maven, duplo clique em `Lifecycle → test`, ou via terminal:

```bash
mvn test -Dtest="LivroTest,BibliotecaServiceTest,EmprestimoTest,BibliotecaServiceExtraTest,LivroRepositoryTest,LivroRepositoryDBTest,EmprestimoRepositoryTest,UsuarioRepositoryTest,BibliotecaServiceDBTest"
```

Saída esperada:

```
Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Testes de integração com banco em memória

Os testes de integração usam `jdbc:sqlite::memory:` — um banco SQLite criado e destruído dentro da JVM para cada classe de teste. Isso garante isolamento total sem criar arquivos em disco.

```java
// Exemplo de setup em LivroRepositoryDBTest
conn = DatabaseConnection.novaConexao("jdbc:sqlite::memory:");
repo = new LivroRepositoryDB(conn);
```

### Testes de API (Postman / Newman)

Importe o arquivo `testes_api/Biblioteca_API_Tests.postman_collection.json` no Postman e execute com a API rodando em `http://localhost:8080`. Ou via linha de comando:

```bash
npm install -g newman
newman run testes_api/Biblioteca_API_Tests.postman_collection.json
```

Resultado esperado: **11 requests | 22 assertions | 0 failed**

### Testes E2E (Playwright)

```bash
cd testes_e2e
npm install @playwright/test
npx playwright install chromium
npx playwright test --config=playwright.config.js
```

Resultado esperado: **5 passed**

---

## CI/CD

O arquivo `.gitlab-ci.yml` define três estágios automáticos:

```
build  →  test  →  deploy (apenas branch master)
```

```yaml
image: maven:3.9.6-eclipse-temurin-17

stages:
  - build    # mvn clean compile
  - test     # mvn test  +  publica relatório JUnit no GitLab
  - deploy   # mvn package -DskipTests  (simula deploy)
```

Os relatórios XML do Surefire (`target/surefire-reports/TEST-*.xml`) são publicados automaticamente como artefatos de teste no GitLab.

---

## Documentação do Trabalho

Todos os documentos estão na pasta `docs/`:

| Arquivo | Conteúdo |
|---------|----------|
| `01_Documento_de_Visao_e_Historias_de_Usuario.docx` | Visão do sistema, requisitos e 5 histórias de usuário com critérios de aceitação e cenários BDD (Gherkin) |
| `02_Plano_de_Testes.docx` | Objetivo, escopo, ferramentas, pirâmide de testes, estratégia e cronograma |
| `03_Casos_de_Teste.docx` | 28+ casos de teste detalhados (unitários, API e E2E) com ID, passos, entradas e saídas esperadas |
| `04_Relatorio_Final.docx` | Relatório completo com introdução, planejamento, resultados, evidências e conclusão |

---

## Integrantes

| Nome | Responsabilidade |
|------|-----------------|
| Aluno 1 | Testes unitários — `Livro` e `Emprestimo` |
| Aluno 2 | Testes unitários — `BibliotecaService` e `LivroRepository` |
| Aluno 3 | Testes de API — Postman e Newman |
| Aluno 4 | Testes E2E — Playwright e Relatório Final |
