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

    private List<Room> roomList;
    // Removed 'hasLab' as it's not needed anymore; we use Course's isLabCourse method
    // private boolean hasLab; // Lab status based on course

    // Constructors
    public Lesson() {}

    public Lesson(Long id, Course course, StudentBatch studentBatch, List<Room> roomList) {
        this.id = id;
        this.course = course;
        this.studentBatch = studentBatch;
        this.roomList = roomList;
        this.room = null;
//        assignPredefinedRoom();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) {
        this.course = course;
//        assignPredefinedRoom();
    }

    public StudentBatch getStudentBatch() { return studentBatch; }
    public void setStudentBatch(StudentBatch studentBatch) {
        this.studentBatch = studentBatch;
//        assignPredefinedRoom();
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
    public void setRoom(Room room) {
        if (this.room == null) {
            this.room = room;
    }}

    public TimeSlot getTimeSlot() { return timeSlot; }
    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
//        assignPredefinedRoom();
    }

    // Removed 'hasLab' getter since it's no longer a member variable
    // public boolean hasLab() { return hasLab; } // Lab status getter

    // Method to assign predefined room based on batch and course type
    // Room Assignment Logic
    // Modified room assignment logic to handle lecture and practical rooms separately
//    public void assignPredefinedRoom() {
//        // Ensure batch and room list are available
//        if (studentBatch == null || roomList == null) {
//            logger.warning(String.format("Cannot assign room for lesson %d - missing batch or room list", id));
//            return;
//        }
//
//        // Fetch the batch-specific room IDs for lecture or practical
//        Long roomIdToAssign = null;
//
//        if ("LAB".equalsIgnoreCase(lessonType)) {
//            // For lab lessons, assign a practical room
//            List<Long> practicalRoomIDs = studentBatch.getPracticalRoomIDs();
//            if (practicalRoomIDs != null && !practicalRoomIDs.isEmpty()) {
//                roomIdToAssign = practicalRoomIDs.get(0); // Get the first available practical room ID
//                logger.info(String.format("Assigning practical room %d to LAB lesson %d for batch %s",
//                        roomIdToAssign, id, studentBatch.getBatchName()));
//            } else {
//                logger.warning(String.format("No practical rooms available for LAB lesson %d for batch %s", id, studentBatch.getBatchName()));
//            }
//        } else if ("LECTURE".equalsIgnoreCase(lessonType)) {
//            // For lecture lessons, assign a lecture room
//            List<Long> lectureRoomIDs = studentBatch.getLectureRoomIDs();
//            if (lectureRoomIDs != null && !lectureRoomIDs.isEmpty()) {
//                roomIdToAssign = lectureRoomIDs.get(0); // Get the first available lecture room ID
//                logger.info(String.format("Assigning lecture room %d to LECTURE lesson %d for batch %s",
//                        roomIdToAssign, id, studentBatch.getBatchName()));
//            } else {
//                logger.warning(String.format("No lecture rooms available for LECTURE lesson %d for batch %s", id, studentBatch.getBatchName()));
//            }
//        } else {
//            logger.warning(String.format("Invalid lesson type '%s' for lesson %d", lessonType, id));
//            return;
//        }
//
//        // After determining the room ID, assign the room from the roomList
//        if (roomIdToAssign != null) {
//            room = findRoomById(roomIdToAssign);
//            if (room != null) {
//                // Log and assign the room
//                logger.info(String.format("Room %s assigned to lesson %d for batch %s",
//                        room.getRoomNumber(), id, studentBatch.getBatchName()));
//            } else {
//                logger.warning(String.format("Failed to assign room with ID %d to lesson %d for batch %s",
//                        roomIdToAssign, id, studentBatch.getBatchName()));
//            }
//        }
//    }


//    // Helper method to assign the first available room from a list of room IDs
//    private void assignRoomById(List<Long> roomIDs, String lessonType) {
//        // Check if the list of room IDs is empty
//        if (roomIDs == null || roomIDs.isEmpty()) {
//            logger.warning(String.format("No predefined room IDs available for %s lesson %d", lessonType, id));
//            return;
//        }
//
//        // Find and assign the first valid room from the list
//        for (Long roomId : roomIDs) {
//            Room candidateRoom = findRoomById(roomId);
//            if (candidateRoom != null) {
//                this.room = candidateRoom;
//                logger.info(String.format("Assigned room %s to %s lesson %d for batch %s",
//                        room.getRoomNumber(),
//                        lessonType,
//                        id,
//                        studentBatch.getBatchName()));
//                return;
//            }
//        }
//
//        // Log a warning if no matching room is found
//        logger.warning(String.format("No matching room found for %s lesson %d in the provided room IDs", lessonType, id));
//    }


    private boolean isLabTimeSlot(TimeSlot timeSlot) {
        // Example logic: check if timeSlot index matches predefined criteria
        return timeSlot.getTimeSlotIndex() == (int) (id % 5) + 1;
    }


    private Room findRoomById(Long roomId) {
        if (roomList == null || roomId == null) return null;
        return roomList.stream()
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

    public List<Room> getRoomList() {
        return roomList;
    }
}
