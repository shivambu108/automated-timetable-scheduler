package com.timetable.domain;

import java.util.Objects;

public class Room {
    private Long id;
    private String roomNumber;
    private int capacity;
    private RoomType roomType; // Room type now includes expanded types
    private boolean isAvailable;

    public Room() {}

    public Room(Long id, String roomNumber, int capacity, RoomType roomType) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.roomType = roomType;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public RoomType getType() { return roomType; }
    public void setType(RoomType roomType) { this.roomType = roomType; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public boolean isLectureRoom() {
        return roomType == RoomType.LECTURE_ROOM;
    }

    public boolean isLabRoom() {
        return roomType == RoomType.COMPUTER_LAB || roomType == RoomType.HARDWARE_LAB;
    }

    public Integer getIdealDailyUsage() {
        if(isLectureRoom()) return 5;
        else return 2;
    }
}