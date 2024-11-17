package com.timetable;

import com.timetable.domain.*;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import com.timetable.score.TimeTableConstraintProvider;
import com.timetable.util.CSVDataLoader;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TimeTableApp {
    private static final Logger logger = Logger.getLogger(TimeTableApp.class.getName());

    // Define the fixed time slots for lectures and labs
    private static final Object[][] TIME_SLOT_DEFINITIONS = {
            {LocalTime.of(9, 0), LocalTime.of(10, 30), "LECTURE"},
            {LocalTime.of(10, 45), LocalTime.of(12, 15), "LECTURE"},
            {LocalTime.of(11, 45), LocalTime.of(12, 45), "LECTURE"},
            {LocalTime.of(12, 15), LocalTime.of(13, 15), "LECTURE"},
            {LocalTime.of(13, 30), LocalTime.of(15, 0), "LECTURE"},
            {LocalTime.of(14, 30), LocalTime.of(16, 0), "LECTURE"},
            {LocalTime.of(15, 0), LocalTime.of(16, 30), "LECTURE"},
            {LocalTime.of(16, 0), LocalTime.of(17, 30), "LECTURE"},
            {LocalTime.of(16, 30), LocalTime.of(17, 30), "LECTURE"},
            {LocalTime.of(16, 30), LocalTime.of(18, 0), "LECTURE"},
            {LocalTime.of(9, 0), LocalTime.of(11, 0), "LAB"},
            {LocalTime.of(11, 15), LocalTime.of(13, 15), "LAB"},
            {LocalTime.of(14, 30), LocalTime.of(16, 30), "LAB"}
    };


    public static void main(String[] args) {
        try {
            // Load data from CSV files
            List<Faculty> facultyList = CSVDataLoader.loadFaculty("faculty.csv");
            List<Room> roomList = CSVDataLoader.loadRooms("rooms.csv");
            List<Course> courseList = CSVDataLoader.loadCourses("courses.csv", facultyList);
            List<StudentBatch> batchList = CSVDataLoader.loadStudentBatches("batches.csv", courseList);

            if (facultyList.isEmpty() || roomList.isEmpty() || courseList.isEmpty() || batchList.isEmpty())
                throw new RuntimeException("Essential data missing");


            List<TimeSlot> timeSlotList = createTimeSlots();
            logger.info("Created " + timeSlotList.size() + " time slots");

            // Create initial solution with categorized rooms
            TimeTable problem = createInitialSolution(facultyList, roomList, timeSlotList, batchList, courseList);
            logger.info("Created initial solution with " + problem.getLessonList().size() + " lessons");

            // Configure solver
            SolverConfig solverConfig = new SolverConfig()
                    .withSolutionClass(TimeTable.class)
                    .withEntityClasses(Lesson.class)
                    .withConstraintProviderClass(TimeTableConstraintProvider.class)
                    .withTerminationSpentLimit(Duration.ofMinutes(10));

            // Solve timetable
            SolverFactory<TimeTable> solverFactory = SolverFactory.create(solverConfig);
            Solver<TimeTable> solver = solverFactory.buildSolver();

            logger.info("Starting solver...");
            TimeTable solution = solver.solve(problem);
            logger.info("Solver finished. Score: " + solution.getScore());

            printSolution(solution);

            exportSolutionToCSV(solution, "final_timetable.csv"); // Added export to CSV

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error generating timetable", e);
            System.exit(1);
        }
    }


    // Modified to create specific time slots for lectures and labs
    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        Long id = 1L;

        // Create time slots for each day
        for (String day : days) {
            for (Object[] slotDef : TIME_SLOT_DEFINITIONS) {
                LocalTime startTime = (LocalTime) slotDef[0];
                LocalTime endTime = (LocalTime) slotDef[1];
                String slotType = (String) slotDef[2];

                timeSlots.add(new TimeSlot(id++, day, startTime, endTime, slotType));
            }
        }
        return timeSlots;
    }

    // Create initial solution with course and batch information
    private static TimeTable createInitialSolution(List<Faculty> facultyList,
                                                   List<Room> roomList,
                                                   List<TimeSlot> timeSlotList,
                                                   List<StudentBatch> batchList,
                                                   List<Course> courseList) {
        List<Lesson> lessonList = new ArrayList<>();
        Long lessonId = 1L;

        // Sort batches by ID to ensure lessons are created in batch ID order
        batchList.sort(Comparator.comparingLong(StudentBatch::getId));

        for (StudentBatch batch : batchList) {
            if (batch.getLectureRoomIDs() == null || batch.getLectureRoomIDs().isEmpty()) {
                logger.severe(String.format("Batch %s has no lecture rooms assigned", batch.getBatchName()));
                continue;
            }

            List<Room> batchLectureRooms = new ArrayList<>();
            List<Room> batchPracticalRooms = new ArrayList<>();

            // Load lecture rooms for this batch
            for (Long roomId : batch.getLectureRoomIDs()) {
                Room room = getRoomById(roomId, roomList);
                if (room != null) {
                    batchLectureRooms.add(room);
                } else {
                    logger.warning(String.format("Lecture room ID %d not found for batch %s", roomId, batch.getBatchName()));
                }
            }

            // Load practical rooms for this batch
            for (Long roomId : batch.getPracticalRoomIDs()) {
                Room room = getRoomById(roomId, roomList);
                if (room != null) {
                    batchPracticalRooms.add(room);
                } else {
                    logger.warning(String.format("Practical room ID %d not found for batch %s", roomId, batch.getBatchName()));
                }
            }

            for (Course course : batch.getCourses()) {
                if (course.getEligibleFaculty() == null || course.getEligibleFaculty().isEmpty()) {
                    logger.warning("Course " + course.getName() + " has no eligible faculty");
                    continue;
                }

                // Create lecture lessons
                for (int i = 0; i < course.getLectureHours(); i++) {
                    Lesson lesson = new Lesson(lessonId++, course, batch, roomList);
                    lesson.setLessonType("LECTURE");

                    if (!batchLectureRooms.isEmpty()) {
                        Room lectureRoom = batchLectureRooms.get(i % batchLectureRooms.size());
                        lesson.setRoom(lectureRoom);
                        logger.info(String.format("Created LECTURE lesson - Course: %s, Batch: %s, Room: %s, Lesson ID: %d",
                                course.getCourseCode(),
                                batch.getBatchName(),
                                lectureRoom.getRoomNumber(),
                                lesson.getId()));
                    } else {
                        logger.warning(String.format("No lecture rooms available for batch %s", batch.getBatchName()));
                    }
                    lessonList.add(lesson);
                }

                // Create theory lessons
                for (int i = 0; i < course.getTheoryHours(); i++) {
                    Lesson lesson = new Lesson(lessonId++, course, batch, roomList);
                    lesson.setLessonType("LECTURE");

                    if (!batchLectureRooms.isEmpty()) {
                        Room lectureRoom = batchLectureRooms.get(i % batchLectureRooms.size());
                        lesson.setRoom(lectureRoom);
                        logger.info(String.format("Created THEORY lesson - Course: %s, Batch: %s, Room: %s, Lesson ID: %d",
                                course.getCourseCode(),
                                batch.getBatchName(),
                                lectureRoom.getRoomNumber(),
                                lesson.getId()));
                    } else {
                        logger.warning(String.format("No theory rooms available for batch %s", batch.getBatchName()));
                    }
                    lessonList.add(lesson);
                }

                // Create lab lessons
                for (int i = 0; i < course.getPracticalHours(); i += 2) {
                    Lesson lesson = new Lesson(lessonId++, course, batch, roomList);
                    lesson.setLessonType("LAB");

                    if (!batchPracticalRooms.isEmpty()) {
                        Room practicalRoom = batchPracticalRooms.get(i % batchPracticalRooms.size());
                        lesson.setRoom(practicalRoom);
                        logger.info(String.format("Created LAB lesson - Course: %s, Batch: %s, Room: %s, Lesson ID: %d",
                                course.getCourseCode(),
                                batch.getBatchName(),
                                practicalRoom.getRoomNumber(),
                                lesson.getId()));
                    } else {
                        logger.warning(String.format("No practical rooms available for batch %s", batch.getBatchName()));
                    }
                    lessonList.add(lesson);
                }
            }
        }

        logger.info(String.format("Initial solution created with %d total lessons", lessonList.size()));
        return new TimeTable(1L, lessonList, facultyList, roomList, timeSlotList);
    }


    public static Room getRoomById(Long roomId, List<Room> roomList) {
        if (roomId == null || roomList == null) {
            return null;
        }
        for (Room room : roomList) {
            if (room.getId().equals(roomId)) {
                return room;
            }
        }
        return null; // Return null if no matching room is found
    }


    // Print solution timetable
    private static void printSolution(TimeTable solution) {
        System.out.println("\nSolved Timetable:");
        System.out.println("Score: " + solution.getScore());
        System.out.println("------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-13s | %-13s | %-10s | %-40s | %-10s%n | %-30s%n",
                "Day", "Time", "Room", "Batch", "Course", "Type", "Faculty");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------");


        solution.getLessonList().stream()
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null && lesson.getFaculty() != null)
                .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                        .thenComparing(lesson -> lesson.getStudentBatch().getBatchName())
                        .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                .forEach(lesson -> System.out.printf("%-10s | %-13s | %-13s | %-10s | %-40s| %-10s%n | %-30s%n",
                        lesson.getTimeSlot().getDay(),
                        lesson.getTimeSlot().getStartTime() + "-" + lesson.getTimeSlot().getEndTime(),
                        lesson.getRoom().getRoomNumber(),
                        lesson.getStudentBatch().getBatchName(),
                        lesson.getCourse().getName(),
                        lesson.getLessonType(),
                        lesson.getFaculty().getName()));
        System.out.println("------------------------------------------------------------");
    }

    // Export solution to a CSV file
    private static void exportSolutionToCSV(TimeTable solution, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Day,Time,Room,Batch,Course,Type,Faculty\n");

            solution.getLessonList().stream()
                    .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null && lesson.getFaculty() != null)
                    .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                            .thenComparing(lesson -> lesson.getStudentBatch().getBatchName())
                            .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .forEach(lesson -> {
                        try {
                            writer.write(String.format("%s,%s-%s,%s,%s,%s,%s,%s\n",
                                    lesson.getTimeSlot().getDay(),
                                    lesson.getTimeSlot().getStartTime(),
                                    lesson.getTimeSlot().getEndTime(),
                                    lesson.getRoom().getRoomNumber(),
                                    lesson.getStudentBatch().getBatchName(),
                                    lesson.getCourse().getName(),
                                    lesson.getLessonType(),
                                    lesson.getFaculty().getName()));
                        } catch (IOException e) {
                            logger.log(Level.SEVERE, "Error writing to CSV", e);
                        }
                    });
            logger.info("Timetable exported to " + fileName);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating CSV file", e);
        }
    }

    // Map weekdays to indices for sorting
    private static int dayToIndex(String day) {
        switch (day) {
            case "Monday": return 1;
            case "Tuesday": return 2;
            case "Wednesday": return 3;
            case "Thursday": return 4;
            case "Friday": return 5;
            default: return 0;
        }
    }
}
