package com.example.urlshortener.service;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Motor de geração de códigos curtos (base62).
 *
 * O método {@link #generateUniqueCode(Predicate)} é {@code synchronized}: apenas uma
 * requisição gera código por vez, garantindo ausência de corrida entre "gerar" e
 * "verificar disponibilidade". Colisões (código já em uso) são resolvidas com
 * retry dentro do mesmo lock.
 */
@ApplicationScoped
public class CodeGenerator {

    static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 5;
    private static final char[] ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final AtomicLong sequence = new AtomicLong(0);

    public synchronized String generateUniqueCode(Predicate<String> codeInUse) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = encode(sequence.getAndIncrement());
            if (!codeInUse.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Não foi possível gerar um código único após " + MAX_ATTEMPTS + " tentativas");
    }

    static String encode(long value) {
        StringBuilder sb = new StringBuilder();
        long v = value;
        do {
            sb.append(ALPHABET[(int) (v % ALPHABET.length)]);
            v /= ALPHABET.length;
        } while (v > 0);
        while (sb.length() < CODE_LENGTH) {
            sb.append('0');
        }
        return sb.reverse().toString();
    }
}
