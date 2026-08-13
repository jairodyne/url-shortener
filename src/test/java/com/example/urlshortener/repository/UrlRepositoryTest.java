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
        UrlEntity saved = repository.save(url("abc123", null, "https://example.com"));

        Optional<UrlEntity> found = repository.findByCode(saved.getCode());

        assertTrue(found.isPresent());
        assertEquals("https://example.com", found.get().getOriginalUrl());
        assertEquals(0, found.get().getClickCount());
    }

    @Test
    public void findsByAlias() {
        repository.save(url("abc123", "meu-alias", "https://example.com"));

        Optional<UrlEntity> found = repository.findByAlias("meu-alias");

        assertTrue(found.isPresent());
        assertEquals("abc123", found.get().getCode());
    }

    @Test
    public void returnsEmptyWhenNotFound() {
        assertFalse(repository.findByCode("nao-existe").isPresent());
        assertFalse(repository.findByAlias("nao-existe").isPresent());
    }

    @Test(expected = PersistenceException.class)
    public void rejectsDuplicateCode() {
        repository.save(url("abc123", null, "https://a.com"));
        repository.save(url("abc123", "outro", "https://b.com"));
    }

    @Test
    public void rejectsDuplicateAlias() {
        repository.save(url("abc123", "ocupado", "https://a.com"));
        try {
            repository.save(url("def456", "ocupado", "https://b.com"));
            fail("duplicidade de alias deveria violar a constraint UNIQUE");
        } catch (PersistenceException expected) {
            assertTrue(expected.getCause() != null);
        }
    }

    @Test
    public void incrementsClickCount() {
        UrlEntity saved = repository.save(url("abc123", null, "https://example.com"));

        repository.incrementClicks(saved.getCode());
        repository.incrementClicks(saved.getCode());

        assertEquals(2, repository.findByCode(saved.getCode()).get().getClickCount());
    }

    private static UrlEntity url(String code, String alias, String original) {
        return new UrlEntity(code, alias, original, System.currentTimeMillis());
    }
}