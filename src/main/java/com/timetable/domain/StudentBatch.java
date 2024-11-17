// StudentBatch class representing a batch of students with additional room information
package com.timetable.domain;

import java.util.ArrayList;
import java.util.List;

public class StudentBatch {
    private Long id;
    private String batchName;
    private int year;
    private int strength;
    private List<Course> courses;
    private List<Long> lectureRoomIDs;     // field for lecture room IDs
    private List<Long> practicalRoomIDs;   // field for practical (lab) room IDs

    // Constructor
    public StudentBatch(Long id, String batchName, int year, int strength, List<Course> courses, List<Long> lectureRoomIDs, List<Long> practicalRoomIDs) {
        this.id = id;
        this.batchName = batchName;
        this.year = year;
        this.strength = strength;
        this.courses = courses != null ? courses : new ArrayList<>();
        this.lectureRoomIDs = lectureRoomIDs != null ? lectureRoomIDs : new ArrayList<>();
        this.practicalRoomIDs = practicalRoomIDs != null ? practicalRoomIDs : new ArrayList<>();

    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }
    public List<Long> getLectureRoomIDs() { return lectureRoomIDs; }  // New getter
    public void setLectureRoomIDs(List<Long> lectureRoomIDs) { this.lectureRoomIDs = lectureRoomIDs; }  // New setter
    public List<Long> getPracticalRoomIDs() { return practicalRoomIDs; }  // New getter
    public void setPracticalRoomIDs(List<Long> practicalRoomIDs) { this.practicalRoomIDs = practicalRoomIDs; }  // New setter

    public int getRequiredLabsPerWeek() {
        if (courses == null || courses.isEmpty()) {
            return 0; // No courses, so no labs required
        }

        int totalPracticalHours = courses.stream()
                .filter(Course::isLabCourse) // Consider only lab courses
                .mapToInt(Course::getPracticalHours)
                .sum();

        return totalPracticalHours;
    }
}
