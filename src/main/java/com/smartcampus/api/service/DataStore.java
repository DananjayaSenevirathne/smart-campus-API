package com.smartcampus.api.service;

import com.smartcampus.api.model.Room;
import com.smartcampus.api.model.Sensor;
import com.smartcampus.api.model.SensorReading;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class DataStore {

    public static final HashMap<String, Room> ROOMS = new HashMap<>();
    public static final HashMap<String, Sensor> SENSORS = new HashMap<>();
    public static final HashMap<String, List<SensorReading>> READINGS = new HashMap<>();

    private DataStore() {
    }

    public static String nextId() {
        return UUID.randomUUID().toString();
    }
}
