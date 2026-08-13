package com.example.urlshortener.service;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@ApplicationScoped
public class UrlValidator {

    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_ALIAS_LENGTH = 30;
    private static final Pattern ALIAS_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");

    public void validateUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidUrlException("URL não pode ser vazia");
        }
        String trimmed = url.trim();
        if (trimmed.length() > MAX_URL_LENGTH) {
            throw new InvalidUrlException("URL excede o tamanho máximo de " + MAX_URL_LENGTH + " caracteres");
        }
        try {
            URI uri = new URI(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new InvalidUrlException("URL deve começar com http:// ou https://");
            }
            if (uri.getHost() == null) {
                throw new InvalidUrlException("URL inválida: host ausente");
            }
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("URL inválida: " + e.getMessage());
        }
    }

    public void validateAlias(String alias) {
        if (alias == null || alias.trim().isEmpty()) {
            throw new InvalidUrlException("Alias não pode ser vazio");
        }
        String trimmed = alias.trim();
        if (trimmed.length() > MAX_ALIAS_LENGTH) {
            throw new InvalidUrlException("Alias excede o tamanho máximo de " + MAX_ALIAS_LENGTH + " caracteres");
        }
        if (!ALIAS_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidUrlException("Alias só pode conter letras, números, hífen e sublinhado");
        }
    }
}