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
        if (room.getId() == null || room.getId().isBlank()) {
            room.setId(DataStore.nextId());
        }
        room.setSensorIds(new ArrayList<>());
        DataStore.ROOMS.put(room.getId(), room);
        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build(); // 201 + Location header
    }

    @GET
    @Path("{id}")
    public Response getRoomById(@PathParam("id") String id) {
        Room room = DataStore.ROOMS.get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Room not found")).build();
        }

        return Response.ok(room).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteRoom(@PathParam("id") String id) {
        Room room = DataStore.ROOMS.get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Room not found")).build();
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room " + id + " still has sensors assigned and cannot be deleted.");
        }

        DataStore.ROOMS.remove(id);
        return Response.noContent().build(); // 204
    }
}
