package com.smartcampus.api.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Context;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response getApiDiscovery(@Context UriInfo uriInfo) {
        String baseUri = uriInfo.getBaseUri().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "1.0");
        payload.put("contact", "smart-campus-support@university.edu");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms", baseUri + "rooms");
        links.put("sensors", baseUri + "sensors");

        payload.put("links", links);

        return Response.ok(payload).build();
    }
}
