package com.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Represents a lesson, including its course, batch, faculty, room, and time slot.
 * This is a @PlanningEntity used by OptaPlanner to optimize timetable scheduling.
 */
@PlanningEntity
public class Lesson {

    private static final Logger logger = Logger.getLogger(Lesson.class.getName());

    @PlanningId
    private Long id;
    private Course course;
    private StudentBatch studentBatch;
    private String lessonType; // Tracks whether the lesson is a LAB or LECTURE

//    @PlanningVariable(valueRangeProviderRefs = "facultyRange")
    private Faculty faculty;

    private Room room; // Room for the lesson, currently not managed as a PlanningVariable

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    private TimeSlot minorTimeSlot;


    private List<Room> roomList; // List of potential rooms for the lesson

    // Constructors
    public Lesson() {}

    public Lesson(Long id, Course course, StudentBatch studentBatch, List<Room> roomList) {
        this.id = id;
        this.course = course;
        this.studentBatch = studentBatch;
        this.roomList = roomList;
        this.room = null; // Room will be assigned during planning
    }

    public Lesson(Long id, Course course, List<Room> roomList) {
        this.id = id;
        this.course = course;
        this.roomList = roomList;
        this.room = null; // Room will be assigned during planning
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public StudentBatch getStudentBatch() { return studentBatch; }
    public void setStudentBatch(StudentBatch studentBatch) { this.studentBatch = studentBatch; }

    public String getLessonType() { return lessonType; }
    public void setLessonType(String lessonType) { this.lessonType = lessonType; }

    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) {
        if (this.room == null) {
            this.room = room;
        }
    }

    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) { this.timeSlot = timeSlot; }

    public TimeSlot getMinorTimeSlot() { return minorTimeSlot; }
    public void setMinorTimeSlot(TimeSlot timeSlot) { this.minorTimeSlot = minorTimeSlot; }

    public List<Room> getRoomList() { return roomList; }

    // Helper method to check if a time slot is suitable for a lab
    private boolean isLabTimeSlot(TimeSlot timeSlot) {
        return timeSlot.getTimeSlotIndex() == (int) (id % 5) + 1; // Example logic
    }

    // Finds a room by its ID within the available room list
    private Room findRoomById(Long roomId) {
        if (roomList == null || roomId == null) return null;
        return roomList.stream()
                .filter(room -> room.getId().equals(roomId))
                .findFirst()
                .orElse(null);
    }

    // Checks if the lesson is fully assigned (faculty, room, and time slot are all set)
    public boolean isAssigned() {
        return faculty != null && room != null && timeSlot != null;
    }

    // Validates if a given faculty member can teach this course
    public boolean isValidFaculty(Faculty faculty) {
        return course != null && course.getEligibleFaculty().contains(faculty);
    }

    // Validates if the room is appropriate for the lesson type
    public boolean isValidRoom() {
        if (room == null) return false;

        if ("LAB".equals(lessonType)) {
            return room.isLabRoom() && studentBatch.getPracticalRoomIDs().contains(room.getId());
        } else {
            return room.isLectureRoom() && studentBatch.getLectureRoomIDs().contains(room.getId());
        }
    }

    public boolean hasValidBatchAndCourse() {
        return this.getStudentBatch() != null && this.getCourse() != null;
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
