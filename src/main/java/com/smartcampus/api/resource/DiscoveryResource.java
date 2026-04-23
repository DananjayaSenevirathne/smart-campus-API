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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiInfo(@Context UriInfo uriInfo) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("version", "1.0");
        info.put("name", "Smart Campus API");
        info.put("contact", "smart-campus-support@university.edu");
        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms", uriInfo.getBaseUri() + "rooms");
        links.put("sensors", uriInfo.getBaseUri() + "sensors");
        info.put("links", links);
        return Response.ok(info).build();
    }
}
