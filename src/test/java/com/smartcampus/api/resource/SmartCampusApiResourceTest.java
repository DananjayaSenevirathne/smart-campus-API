package com.smartcampus.api.resource;

import com.smartcampus.api.config.ApplicationConfig;
import com.smartcampus.api.service.DataStore;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartCampusApiResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ApplicationConfig();
    }

    @BeforeEach
    void resetDataStore() {
        DataStore.ROOMS.clear();
        DataStore.SENSORS.clear();
        DataStore.READINGS.clear();
    }

    @Test
    void discoveryEndpointReturnsApiMetadata() {
        Response response = apiTarget().request().get();
        assertEquals(200, response.getStatus());

        Map<?, ?> payload = response.readEntity(Map.class);
        assertEquals("1.0", payload.get("version"));
        assertEquals("smart-campus-support@university.edu", payload.get("contact"));

        Map<?, ?> links = (Map<?, ?>) payload.get("links");
        assertTrue(String.valueOf(links.get("rooms")).endsWith("rooms"));
        assertTrue(String.valueOf(links.get("sensors")).endsWith("sensors"));
    }

    @Test
    void roomLifecycleCreateGetAndDelete() {
        String roomId = createRoom("Innovation Lab", 45);

        Response getResponse = apiTarget("rooms", roomId).request().get();
        assertEquals(200, getResponse.getStatus());

        Map<?, ?> room = getResponse.readEntity(Map.class);
        assertEquals(roomId, room.get("id"));
        assertEquals("Innovation Lab", room.get("name"));

        Response deleteResponse = apiTarget("rooms", roomId).request().delete();
        assertEquals(204, deleteResponse.getStatus());
    }

    @Test
    void deleteRoomWithSensorsReturns409() {
        String roomId = createRoom("AI Room", 30);
        createSensor(roomId, "CO2", "ACTIVE");

        Response deleteResponse = apiTarget("rooms", roomId).request().delete();
        assertEquals(409, deleteResponse.getStatus());

        Map<?, ?> payload = deleteResponse.readEntity(Map.class);
        assertEquals(409, ((Number) payload.get("status")).intValue());
        assertTrue(((String) payload.get("message")).contains("cannot be deleted"));
    }

    @Test
    void creatingSensorWithMissingRoomReturns422() {
        Map<String, Object> sensor = new HashMap<>();
        sensor.put("type", "CO2");
        sensor.put("roomId", "missing-room");

        Response response = apiTarget("sensors").request().post(Entity.json(sensor));
        assertEquals(422, response.getStatus());

        Map<?, ?> payload = response.readEntity(Map.class);
        assertEquals(422, ((Number) payload.get("status")).intValue());
        assertTrue(((String) payload.get("message")).contains("does not exist"));
    }

    @Test
    void sensorsCanBeFilteredByType() {
        String roomId = createRoom("Main Hall", 100);
        createSensor(roomId, "CO2", "ACTIVE");
        createSensor(roomId, "TEMPERATURE", "ACTIVE");

        Response response = apiTarget("sensors").queryParam("type", "CO2").request().get();
        assertEquals(200, response.getStatus());

        List<?> sensors = response.readEntity(List.class);
        assertEquals(1, sensors.size());
        Map<?, ?> firstSensor = (Map<?, ?>) sensors.get(0);
        assertEquals("CO2", firstSensor.get("type"));
    }

    @Test
    void addingReadingUpdatesCurrentSensorValue() {
        String roomId = createRoom("Chemistry Lab", 40);
        String sensorId = createSensor(roomId, "CO2", "ACTIVE");

        Map<String, Object> reading = new HashMap<>();
        reading.put("value", 512.4);

        Response createReadingResponse = apiTarget("sensors", sensorId, "readings")
                .request()
                .post(Entity.json(reading));

        assertEquals(201, createReadingResponse.getStatus());
        assertEquals(512.4, DataStore.SENSORS.get(sensorId).getCurrentValue());

        Response listReadingsResponse = apiTarget("sensors", sensorId, "readings").request().get();
        assertEquals(200, listReadingsResponse.getStatus());

        List<?> readings = listReadingsResponse.readEntity(List.class);
        assertEquals(1, readings.size());
        Map<?, ?> firstReading = (Map<?, ?>) readings.get(0);
        assertEquals(512.4, ((Number) firstReading.get("value")).doubleValue());
    }

    @Test
    void addingReadingToUnavailableSensorReturns403() {
        String roomId = createRoom("Server Room", 8);
        String sensorId = createSensor(roomId, "TEMPERATURE", "MAINTENANCE");

        Map<String, Object> reading = new HashMap<>();
        reading.put("value", 22.2);

        Response response = apiTarget("sensors", sensorId, "readings")
                .request()
                .post(Entity.json(reading));

        assertEquals(403, response.getStatus());

        Map<?, ?> payload = response.readEntity(Map.class);
        assertEquals(403, ((Number) payload.get("status")).intValue());
        assertTrue(((String) payload.get("message")).contains("not available"));
    }

    private String createRoom(String name, int capacity) {
        Map<String, Object> room = new HashMap<>();
        room.put("name", name);
        room.put("capacity", capacity);

        Response response = apiTarget("rooms").request().post(Entity.json(room));
        assertEquals(201, response.getStatus());

        Map<?, ?> payload = response.readEntity(Map.class);
        String roomId = (String) payload.get("id");
        assertNotNull(roomId);
        return roomId;
    }

    private String createSensor(String roomId, String type, String status) {
        Map<String, Object> sensor = new HashMap<>();
        sensor.put("roomId", roomId);
        sensor.put("type", type);
        sensor.put("status", status);

        Response response = apiTarget("sensors").request().post(Entity.json(sensor));
        assertEquals(201, response.getStatus());

        Map<?, ?> payload = response.readEntity(Map.class);
        String sensorId = (String) payload.get("id");
        assertNotNull(sensorId);
        return sensorId;
    }

    private WebTarget apiTarget(String... paths) {
        WebTarget target = target();
        for (String path : paths) {
            target = target.path(path);
        }
        return target;
    }
}
