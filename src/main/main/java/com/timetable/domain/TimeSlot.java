package com.timetable.domain;

import java.time.DayOfWeek;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Objects;

public class TimeSlot {
    private Long id;
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String slotType;

    public TimeSlot() {}

    public TimeSlot(Long id, String day, LocalTime startTime, LocalTime endTime, String slotType) {
        this.id = id;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotType = slotType;
    }

    // Legacy constructor for backward compatibility
    public TimeSlot(Long id, String day, LocalTime startTime, LocalTime endTime) {
        this(id, day, startTime, endTime, "LECTURE"); // Default to lecture type
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

    // New getter and setter for slot type
    public String getSlotType() { return slotType; }
    public void setSlotType(String slotType) { this.slotType = slotType; }

    // Overriding equals and hashCode to compare TimeSlots by id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(id, timeSlot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

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

    @Override
    public String toString() {
        return String.format("%s %s-%s (%s)",
                day,
                startTime.toString(),
                endTime.toString(),
                slotType);
    }

    public int getDurationInMinutes() {
        LocalTime startTime = this.startTime;
        LocalTime endTime = this.endTime;
        Duration duration = Duration.between(startTime, endTime);

        return (int)duration.toMinutes() % 60;
    }
}