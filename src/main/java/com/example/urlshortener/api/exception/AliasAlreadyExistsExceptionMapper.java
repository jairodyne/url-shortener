package com.example.urlshortener.api.exception;

import com.example.urlshortener.model.dto.ErrorResponse;
import com.example.urlshortener.service.AliasAlreadyExistsException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AliasAlreadyExistsExceptionMapper implements ExceptionMapper<AliasAlreadyExistsException> {

    @Override
    public Response toResponse(AliasAlreadyExistsException e) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}