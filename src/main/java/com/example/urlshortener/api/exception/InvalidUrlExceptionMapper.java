package com.example.urlshortener.api.exception;

import com.example.urlshortener.model.dto.ErrorResponse;
import com.example.urlshortener.service.InvalidUrlException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InvalidUrlExceptionMapper implements ExceptionMapper<InvalidUrlException> {

    @Override
    public Response toResponse(InvalidUrlException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(Response.Status.BAD_REQUEST.getStatusCode(), e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}