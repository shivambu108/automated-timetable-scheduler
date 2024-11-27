package com.timetable.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a faculty member with assigned lessons, preferences, and teaching constraints.
 */
public class Faculty extends User {
    private List<String> subjects;                // Subjects taught by the faculty
    private List<TimeSlot> preferredSlots;        // Preferred time slots for teaching
    private int maxHoursPerDay;                   // Maximum teaching hours per day
    private boolean isAvailable;                  // Availability status of the faculty
    private List<Lesson> assignedLessons;         // Lessons assigned to the faculty

    // Constructor to initialize faculty details
    public Faculty(Long id, String name, String email, String password,
                   List<String> subjects, int maxHoursPerDay) {
        super(id, name, email, password);
        this.subjects = subjects;
        this.maxHoursPerDay = maxHoursPerDay;
        this.isAvailable = true; // Default to available
        this.assignedLessons = new ArrayList<>(); // Initialize assigned lessons
        this.preferredSlots = new ArrayList<>(); // Initialize preferred slots
    }

    // Getters and Setters
    public List<String> getSubjects() {
        return subjects;
    }

    public List<TimeSlot> getPreferredSlots() {
        return preferredSlots;
    }

    public int getMaxHoursPerDay() {
        return maxHoursPerDay;
    }

}
