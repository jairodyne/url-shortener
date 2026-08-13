package com.example.urlshortener.service;

import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.repository.UrlRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UrlShortenerServiceTest {

    @Mock
    private UrlRepository repository;

    private UrlShortenerService service;

    @Before
    public void setUp() {
        service = new UrlShortenerService(repository, new CodeGenerator(), new UrlValidator());
    }

    @Test
    public void createsWithoutAliasGeneratesShortCode() {
        when(repository.existsByCode(any())).thenReturn(false);
        when(repository.save(any(UrlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity created = service.createUrl("https://example.com/um/tres", null);

        ArgumentCaptor<UrlEntity> captor = ArgumentCaptor.forClass(UrlEntity.class);
        verify(repository).save(captor.capture());
        UrlEntity saved = captor.getValue();
        assertEquals("https://example.com/um/tres", saved.getOriginalUrl());
        assertTrue("código gerado deve ser base62 de 6 chars",
                saved.getCode().matches("[0-9a-zA-Z]{6}"));
        assertEquals(created.getCode(), saved.getCode());
    }

    @Test
    public void createsWithAliasUsesAliasAsCode() {
        when(repository.findByCode("meu-alias")).thenReturn(Optional.empty());
        when(repository.save(any(UrlEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createUrl("https://example.com", "meu-alias");

        ArgumentCaptor<UrlEntity> captor = ArgumentCaptor.forClass(UrlEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("meu-alias", captor.getValue().getCode());
    }

    @Test(expected = AliasAlreadyExistsException.class)
    public void rejectsAliasAlreadyInUse() {
        when(repository.findByCode("ocupado")).thenReturn(Optional.of(entity("ocupado")));

        service.createUrl("https://example.com", "ocupado");
    }

    @Test(expected = AliasAlreadyExistsException.class)
    public void doesNotSaveWhenAliasInUse() {
        when(repository.findByCode("ocupado")).thenReturn(Optional.of(entity("ocupado")));

        service.createUrl("https://example.com", "ocupado");

        verify(repository, never()).save(any(UrlEntity.class));
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsInvalidUrl() {
        service.createUrl("sem-scheme", null);
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsInvalidAlias() {
        service.createUrl("https://example.com", "tem espaço");
    }

    @Test
    public void resolvesAndCountsClick() {
        UrlEntity entity = entity("abc123");
        when(repository.findByCode("abc123")).thenReturn(Optional.of(entity));

        UrlEntity resolved = service.resolve("abc123");

        assertEquals(entity.getOriginalUrl(), resolved.getOriginalUrl());
        verify(repository).incrementClicks("abc123");
    }

    @Test(expected = UrlNotFoundException.class)
    public void throwsWhenCodeNotFound() {
        when(repository.findByCode("xpto")).thenReturn(Optional.empty());

        service.resolve("xpto");
    }

    @Test
    public void buildsShortUrlWithoutDuplicateSlash() {
        String shortUrl = service.buildShortUrl("http://localhost:8080/url-shortener", "abc123");
        assertEquals("http://localhost:8080/url-shortener/r/abc123", shortUrl);

        String withSlash = service.buildShortUrl("http://localhost:8080/url-shortener/", "abc123");
        assertEquals("http://localhost:8080/url-shortener/r/abc123", withSlash);
    }

    @Test
    public void concurrentCreatesProduceDistinctCodesWithRealRepository() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("url-shortener-pu", dbProperties());
        try {
            UrlShortenerService realService =
                    new UrlShortenerService(new UrlRepository(emf), new CodeGenerator(), new UrlValidator());
            int n = 20;
            ExecutorService pool = Executors.newFixedThreadPool(n);
            try {
                Set<String> codes = new HashSet<>();
                for (Future<UrlEntity> future : submitCreates(pool, realService, n)) {
                    codes.add(future.get().getCode());
                }
                assertEquals("requisições simultâneas devem produzir códigos distintos", n, codes.size());
            } finally {
                pool.shutdownNow();
            }
        } finally {
            emf.close();
        }
    }

    private java.util.List<Future<UrlEntity>> submitCreates(ExecutorService pool, UrlShortenerService s, int n) {
        java.util.List<Future<UrlEntity>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            final String url = "https://example.com/item-" + i;
            futures.add(pool.submit(() -> s.createUrl(url, null)));
        }
        return futures;
    }

    private static UrlEntity entity(String code) {
        return new UrlEntity(code, "https://example.com", System.currentTimeMillis());
    }

    private static Map<String, String> dbProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.url",
                "jdbc:h2:mem:urlshortener_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        return props;
    }
}
