package com.timetable.util;

import com.timetable.domain.*;
import java.util.List;
import java.util.stream.Collectors;

import com.timetable.domain.*;
import java.util.List;
import java.util.logging.Logger;

// just to test if data is being loaded properly or not
public class DataPrinter {

    private static final Logger logger = Logger.getLogger(DataPrinter.class.getName());

    // Print all batches
    public static void printBatches(List<StudentBatch> batches) {
        if (batches == null || batches.isEmpty()) {
            logger.info("No batches found.");
            return;
        }

        logger.info("Printing all batches:");
        for (StudentBatch batch : batches) {
            logger.info("ID: " + batch.getId() + ", Name: " + batch.getBatchName() + ", Year: " + batch.getYear() +
                    ", Strength: " + batch.getStrength() + ", Courses: " + batch.getCourses() +
                    ", Lecture Rooms: " + batch.getLectureRoomIDs() + ", Practical Rooms: " + batch.getPracticalRoomIDs());
        }
    }

    // Print all courses
    public static void printCourses(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            logger.info("No courses found.");
            return;
        }

        logger.info("Printing all courses:");
        for (Course course : courses) {
            logger.info("ID: " + course.getId() + ", Course Code: " + course.getCourseCode() + ", Name: " + course.getName() +
                    ", Type: " + course.getCourseType() + ", Batch IDs: " + course.getBatchIds() +
                    ", Lecture Hours: " + course.getLectureHours() + ", Theory Hours: " + course.getTheoryHours() +
                    ", Practical Hours: " + course.getPracticalHours() + ", Credits: " + course.getCredits() +
                    ", Faculty IDs: " + course.getEligibleFaculty());
        }
    }

    // Print all faculties
    public static void printFaculties(List<Faculty> faculties) {
        if (faculties == null || faculties.isEmpty()) {
            logger.info("No faculties found.");
            return;
        }

        logger.info("Printing all faculties:");
        for (Faculty faculty : faculties) {
            logger.info("ID: " + faculty.getId() + ", Name: " + faculty.getName() + ", Email: " + faculty.getEmail() +
                    ", Subjects: " + faculty.getSubjects() + ", Max Hours Per Day: " + faculty.getMaxHoursPerDay());
        }
    }

    // Print all rooms
    public static void printRooms(List<Room> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            logger.info("No rooms found.");
            return;
        }

        logger.info("Printing all rooms:");
        for (Room room : rooms) {
            logger.info("ID: " + room.getId() + ", Room Number: " + room.getRoomNumber() + ", Capacity: " + room.getCapacity() +
                    ", Type: " + room.getType());
        }
    }
}
