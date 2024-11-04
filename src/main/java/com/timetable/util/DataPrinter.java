package com.timetable.util;

import com.timetable.domain.*;
import java.util.List;
import java.util.stream.Collectors;

public class DataPrinter {

    public static void printFacultyData(List<Faculty> facultyList) {
        System.out.printf("%-5s %-20s %-30s %-20s %-30s %-15s%n",
                "ID", "Name", "Email", "Password", "Subjects", "Max Hours Per Day");
        System.out.println("---------------------------------------------------------------------------------------");

        for (Faculty faculty : facultyList) {
            System.out.printf("%-5d %-20s %-30s %-20s %-30s %-15d%n",
                    faculty.getId(),
                    faculty.getName(),
                    faculty.getEmail(),
                    faculty.getPassword(),
                    String.join(", ", faculty.getSubjects()),
                    faculty.getMaxHoursPerDay());
        }
    }

    public static void printRooms(List<Room> roomList) {
        System.out.println("=== Room List ===");
        for (Room room : roomList) {
            System.out.println("ID: " + room.getId() +
                    ", Name: " + room.getRoomNumber() +
                    ", Capacity: " + room.getCapacity() +
                    ", Type: " + room.getType());
        }
        System.out.println();
    }

    public static void printCourses(List<Course> courseList) {
        System.out.println("=== Course List ===");
        for (Course course : courseList) {
            System.out.println("ID: " + course.getId() +
                    ", Code: " + course.getCourseCode() +
                    ", Name: " + course.getName() +
                    ", Type: " + course.getCourseType() +
                    ", Branch: " + course.getBranch() +
                    ", Section: " + course.getSection() +
                    ", Lecture Hours: " + course.getLectureHours() +
                    ", Theory Hours: " + course.getTheoryHours() +
                    ", Practical Hours: " + course.getPracticalHours() +
                    ", Credits: " + course.getCredits() +
                    ", Eligible Faculty: " + course.getEligibleFaculty().stream()
                    .map(Faculty::getName)
                    .collect(Collectors.joining(", ")));
        }
        System.out.println();
    }

    public static void printStudentBatches(List<StudentBatch> batchList) {
        System.out.println("=== Student Batch List ===");
        for (StudentBatch batch : batchList) {
            System.out.println("ID: " + batch.getId() +
                    ", Name: " + batch.getBatchName() +
                    ", Strength: " + batch.getStrength() +
                    ", Year: " + batch.getYear() +
                    ", Courses: " + batch.getCourses().stream()
                    .map(Course::getName)
                    .collect(Collectors.joining(", ")) +
                    ", Lecture Room IDs: " + batch.getLectureRoomIDs() +
                    ", Practical Room IDs: " + batch.getPracticalRoomIDs());
        }
        System.out.println();
    }
}
