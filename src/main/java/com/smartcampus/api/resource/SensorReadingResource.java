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
                .entity(Map.of("message", "Sensor not found")).build();
        }

        List<SensorReading> readings = DataStore.READINGS.getOrDefault(sensorId, new java.util.ArrayList<>());
        return Response.ok(readings).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading, @Context UriInfo uriInfo) {
        Sensor sensor = DataStore.SENSORS.get(sensorId);
        // 1. Check sensor exists (404 if not)
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("message", "Sensor not found")).build();
        }

        // 2. Check sensor status is NOT "MAINTENANCE" (throw SensorUnavailableException → 403)
        if (sensor.getStatus() == Sensor.Status.MAINTENANCE) {
            throw new SensorUnavailableException(
                "Sensor " + sensorId + " is in maintenance and not available for readings."
            );
        }

        // 3. Set reading timestamp to System.currentTimeMillis()
        reading.setTimestamp(String.valueOf(System.currentTimeMillis()));
        // 4. Generate UUID for reading ID
        reading.setId(UUID.randomUUID().toString());

        // Ensure thread-safe list and add reading
        DataStore.READINGS.computeIfAbsent(sensorId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(reading);

        // 5. Update parent sensor's currentValue
        sensor.setCurrentValue(reading.getValue());

        // 6. Return 201
        URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(location).entity(reading).build();
    }
}
