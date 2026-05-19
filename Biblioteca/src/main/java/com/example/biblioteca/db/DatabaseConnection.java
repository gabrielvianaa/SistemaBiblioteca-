package com.example.biblioteca.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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

    public static Connection novaConexao(String url) throws SQLException {
        return abrir(url);
    }

    private static Connection abrir(String url) throws SQLException {
        Connection conn = DriverManager.getConnection(url);

        try (Statement st = conn.createStatement()) {
            conn.setAutoCommit(true);
            st.execute("PRAGMA journal_mode=WAL;");
            st.execute("PRAGMA busy_timeout=3000;");
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
