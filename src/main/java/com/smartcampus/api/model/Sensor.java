package com.smartcampus.api.model;

public class Sensor {

    public enum Status {
        ACTIVE,
        MAINTENANCE,
        OFFLINE
    }

    private String id;
    private String type;
    private Status status;
    private Double currentValue;
    private String roomId;

    public Sensor() {
        this.status = Status.ACTIVE;
    }

    public Sensor(String id, String type, Status status, Double currentValue, String roomId) {
        this.id = id;
        this.type = type;
        this.status = status == null ? Status.ACTIVE : status;
        this.currentValue = currentValue;
        this.roomId = roomId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
}
