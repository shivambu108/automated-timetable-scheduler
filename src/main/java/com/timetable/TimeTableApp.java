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

            // Create initial solution
            TimeTable problem = createInitialSolution(facultyList, roomList, timeSlotList, batchList, courseList);
            logger.info("Created initial solution with " + problem.getLessonList().size() + " lessons");

            // Configure solver
            SolverConfig solverConfig = new SolverConfig()
                    .withSolutionClass(TimeTable.class)
                    .withEntityClasses(Lesson.class)
                    .withConstraintProviderClass(TimeTableConstraintProvider.class)
                    .withTerminationSpentLimit(Duration.ofMinutes(5));

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

    // Generate time slots for each day of the week
    private static List<TimeSlot> createTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        Long id = 1L;
        for (String day : days) {
            for (int hour = 9; hour <= 16; hour++) {
                timeSlots.add(new TimeSlot(id++, day, LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0)));
            }
        }
        return timeSlots;
    }

    // Create initial solution with course and batch information
    private static TimeTable createInitialSolution(List<Faculty> facultyList, List<Room> roomList,
                                                   List<TimeSlot> timeSlotList, List<StudentBatch> batchList, List<Course> courseList) {

        List<Lesson> lessonList = new ArrayList<>();
        Long lessonId = 1L;

        for (StudentBatch batch : batchList) {
            for (Course course : batch.getCourses()) {
                if (course.getEligibleFaculty() == null || course.getEligibleFaculty().isEmpty()) {
                    logger.warning("Course " + course.getName() + " has no eligible faculty");
                    continue;
                }

                // Adjust lesson creation to consider practical labs if needed
                logger.info("Creating " + (course.getPracticalHours() > 0 ? "lab" : "regular") + " lesson for course " + course.getName());

                // Adding lessons based on course hours
                for (int i = 0; i < course.getHoursPerWeek(); i++) {
                    Lesson lesson = new Lesson(lessonId++, course, batch,roomList);
                    lesson.assignPredefinedRoom(); // Automatically assign the predefined room based on course type (lab or regular)
                    lessonList.add(lesson); // Add the lesson to the list
                }
            }
        }
        return new TimeTable(1L, lessonList, facultyList, roomList, timeSlotList);
    }

    // Print solution timetable
    private static void printSolution(TimeTable solution) {
        System.out.println("\nSolved Timetable:");
        System.out.println("Score: " + solution.getScore());
        System.out.println("------------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-15s | %-15s | %-15s | %-50s | %-30s%n",
                "Day", "Time", "Room", "Batch", "Course", "Faculty");
        System.out.println("------------------------------------------------------------------------------------------------------------------------------");


        solution.getLessonList().stream()
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null && lesson.getFaculty() != null)
                .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                        .thenComparing(lesson -> lesson.getStudentBatch().getBatchName())
                        .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                .forEach(lesson -> System.out.printf("%-10s | %-15s | %-15s | %-15s | %-50s | %-30s%n",
                        lesson.getTimeSlot().getDay(),
                        lesson.getTimeSlot().getStartTime() + "-" + lesson.getTimeSlot().getEndTime(),
                        lesson.getRoom().getRoomNumber(),
                        lesson.getStudentBatch().getBatchName(),
                        lesson.getCourse().getName(),
                        lesson.getFaculty().getName()));
        System.out.println("------------------------------------------------------------");
    }

    // Export solution to a CSV file
    private static void exportSolutionToCSV(TimeTable solution, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Day,Time,Room,Batch,Course,Faculty\n");

            solution.getLessonList().stream()
                    .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null && lesson.getFaculty() != null)
                    .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                            .thenComparing(lesson -> lesson.getStudentBatch().getBatchName())
                            .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .forEach(lesson -> {
                        try {
                            writer.write(String.format("%s,%s-%s,%s,%s,%s,%s\n",
                                    lesson.getTimeSlot().getDay(),
                                    lesson.getTimeSlot().getStartTime(),
                                    lesson.getTimeSlot().getEndTime(),
                                    lesson.getRoom().getRoomNumber(),
                                    lesson.getStudentBatch().getBatchName(),
                                    lesson.getCourse().getName(),
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
