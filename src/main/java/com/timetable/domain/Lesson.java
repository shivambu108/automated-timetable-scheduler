package com.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import java.util.Objects;

@PlanningEntity
public class Lesson {
    @PlanningId
    private Long id;
    private Course course;
    private StudentBatch studentBatch;

    @PlanningVariable(valueRangeProviderRefs = "facultyRange")
    private Faculty faculty;

    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Room room;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    // Removed 'hasLab' as it's not needed anymore; we use Course's isLabCourse method
    // private boolean hasLab; // Lab status based on course

    // Constructors
    public Lesson() {}

    public Lesson(Long id, Course course, StudentBatch studentBatch) {
        this.id = id;
        this.course = course;
        this.studentBatch = studentBatch;
        // Removed initialization of 'hasLab' since it's no longer a member variable
        // this.hasLab = course.hasLab();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) {
        this.course = course;
        // Removed the update of 'hasLab' since it's no longer a member variable
        // this.hasLab = course.hasLab();
    }

    public StudentBatch getStudentBatch() { return studentBatch; }
    public void setStudentBatch(StudentBatch studentBatch) { this.studentBatch = studentBatch; }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }

    // Removed 'hasLab' getter since it's no longer a member variable
    // public boolean hasLab() { return hasLab; } // Lab status getter

    // Custom methods for constraint checking
    public boolean isAssigned() {
        return faculty != null && room != null && timeSlot != null;
    }

    public boolean isValidFaculty(Faculty faculty) {
        return course.getEligibleFaculty().contains(faculty);
    }

    // New method to check if the course is a lab course
    public boolean isLabCourse() {
        return course.isLabCourse(); // Check if the course is a lab course using the new method
    }

    public boolean isValidRoom() {
        if (isLabCourse()) {
            return room.isLabRoom(); // Lab courses must be in practical rooms
        } else {
            return !room.isLabRoom(); // Lecture courses must be in regular rooms
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lesson lesson = (Lesson) o;
        return Objects.equals(id, lesson.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Lesson{" +
                "id=" + id +
                ", course=" + (course != null ? course.getCourseCode() : "null") +
                ", studentBatch=" + (studentBatch != null ? studentBatch.getBatchName() : "null") +
                ", faculty=" + (faculty != null ? faculty.getName() : "null") +
                ", room=" + (room != null ? room.getRoomNumber() : "null") +
                ", timeSlot=" + (timeSlot != null ? timeSlot.getDay() + " " + timeSlot.getStartTime() : "null") +
                '}';
    }
}
