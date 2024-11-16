package com.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import com.timetable.util.CSVDataLoader;


@PlanningEntity
public class Lesson {

    private static final Logger logger = Logger.getLogger(Lesson.class.getName());

    @PlanningId
    private Long id;
    private Course course;
    private StudentBatch studentBatch;
    private String lessonType; // Added to track lesson type

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

    public void setLessonType(String lessonType) {
        this.lessonType = lessonType;
    }

    public String getLessonType() {
        return lessonType;
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
    // Modified room assignment logic to handle lecture and practical rooms separately
    public void assignPredefinedRoom() {
        if (course == null || studentBatch == null || allRooms == null) {
            logger.warning(String.format("Cannot assign room for lesson %d - missing required data", id));
            return;
        }

        if ("LAB".equals(lessonType)) {
            // For lab lessons, assign from practical rooms
            if (!studentBatch.getPracticalRoomIDs().isEmpty()) {
                room = findRoomById(studentBatch.getPracticalRoomIDs().get(0));
                if (room != null) {
                    logger.info(String.format("Assigned practical room %s to LAB lesson %d for course %s",
                            room.getRoomNumber(),
                            id,
                            course.getCourseCode()));
                } else {
                    logger.warning(String.format("Failed to assign practical room to LAB lesson %d for course %s",
                            id,
                            course.getCourseCode()));
                }
            }
        } else {
            // For lecture lessons, assign from lecture rooms
            if (!studentBatch.getLectureRoomIDs().isEmpty()) {
                room = findRoomById(studentBatch.getLectureRoomIDs().get(0));
                if (room != null) {
                    logger.info(String.format("Assigned lecture room %s to LECTURE lesson %d for course %s",
                            room.getRoomNumber(),
                            id,
                            course.getCourseCode()));
                } else {
                    logger.warning(String.format("Failed to assign lecture room to LECTURE lesson %d for course %s",
                            id,
                            course.getCourseCode()));
                }
            }
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

    // Modified room validation to check room type against lesson type
    public boolean isValidRoom() {
        if (room == null) return false;

        if ("LAB".equals(lessonType)) {
            return room.isLabRoom() && studentBatch.getPracticalRoomIDs().contains(room.getId());
        } else {
            return room.isLectureRoom() && studentBatch.getLectureRoomIDs().contains(room.getId());
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
