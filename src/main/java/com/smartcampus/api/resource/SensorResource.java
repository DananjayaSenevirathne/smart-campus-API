package com.smartcampus.api.resource;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;
import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.service.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @PathParam("id")
    private String sensorId;

    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorPayload("Validation error", "Sensor payload is required."))
                    .build();
        }

        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorPayload("Validation error", "roomId is required to create a sensor."))
                    .build();
        }

        if (sensor.getType() == null || sensor.getType().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorPayload("Validation error", "type is required to create a sensor."))
                    .build();
        }

        Room room = DataStore.ROOMS.get(sensor.getRoomId());
        if (room == null) {
            throw new LinkedResourceNotFoundException("Room not found");
        }

        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(DataStore.nextId());
        }

        if (DataStore.SENSORS.containsKey(sensor.getId())) {
            return Response.status(409)
                    .entity(errorPayload("Conflict", "Sensor with id " + sensor.getId() + " already exists."))
                    .build();
        }

        if (sensor.getStatus() == null) {
            sensor.setStatus(Sensor.Status.ACTIVE);
        }

        DataStore.SENSORS.put(sensor.getId(), sensor);
        if (room.getSensorIds() == null) {
            room.setSensorIds(new ArrayList<>());
        }
        if (!room.getSensorIds().contains(sensor.getId())) {
            room.getSensorIds().add(sensor.getId());
        }

        return Response.status(201).entity(sensor).build();
    }

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>();
        for (Sensor sensor : DataStore.SENSORS.values()) {
            if (type == null || type.isBlank()) {
                result.add(sensor);
            } else if (sensor.getType() != null && sensor.getType().equalsIgnoreCase(type)) {
                result.add(sensor);
            }
        }

        return Response.ok(result).build();
    }

    @Path("{id}/readings")
    public SensorReadingResource getReadingResource() {
        return new SensorReadingResource(sensorId);
    }

    private Map<String, String> errorPayload(String error, String message) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", error);
        payload.put("message", message);
        return payload;
    }
}
