package com.smartcampus.api.resource;

import com.smartcampus.api.exception.SensorUnavailableException;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;
import com.smartcampus.api.service.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import java.util.UUID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = DataStore.SENSORS.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorPayload("Not Found", "Sensor with id " + sensorId + " was not found."))
                    .build();
        }

        List<SensorReading> readings = DataStore.READINGS.getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    @POST
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        Sensor sensor = DataStore.SENSORS.get(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(errorPayload("Not Found", "Sensor with id " + sensorId + " was not found."))
                    .build();
        }

        if (sensor.getStatus() == Sensor.Status.MAINTENANCE) {
            throw new SensorUnavailableException(
                    "Sensor " + sensorId + " is not available for new readings."
            );
        }

        if (reading == null || reading.getValue() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errorPayload("Validation error", "Reading value is required."))
                    .build();
        }

        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(String.valueOf(System.currentTimeMillis()));

        DataStore.READINGS.computeIfAbsent(sensorId, key -> new ArrayList<>()).add(reading);
        sensor.setCurrentValue(reading.getValue());

        URI createdUri = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(createdUri).entity(reading).build();
    }

    private Map<String, String> errorPayload(String error, String message) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("error", error);
        payload.put("message", message);
        return payload;
    }
}
