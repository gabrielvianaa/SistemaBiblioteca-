package com.example.biblioteca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerencia a conexao com o banco de dados SQLite.
 *
 * Correcoes aplicadas:
 *   1. autoCommit = true  — evita transacoes abertas travando o arquivo
 *   2. PRAGMA busy_timeout — espera ate 3s se outro processo tiver o banco
 *   3. WAL configurado ANTES de criar tabelas
 *   4. Statements fechados explicitamente (try-with-resources)
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:biblioteca.db";
    private static Connection   instance;

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            instance = abrir(URL);
        }
        return instance;
    }

    /** Conexao independente para testes (banco em memoria). */
    public static Connection novaConexao(String url) throws SQLException {
        return abrir(url);
    }

    /** Abre e configura uma conexao SQLite. */
    private static Connection abrir(String url) throws SQLException {
        Connection conn = DriverManager.getConnection(url);

        try (Statement st = conn.createStatement()) {
            // 1. Autocommit ligado — sem transacao implicita pendente
            conn.setAutoCommit(true);

            // 2. WAL: melhor concorrencia, evita SQLITE_BUSY em leituras
            st.execute("PRAGMA journal_mode=WAL;");

            // 3. Timeout de 3 segundos antes de desistir com SQLITE_BUSY
            st.execute("PRAGMA busy_timeout=3000;");

            // 4. Chaves estrangeiras
            st.execute("PRAGMA foreign_keys=ON;");
        }

        criarTabelas(conn);
        return conn;
    }

    public static void criarTabelas(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS livros (
                    isbn        TEXT PRIMARY KEY,
                    titulo      TEXT NOT NULL,
                    autor       TEXT NOT NULL,
                    ano_pub     INTEGER NOT NULL,
                    disponivel  INTEGER NOT NULL DEFAULT 1
                        CHECK (disponivel IN (0,1))
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome    TEXT NOT NULL,
                    login   TEXT NOT NULL UNIQUE,
                    senha   TEXT NOT NULL,
                    perfil  TEXT NOT NULL DEFAULT 'LEITOR'
                        CHECK (perfil IN ('ADMIN','BIBLIOTECARIO','LEITOR'))
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS emprestimos (
                    id               TEXT PRIMARY KEY,
                    isbn             TEXT NOT NULL,
                    nome_usuario     TEXT NOT NULL,
                    data_emprestimo  TEXT NOT NULL,
                    data_devolucao   TEXT,
                    devolvido        INTEGER NOT NULL DEFAULT 0
                        CHECK (devolvido IN (0,1)),
                    FOREIGN KEY (isbn) REFERENCES livros(isbn) ON DELETE RESTRICT
                );
            """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_livros_autor ON livros(autor COLLATE NOCASE);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_emp_usuario  ON emprestimos(nome_usuario COLLATE NOCASE);");
            st.execute("CREATE INDEX IF NOT EXISTS idx_emp_isbn     ON emprestimos(isbn);");
        }
    }

    public static void fechar() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                instance = null;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao fechar conexao: " + e.getMessage());
        }
    }
}
