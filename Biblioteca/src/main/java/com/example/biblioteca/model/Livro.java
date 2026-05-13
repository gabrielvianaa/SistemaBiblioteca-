package com.example.biblioteca.model;

public class Livro {

        private final String isbn;
        private final String titulo;
        private final String autor;
        private final int anoPub;
        private boolean disponivel;

        public Livro(String isbn, String titulo, String autor, int anoPub) {
            if (isbn == null || isbn.isBlank()) throw new IllegalArgumentException("ISBN invalido");
            if (titulo == null || titulo.isBlank()) throw new IllegalArgumentException("Titulo invalido");
            if (autor == null || autor.isBlank()) throw new IllegalArgumentException("Autor invalido");
            if (anoPub < 1000 || anoPub > 9999) throw new IllegalArgumentException("Ano invalido");
            this.isbn = isbn;
            this.titulo = titulo;
            this.autor = autor;
            this.anoPub = anoPub;
            this.disponivel = true;
        }

        public String getIsbn() {
            return isbn; }
        public String getTitulo() {
            return titulo; }
        public String getAutor() {
            return autor; }
        public int getAnoPub() {
            return anoPub; }
        public boolean isDisponivel() {
            return disponivel; }
        public void setDisponivel(boolean disponivel) {
            this.disponivel = disponivel; }
}
