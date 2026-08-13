package com.example.urlshortener.api;

import com.example.urlshortener.model.UrlEntity;
import com.example.urlshortener.model.dto.CreateUrlRequest;
import com.example.urlshortener.model.dto.CreateUrlResponse;
import com.example.urlshortener.service.UrlShortenerService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/urls")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UrlShortenerResource {

    @Inject
    private UrlShortenerService service;

    @Context
    private HttpServletRequest request;

    @POST
    public Response create(CreateUrlRequest body) {
        UrlEntity entity = service.createUrl(body.getUrl(), body.getAlias());
        String baseUrl = baseUrl(request);
        CreateUrlResponse response = new CreateUrlResponse(
                service.buildShortUrl(baseUrl, entity.getCode()),
                entity.getCode(),
                entity.getOriginalUrl());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    static String baseUrl(HttpServletRequest req) {
        int port = req.getServerPort();
        String hostAndPort = (port == 80 || port == 443)
                ? req.getServerName()
                : req.getServerName() + ":" + port;
        return req.getScheme() + "://" + hostAndPort + req.getContextPath();
    }
}