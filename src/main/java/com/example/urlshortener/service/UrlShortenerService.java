package com.example.urlshortener.service;

import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.repository.UrlRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;

/**
 * Orquestra o fluxo de criação e resolução de URLs curtas.
 *
 * {@link #createUrl(String, String)} é {@code synchronized}: atende o requisito
 * de processar apenas uma requisição por vez. Toda a sequência
 * validar -> verificar alias -> gerar código -> persistir é executada sob o mesmo
 * lock, então não há corrida entre checagem e gravação. A constraint UNIQUE no
 * banco permanece como backstop (retry para códigos gerados).
 */
@ApplicationScoped
public class UrlShortenerService {

    private final UrlRepository repository;
    private final CodeGenerator codeGenerator;
    private final UrlValidator validator;

    protected UrlShortenerService() {
        this(null, null, null);
    }

    @Inject
    public UrlShortenerService(UrlRepository repository, CodeGenerator codeGenerator, UrlValidator validator) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.validator = validator;
    }

    public synchronized UrlEntity createUrl(String url, String alias) {
        validator.validateUrl(url);
        String normalizedUrl = url.trim();

        boolean customAlias = isCustomAlias(alias);
        if (customAlias) {
            validator.validateAlias(alias);
        }

        String code;
        if (customAlias) {
            if (repository.findByCode(alias).isPresent()) {
                throw new AliasAlreadyExistsException(alias);
            }
            code = alias;
        } else {
            code = codeGenerator.generateUniqueCode(repository::existsByCode);
        }

        try {
            return repository.save(new UrlEntity(code, normalizedUrl, System.currentTimeMillis()));
        } catch (PersistenceException e) {
            if (customAlias) {
                throw new AliasAlreadyExistsException(alias);
            }
            throw new IllegalStateException("Falha ao persistir URL curta após gerar código único", e);
        }
    }

    public UrlEntity resolve(String code) {
        return repository.findByCode(code)
                .map(entity -> {
                    repository.incrementClicks(code);
                    return entity;
                })
                .orElseThrow(() -> new UrlNotFoundException(code));
    }

    public String buildShortUrl(String baseUrl, String code) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/r/" + code;
    }

    private boolean isCustomAlias(String alias) {
        return alias != null && !alias.trim().isEmpty();
    }
}
