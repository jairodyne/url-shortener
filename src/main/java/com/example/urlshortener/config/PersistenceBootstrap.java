package com.example.urlshortener.config;

import com.example.urlshortener.repository.UrlRepository;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Cria a {@link EntityManagerFactory} da persistence unit RESOURCE_LOCAL
 * (H2 em memória, sem datasource gerenciado pelo WildFly) e expõe o
 * {@link UrlRepository} para injeção CDI.
 */
@ApplicationScoped
public class PersistenceBootstrap {

    private EntityManagerFactory emf;

    @PostConstruct
    public void init() {
        emf = Persistence.createEntityManagerFactory("url-shortener-pu");
    }

    @PreDestroy
    public void destroy() {
        if (emf != null) {
            emf.close();
        }
    }

    @Produces
    @ApplicationScoped
    public UrlRepository produceUrlRepository() {
        return new UrlRepository(emf);
    }
}