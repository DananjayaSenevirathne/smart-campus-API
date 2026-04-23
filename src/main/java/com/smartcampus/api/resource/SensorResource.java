package com.smartcampus.api.resource;

import com.smartcampus.api.exception.LinkedResourceNotFoundException;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.service.DataStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Path("sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        // Validate that the roomId exists
        if (sensor.getRoomId() == null || !DataStore.ROOMS.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Room with id '" + sensor.getRoomId() + "' does not exist."
            );
        }
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            sensor.setId(DataStore.nextId());
        }
        if (sensor.getStatus() == null) sensor.setStatus(Sensor.Status.ACTIVE);
        DataStore.SENSORS.put(sensor.getId(), sensor);
        // Add sensor ID to the room's sensorIds list
        DataStore.ROOMS.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
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

    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId); // NO HTTP method annotation here!
    }
}
