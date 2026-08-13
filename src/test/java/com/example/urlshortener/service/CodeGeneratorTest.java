package com.example.urlshortener.service;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CodeGeneratorTest {

    @Test
    public void generatesBase62CodesOfExpectedLength() {
        CodeGenerator generator = new CodeGenerator();

        String code = generator.generateUniqueCode(c -> false);

        assertTrue("código deve ter " + CodeGenerator.CODE_LENGTH + " caracteres alfanuméricos",
                code.matches("[0-9a-zA-Z]{" + CodeGenerator.CODE_LENGTH + "}"));
    }

    @Test
    public void generatesDistinctCodesSequentially() {
        CodeGenerator generator = new CodeGenerator();
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generateUniqueCode(c -> false));
        }

        assertEquals(1000, codes.size());
    }

    @Test
    public void skipsCodesAlreadyInUse() {
        CodeGenerator generator = new CodeGenerator();
        Set<String> used = new HashSet<>(Arrays.asList("000000", "000001"));

        String code = generator.generateUniqueCode(used::contains);

        assertEquals("000002", code);
    }

    @Test
    public void generatesDistinctCodesUnderConcurrency() throws Exception {
        CodeGenerator generator = new CodeGenerator();
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            Set<String> codes = new HashSet<>();
            for (Future<String> future : submitGenerateAll(pool, generator, n)) {
                codes.add(future.get());
            }
            assertEquals("requisições simultâneas não podem gerar códigos duplicados", n, codes.size());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void encodeProducesBase62() {
        assertEquals("0", CodeGenerator.encode(0).substring(CodeGenerator.CODE_LENGTH - 1));
        assertEquals("1", CodeGenerator.encode(1).substring(CodeGenerator.CODE_LENGTH - 1));
        assertEquals("a", CodeGenerator.encode(10).substring(CodeGenerator.CODE_LENGTH - 1));
    }

    private java.util.List<Future<String>> submitGenerateAll(ExecutorService pool, CodeGenerator generator, int n) {
        java.util.List<Future<String>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> generator.generateUniqueCode(c -> false)));
        }
        return futures;
    }
}
