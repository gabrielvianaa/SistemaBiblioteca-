package com.example.biblioteca.cli;

import java.util.Scanner;

public class Console {

    private static final Scanner sc = new Scanner(System.in);

    public static String lerTexto(String prompt) {
        System.out.print("  " + prompt + ": ");
        return sc.nextLine().trim();
    }

    public static String lerTextoObrigatorio(String prompt) {
        while (true) {
            String valor = lerTexto(prompt);
            if (!valor.isBlank()) return valor;
            erro("Campo obrigatorio. Tente novamente.");
        }
    }

    public static int lerInteiro(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(lerTextoObrigatorio(prompt));
            } catch (NumberFormatException e) {
                erro("Digite um numero valido.");
            }
        }
    }

    public static int lerOpcao(int min, int max) {
        while (true) {
            int op = lerInteiro("Opcao");
            if (op >= min && op <= max) return op;
            erro("Opcao invalida. Escolha entre " + min + " e " + max + ".");
        }
    }

    public static boolean confirmar(String mensagem) {
        System.out.print("  " + mensagem + " (s/n): ");
        return sc.nextLine().trim().equalsIgnoreCase("s");
    }

    public static void limpar() {
        for (int i = 0; i < 3; i++) System.out.println();
    }

    public static void titulo(String texto) {
        String linha = "=".repeat(58);
        System.out.println();
        System.out.println(linha);
        System.out.println("  " + texto);
        System.out.println(linha);
    }

    public static void subtitulo(String texto) {
        System.out.println("\n  -- " + texto + " --");
    }

    public static void sucesso(String msg) {
        System.out.println("\n  [OK] " + msg);
    }

    public static void erro(String msg) {
        System.out.println("\n  [ERRO] " + msg);
    }

    public static void aviso(String msg) {
        System.out.println("\n  [!] " + msg);
    }

    public static void info(String msg) {
        System.out.println("  " + msg);
    }

    public static void separador() {
        System.out.println("  " + "-".repeat(54));
    }

    public static void pausar() {
        System.out.print("\n  Pressione ENTER para continuar...");
        sc.nextLine();
    }

    public static void opcao(int num, String descricao) {
        System.out.printf("  [%d] %s%n", num, descricao);
    }
}
