package com.example.biblioteca.repository;

import com.example.biblioteca.model.Livro;
import java.util.*;
import java.util.stream.Collectors;

public class LivroRepository {
    private final Map<String, Livro> acervo = new LinkedHashMap<>();

    public void adicionar(Livro livro) {
        if (acervo.containsKey(livro.getIsbn()))
            throw new IllegalArgumentException("ISBN ja existe: " + livro.getIsbn());
        acervo.put(livro.getIsbn(), livro);
    }

    public Optional<Livro> buscarPorIsbn(String isbn) {
        return Optional.ofNullable(acervo.get(isbn));
    }

    public List<Livro> buscarPorAutor(String autor) {
        return acervo.values().stream()
                .filter(l -> l.getAutor().toLowerCase().contains(autor.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<Livro> listarDisponiveis() {
        return acervo.values().stream()
                .filter(Livro::isDisponivel)
                .collect(Collectors.toList());
    }

    public List<Livro> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(acervo.values()));
    }

    public int total() { return acervo.size(); }
}