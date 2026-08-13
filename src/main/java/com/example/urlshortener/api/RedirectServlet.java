package com.example.urlshortener.api;

import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.service.UrlNotFoundException;
import com.example.urlshortener.service.UrlShortenerService;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Atende a rota `/r/{code}` com HTTP 302 para a URL original.
 *
 * Implementado como servlet (e não JAX-RS) porque a aplicação JAX-RS está
 * registrada em `@ApplicationPath("/api")`; manter o redirecionamento fora
 * do `/api` dá short URLs limpas sem conflitar com os assets estáticos do
 * frontend servidos na raiz do WAR.
 */
@WebServlet(name = "RedirectServlet", urlPatterns = "/r/*")
public class RedirectServlet extends HttpServlet {

    @Inject
    private UrlShortenerService service;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String code = pathCode(request.getPathInfo());
        try {
            UrlEntity entity = service.resolve(code);
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader("Location", entity.getOriginalUrl());
        } catch (UrlNotFoundException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static String pathCode(String pathInfo) {
        return pathInfo == null || pathInfo.equals("/") ? "" : pathInfo.substring(1);
    }
}