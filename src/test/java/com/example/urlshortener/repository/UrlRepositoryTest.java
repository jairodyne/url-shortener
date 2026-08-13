package com.example.urlshortener.repository;

import com.example.urlshortener.model.UrlEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UrlRepositoryTest {

    private EntityManagerFactory emf;
    private UrlRepository repository;

    @Before
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("url-shortener-pu", dbProperties());
        repository = new UrlRepository(emf);
    }

    @After
    public void tearDown() {
        emf.close();
    }

    private static Map<String, String> dbProperties() {
        Map<String, String> props = new HashMap<>();
        props.put("javax.persistence.jdbc.url",
                "jdbc:h2:mem:urlshortener_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        return props;
    }

    @Test
    public void savesAndFindsByCode() {
        UrlEntity saved = repository.save(url("abc123", "https://example.com"));

        Optional<UrlEntity> found = repository.findByCode("abc123");

        assertTrue(found.isPresent());
        assertEquals("https://example.com", found.get().getOriginalUrl());
        assertEquals(0, found.get().getClickCount());
    }

    @Test
    public void returnsEmptyWhenNotFound() {
        assertFalse(repository.findByCode("nao-existe").isPresent());
        assertFalse(repository.existsByCode("nao-existe"));
    }

    @Test
    public void reportsExistenceByCode() {
        repository.save(url("abc123", "https://example.com"));

        assertTrue(repository.existsByCode("abc123"));
    }

    @Test
    public void rejectsDuplicateCode() {
        repository.save(url("abc123", "https://a.com"));
        try {
            repository.save(url("abc123", "https://b.com"));
            fail("duplicidade de code deveria violar a constraint UNIQUE");
        } catch (PersistenceException expected) {
            assertTrue(expected.getCause() != null);
        }
    }

    @Test
    public void incrementsClickCount() {
        UrlEntity saved = repository.save(url("abc123", "https://example.com"));

        repository.incrementClicks(saved.getCode());
        repository.incrementClicks(saved.getCode());

        assertEquals(2, repository.findByCode("abc123").get().getClickCount());
    }

    private static UrlEntity url(String code, String original) {
        return new UrlEntity(code, original, System.currentTimeMillis());
    }
}
