package com.timetable.domain;

import java.time.DayOfWeek;

import java.time.LocalTime;
import java.util.Objects;

public class TimeSlot {
    private Long id;
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;

    public TimeSlot() {}

    public TimeSlot(Long id, String day, LocalTime startTime, LocalTime endTime) {
        this.id = id;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    // Overriding equals and hashCode to compare TimeSlots by id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(id, timeSlot.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }


    public int getTimeSlotIndex() {
        switch (day.toLowerCase()) {
            case "monday":
                return 1;
            case "tuesday":
                return 2;
            case "wednesday":
                return 3;
            case "thursday":
                return 4;
            case "friday":
                return 5;
            default:
                throw new IllegalArgumentException("Invalid day: " + day);
        }
    }
}
