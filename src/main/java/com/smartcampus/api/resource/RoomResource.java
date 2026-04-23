package com.smartcampus.api.resource;

import com.smartcampus.api.exception.RoomNotEmptyException;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.service.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(DataStore.ROOMS.values());
        return Response.ok(rooms).build();
    }

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        if (room == null || room.getName() == null || room.getName().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorPayload("Validation error", "Room name is required."))
                    .build();
        }

        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(DataStore.nextId());
        }

        // Always initialize sensorIds as an empty list when a room is created.
        room.setSensorIds(new ArrayList<>());

        if (DataStore.ROOMS.containsKey(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(errorPayload("Conflict", "Room with id " + room.getId() + " already exists."))
                    .build();
        }

        DataStore.ROOMS.put(room.getId(), room);

        URI createdUri = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(createdUri).entity(room).build();
    }

    @GET
    @Path("{id}")
    public Response getRoomById(@PathParam("id") String id) {
        Room room = DataStore.ROOMS.get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorPayload("Not Found", "Room with id " + id + " was not found."))
                    .build();
        }

        return Response.ok(room).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteRoom(@PathParam("id") String id) {
        Room room = DataStore.ROOMS.get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorPayload("Not Found", "Room with id " + id + " was not found."))
                    .build();
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + id + " cannot be deleted because it has linked sensors.");
        }

        DataStore.ROOMS.remove(id);
        return Response.noContent().build();
    }

    private Map<String, String> errorPayload(String error, String message) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", error);
        payload.put("message", message);
        return payload;
    }
}
