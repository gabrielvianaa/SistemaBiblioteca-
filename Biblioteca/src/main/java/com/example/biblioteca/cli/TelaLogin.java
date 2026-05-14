package com.example.biblioteca.cli;

import com.example.biblioteca.model.Usuario;
import com.example.biblioteca.service.BibliotecaServiceDB;

public class TelaLogin {

    private static final int MAX_TENTATIVAS = 3;
    private final BibliotecaServiceDB service;

    public TelaLogin(BibliotecaServiceDB service) {
        this.service = service;
    }

    public Usuario autenticar() {
        exibirBanner();

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            System.out.println();
            String login = Console.lerTextoObrigatorio("Login");
            String senha = Console.lerTextoObrigatorio("Senha");

            try {
                Usuario usuario = service.login(login, senha);
                Console.sucesso("Bem-vindo(a), " + usuario.getNome()
                        + "! Perfil: " + usuario.getPerfil());
                Console.pausar();
                return usuario;
            } catch (IllegalArgumentException e) {
                int restantes = MAX_TENTATIVAS - tentativa;
                if (restantes > 0) {
                    Console.erro("Login ou senha incorretos. "
                            + restantes + " tentativa(s) restante(s).");
                } else {
                    Console.erro("Numero maximo de tentativas atingido. Encerrando.");
                }
            }
        }
        return null;
    }

    private void exibirBanner() {
        System.out.println();
        System.out.println("  ========================================================");
        System.out.println("  |                                                      |");
        System.out.println("  |          SISTEMA DE BIBLIOTECA  v1.0                |");
        System.out.println("  |         Gerenciamento de Acervo e Emprestimos       |");
        System.out.println("  |                                                      |");
        System.out.println("  ========================================================");
        System.out.println();
        System.out.println("  Usuarios padrao (primeira execucao):");
        System.out.println("    admin / admin123   -> ADMIN");
        System.out.println("    maria / maria123   -> BIBLIOTECARIO");
        System.out.println("    joao  / joao123    -> LEITOR");
        System.out.println();
        System.out.println("  --------------------------------------------------------");
        System.out.println("  Faca login para continuar:");
    }
}
