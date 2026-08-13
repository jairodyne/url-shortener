package com.example.urlshortener.service;

import org.junit.Test;

import static org.junit.Assert.fail;

public class UrlValidatorTest {

    private final UrlValidator validator = new UrlValidator();

    @Test
    public void acceptsValidHttpAndHttpsUrls() {
        validator.validateUrl("https://example.com/path");
        validator.validateUrl("http://example.com:8080/a?b=c");
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsEmptyUrl() {
        validator.validateUrl("   ");
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsNullUrl() {
        validator.validateUrl(null);
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsUrlWithoutScheme() {
        validator.validateUrl("example.com");
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsNonHttpScheme() {
        validator.validateUrl("ftp://example.com");
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsMalformedUrl() {
        validator.validateUrl("https://");
    }

    @Test
    public void acceptsValidAlias() {
        validator.validateAlias("meu-alias_123");
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsAliasWithSpaces() {
        validator.validateAlias("meu alias");
    }

    @Test(expected = InvalidUrlException.class)
    public void rejectsEmptyAlias() {
        validator.validateAlias("");
    }

    @Test
    public void rejectsOversizedAlias() {
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 31; i++) {
            big.append("a");
        }
        try {
            validator.validateAlias(big.toString());
            fail("alias com 31 caracteres deveria ser rejeitado");
        } catch (InvalidUrlException expected) {
            // ok
        }
    }
}
