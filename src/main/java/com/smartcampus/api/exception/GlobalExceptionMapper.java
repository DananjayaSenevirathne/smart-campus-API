package com.smartcampus.api.exception;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof NotFoundException) {
            Map<String, String> error = new LinkedHashMap<>();
            error.put("error", "Not Found");

            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(error)
                    .build();
        }

        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof LinkedResourceNotFoundException) {
                Map<String, String> error = new LinkedHashMap<>();
                error.put("error", "Referenced room does not exist");
                error.put("message", cause.getMessage());

                return Response.status(422)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(error)
                        .build();
            }
            cause = cause.getCause();
        }

        LOGGER.log(Level.SEVERE, "Unhandled exception", exception);

        Map<String, String> error = new LinkedHashMap<>();
        error.put("error", "Something went wrong");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}
