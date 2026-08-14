package com.example.urlshortener.service;

public class UrlNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UrlNotFoundException(String code) {
        super("URL curta não encontrada para o código '" + code + "'");
    }
}
