package com.example.urlshortener.service;

public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String code) {
        super("URL curta não encontrada para o código '" + code + "'");
    }
}
