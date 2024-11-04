package com.timetable.domain;

import java.util.ArrayList;
import java.util.List;

public class Faculty extends User {
    private List<String> subjects;                // List of subjects the faculty teaches
    private List<TimeSlot> preferredSlots;       // List of preferred time slots for lessons
    private int maxHoursPerDay;                   // Maximum hours a faculty member can teach in a day
    private boolean isAvailable;                   // Availability status of the faculty member
    private List<Lesson> assignedLessons;         // List of lessons assigned to the faculty

    // Constructor
    public Faculty(Long id, String name, String email, String password,
                   List<String> subjects, int maxHoursPerDay) {
        super(id, name, email, password);
        this.subjects = subjects;
        this.maxHoursPerDay = maxHoursPerDay;
        this.isAvailable = true; // Default availability status
        this.assignedLessons = new ArrayList<>(); // Initialize the assigned lessons list
        this.preferredSlots = new ArrayList<>(); // Initialize the preferred slots list
    }

    // Getters and Setters
    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<TimeSlot> getPreferredSlots() {
        return preferredSlots; // Return the actual list
    }

    public void setPreferredSlots(List<TimeSlot> preferredSlots) {
        this.preferredSlots = preferredSlots;
    }

    public int getMaxHoursPerDay() {
        return maxHoursPerDay;
    }

    public void setMaxHoursPerDay(int maxHoursPerDay) {
        this.maxHoursPerDay = maxHoursPerDay;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    // Method to calculate total assigned hours per day
    public int getAssignedHoursPerDay() {
        int totalHours = 0;

        // Sum the hours for each assigned lesson
        for (Lesson lesson : assignedLessons) {
            totalHours += lesson.getCourse().getHoursPerWeek(); // Assuming this returns the hours per week for that course
        }

        return totalHours; // Return the total hours assigned for the day
    }

    // Method to assign a lesson to the faculty
    public void assignLesson(Lesson lesson) {
        assignedLessons.add(lesson);
    }

    // Method to unassign a lesson from the faculty
    public void unassignLesson(Lesson lesson) {
        assignedLessons.remove(lesson);
    }
}
