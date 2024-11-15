package com.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.List;
import java.util.Objects;
import com.timetable.util.CSVDataLoader;


@PlanningEntity
public class Lesson {
    @PlanningId
    private Long id;
    private Course course;
    private StudentBatch studentBatch;

    @PlanningVariable(valueRangeProviderRefs = "facultyRange")
    private Faculty faculty;

//    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Room room;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    private List<Room> allRooms;
    // Removed 'hasLab' as it's not needed anymore; we use Course's isLabCourse method
    // private boolean hasLab; // Lab status based on course

    // Constructors
    public Lesson() {}

    public Lesson(Long id, Course course, StudentBatch studentBatch, List<Room> allRooms) {
        this.id = id;
        this.course = course;
        this.studentBatch = studentBatch;
        this.allRooms = allRooms;
        assignPredefinedRoom();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) {
        this.course = course;
        assignPredefinedRoom();
    }

    public StudentBatch getStudentBatch() { return studentBatch; }
    public void setStudentBatch(StudentBatch studentBatch) {
        this.studentBatch = studentBatch;
        assignPredefinedRoom();
    }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
        assignPredefinedRoom();
    }

    // Removed 'hasLab' getter since it's no longer a member variable
    // public boolean hasLab() { return hasLab; } // Lab status getter

    // Method to assign predefined room based on batch and course type
    // Room Assignment Logic
    public void assignPredefinedRoom() {
        if (course == null || studentBatch == null || allRooms == null) return;

        if (course.isLabCourse() && timeSlot != null) {
            room = isLabTimeSlot(timeSlot)
                    ? findRoomById(studentBatch.getPracticalRoomIDs().get(0))
                    : findRoomById(studentBatch.getLectureRoomIDs().get(0));
        } else {
            room = findRoomById(studentBatch.getLectureRoomIDs().get(0));
        }
    }

    private boolean isLabTimeSlot(TimeSlot timeSlot) {
        // Example logic: check if timeSlot index matches predefined criteria
        return timeSlot.getTimeSlotIndex() == (int) (id % 5) + 1;
    }


    private Room findRoomById(Long roomId) {
        if (allRooms == null || roomId == null) return null;
        return allRooms.stream()
                .filter(room -> room.getId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    public boolean isAssigned() {
        return faculty != null && room != null && timeSlot != null;
    }

    public boolean isValidFaculty(Faculty faculty) {
        return course != null && course.getEligibleFaculty().contains(faculty);
    }

    public boolean isValidRoom() {
        if (room == null || course == null) return false;
        return course.isLabCourse() == room.isLabRoom();
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
