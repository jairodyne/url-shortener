package com.example.urlshortener.service;

public class AliasAlreadyExistsException extends RuntimeException {

    public AliasAlreadyExistsException(String alias) {
        super("Alias '" + alias + "' já está em uso");
    }
}
