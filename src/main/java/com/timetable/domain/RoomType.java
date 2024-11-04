package com.timetable.domain;

public enum RoomType {
    LECTURE_ROOM,
    COMPUTER_LAB,
    HARDWARE_LAB,
    SEATER_120,
    SEATER_240;

    public boolean isLabRoom() {
        return false;
    }
}
