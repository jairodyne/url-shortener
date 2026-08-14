package com.example.urlshortener.service;

public class AliasAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AliasAlreadyExistsException(String alias) {
        super("Alias '" + alias + "' já está em uso");
    }
}
