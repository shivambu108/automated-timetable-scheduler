package com.timetable;

import com.timetable.domain.*;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.LocalSearchType;
import org.optaplanner.core.config.solver.SolverConfig;
import com.timetable.score.TimeTableConstraintProvider;
import com.timetable.util.CSVDataLoader;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.optaplanner.core.config.localsearch.LocalSearchType;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TimeTableApp {
    private static final Logger logger = Logger.getLogger(TimeTableApp.class.getName());

    // Defining the fixed time slots for lectures and labs
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
            {LocalTime.of(14, 30), LocalTime.of(16, 30), "LAB"},

    };

    private static final Object[][] TIME_SLOT_DEFINITIONS_Year1 = {
            {LocalTime.of(9, 0), LocalTime.of(10, 30), "LECTURE"},
            {LocalTime.of(10, 45), LocalTime.of(12, 15), "LECTURE"},
            {LocalTime.of(12, 15), LocalTime.of(13, 15), "LECTURE"},
            {LocalTime.of(14, 30), LocalTime.of(16, 0), "LECTURE"},
            {LocalTime.of(16, 15), LocalTime.of(17, 45), "LECTURE"},
            {LocalTime.of(11, 15), LocalTime.of(13, 15), "LAB"},
            {LocalTime.of(14, 30), LocalTime.of(16, 30), "LAB"},

    };

    private static final Object[][] TIME_SLOT_DEFINITIONS_Year2 = {
            {LocalTime.of(9, 0), LocalTime.of(10, 30), "LECTURE"},
            {LocalTime.of(10, 45), LocalTime.of(12, 15), "LECTURE"},
            {LocalTime.of(12, 15), LocalTime.of(13, 15), "LECTURE"},
            {LocalTime.of(14, 30), LocalTime.of(16, 0), "LECTURE"},
            {LocalTime.of(14, 30), LocalTime.of(16, 30), "LAB"},

    };

    private static final Object[][] TIME_SLOT_DEFINITIONS_Year3 = {
            {LocalTime.of(9, 0), LocalTime.of(10, 30), "LECTURE"},
            {LocalTime.of(11, 15), LocalTime.of(12, 15), "LECTURE"},
            {LocalTime.of(13, 30), LocalTime.of(15, 0), "LECTURE"},
            {LocalTime.of(15, 15), LocalTime.of(16, 45), "LECTURE"},
            {LocalTime.of(17, 0), LocalTime.of(18, 0), "LECTURE"},
            {LocalTime.of(9, 0), LocalTime.of(11, 0), "LAB"},

    };

    private static final Object[][] TIME_SLOT_DEFINITIONS_Year4 = {
            {LocalTime.of(9, 0), LocalTime.of(10, 30), "LECTURE"},
            {LocalTime.of(13, 30), LocalTime.of(14, 30), "LECTURE"},
            {LocalTime.of(14, 45), LocalTime.of(16, 15), "LECTURE"},
            {LocalTime.of(16, 30), LocalTime.of(18, 0), "LECTURE"},

    };

    // Defining the fixed time slots for lectures and labs
    private static final Object[][] MINOR_TIME_SLOT_DEFINITIONS = {
            {LocalTime.of(8, 0), LocalTime.of(9, 0), "MINOR"},
            {LocalTime.of(18, 0), LocalTime.of(19, 30), "MINOR"} // evening slot for minor courses
    };


    public static void main(String[] args) {
        try {
            // Load data from CSV files
            List<Faculty> facultyList = CSVDataLoader.loadFaculty("faculty.csv");
            List<Room> roomList = CSVDataLoader.loadRooms("rooms.csv");
            List<Course> courseList = CSVDataLoader.loadCourses("courses.csv", facultyList);
            List<Course> minorCourseList = CSVDataLoader.loadMinors("minor.csv", facultyList);
            List<StudentBatch> batchList = CSVDataLoader.loadStudentBatches("batches.csv", courseList);

            if (facultyList.isEmpty() || roomList.isEmpty() || courseList.isEmpty()|| minorCourseList.isEmpty() || batchList.isEmpty())
                throw new RuntimeException("Essential data missing");


            List<TimeSlot> timeSlotList = new ArrayList<>();
            for (StudentBatch batch : batchList) {
                timeSlotList.addAll(createTimeSlots(batch));
            }
            List<TimeSlot> minorTimeSlotList = createMinorTimeSlots();
            logger.info("Created " + timeSlotList.size() + " time slots");

            // Create initial solution with categorized rooms
            TimeTable problem = createInitialSolution(facultyList, roomList, timeSlotList, minorTimeSlotList, batchList, courseList, minorCourseList);
            logger.info("Created initial solution with " + problem.getLessonList().size() + " lessons and " + problem.getMinorLessonList().size() + " minor lessons");

            // Configure solver
            SolverConfig solverConfig = new SolverConfig()
                    .withSolutionClass(TimeTable.class)
                    .withEntityClasses(Lesson.class)
                    .withConstraintProviderClass(TimeTableConstraintProvider.class)
                    .withTerminationSpentLimit(Duration.ofMinutes(15));

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


//    // Modified to create specific time slots for lectures and labs
//    private static List<TimeSlot> createTimeSlots() {
//        List<TimeSlot> timeSlots = new ArrayList<>();
//        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
//        Long id = 1L;
//
//        // Create time slots for each day
//        for (String day : days) {
//            for (Object[] slotDef : TIME_SLOT_DEFINITIONS) {
//                LocalTime startTime = (LocalTime) slotDef[0];
//                LocalTime endTime = (LocalTime) slotDef[1];
//                String slotType = (String) slotDef[2];
//
//                timeSlots.add(new TimeSlot(id++, day, startTime, endTime, slotType));
//            }
//        }
//        return timeSlots;
//    }

    // MODIFY createTimeSlots method to handle year-specific slots
    private static List<TimeSlot> createTimeSlots(StudentBatch batch) {
        List<TimeSlot> timeSlots = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        Long id = 1L;

        // Determine which time slot definitions to use based on the batch year
        Object[][] selectedTimeSlotDefinitions;
        switch (batch.getYear()) {
            case 2021:
                selectedTimeSlotDefinitions = TIME_SLOT_DEFINITIONS_Year4;
                break;
            case 2022:
                selectedTimeSlotDefinitions = TIME_SLOT_DEFINITIONS_Year3;
                break;
            case 2023:
                selectedTimeSlotDefinitions = TIME_SLOT_DEFINITIONS_Year2;
                break;
            case 2024:
                selectedTimeSlotDefinitions = TIME_SLOT_DEFINITIONS_Year1; // Using Year1 for 2024 batch
                break;
            default:
                selectedTimeSlotDefinitions = TIME_SLOT_DEFINITIONS_Year1;
        }

        // Create time slots for each day using the selected definitions
        for (String day : days) {
            for (Object[] slotDef : selectedTimeSlotDefinitions) {
                LocalTime startTime = (LocalTime) slotDef[0];
                LocalTime endTime = (LocalTime) slotDef[1];
                String slotType = (String) slotDef[2];

                timeSlots.add(new TimeSlot(id++, day, startTime, endTime, slotType));
            }
        }
        return timeSlots;
    }

    private static List<TimeSlot> createMinorTimeSlots() {
        List<TimeSlot> minorTimeSlots = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
        Long id = 1L;

        // Create time slots for each day
        for (String day : days) {
            for (Object[] slotDef : MINOR_TIME_SLOT_DEFINITIONS) {
                LocalTime startTime = (LocalTime) slotDef[0];
                LocalTime endTime = (LocalTime) slotDef[1];
                String slotType = (String) slotDef[2];

                minorTimeSlots.add(new TimeSlot(id++, day, startTime, endTime, slotType));
            }
        }
        return minorTimeSlots;
    }

    // Create initial solution with course and batch information
    private static TimeTable createInitialSolution(List<Faculty> facultyList,
                                                   List<Room> roomList,
                                                   List<TimeSlot> timeSlotList,
                                                   List<TimeSlot> minorTimeSlotList,
                                                   List<StudentBatch> batchList,
                                                   List<Course> courseList,
                                                   List<Course> minorCourseList) {
        List<Lesson> lessonList = new ArrayList<>();
        List<Lesson> minorLessonList = new ArrayList<>(); // Separate list for minor lessons
        Long lessonId = 1L;

        // Sorting batches by ID to ensure lessons are created in batch ID order
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

                        Faculty faculty = course.getEligibleFaculty().get(i % course.getEligibleFaculty().size());
                        lesson.setFaculty(faculty);

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
//
                        Faculty faculty = course.getEligibleFaculty().get(i % course.getEligibleFaculty().size());
                        lesson.setFaculty(faculty);

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
//
                        Faculty faculty = course.getEligibleFaculty().get(i % course.getEligibleFaculty().size());
                        lesson.setFaculty(faculty);

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

        // Create minor lessons independently of any batch
        TimeSlot minorTimeSlot = minorTimeSlotList.stream()
                .filter(slot -> slot.getStartTime().equals(LocalTime.of(18, 0)) &&
                        slot.getEndTime().equals(LocalTime.of(19, 30)) &&
                        slot.getSlotType().equals("MINOR"))
                .findFirst()
                .orElse(null);

        if (minorTimeSlot != null) {
            for (Course minorCourse : minorCourseList) {
                if (minorCourse.getEligibleFaculty() == null || minorCourse.getEligibleFaculty().isEmpty()) {
                    logger.warning("Minor course " + minorCourse.getName() + " has no eligible faculty");
                    continue;
                }

                List<Room> minorLectureRooms = new ArrayList<>();

                // Load lecture rooms for this minor
                for (Long roomId : minorCourse.getLectureRoomIDs()) {
                    Room room = getRoomById(roomId, roomList);
                    if (room != null) {
                        minorLectureRooms.add(room);
                    } else {
                        logger.warning(String.format("Lecture room ID %d not found for minor %s", roomId, minorCourse.getCourseCode()));
                    }
                }

//                for (Faculty faculty : minorCourse.getEligibleFaculty()) {
//                    Room minorRoom = minorLectureRooms.get(0);
//                    Lesson minorLesson = new Lesson(lessonId++, minorCourse, null, roomList);
//                    minorLesson.setLessonType("MINOR");
//                    minorLesson.setFaculty(faculty);
//                    minorLesson.setRoom(minorRoom);
//                    minorLesson.setTimeSlot(minorTimeSlot);
//
//                    minorLessonList.add(minorLesson);
//                    logger.info(String.format("Created MINOR lesson - Course: %s, Room: %s, Faculty: %s, Lesson ID: %d",
//                            minorCourse.getCourseCode(),
//                            minorRoom.getRoomNumber(),
//                            faculty.getName(),
//                            minorLesson.getId()));
//                }
//

                for (int i = 0; i < minorCourse.getLectureHours(); i++) {
                    Lesson minorLesson = new Lesson(lessonId++, minorCourse, roomList);
                    minorLesson.setLessonType("MINOR");

                    if (!minorLectureRooms.isEmpty()) {
                        Room minorRoom = minorLectureRooms.get(i % minorLectureRooms.size());
                        minorLesson.setRoom(minorRoom);
                        minorLesson.setMinorTimeSlot(minorTimeSlot);
//                        minorLesson.setTimeSlot(minorTimeSlot);
//
                        Faculty faculty = minorCourse.getEligibleFaculty().get(i % minorCourse.getEligibleFaculty().size());
                        minorLesson.setFaculty(faculty);

                        logger.info(String.format("Created MINOR lesson - Course: %s, Batch: %s, Room: %s, Lesson ID: %d",
                                minorCourse.getCourseCode(),
                                "ALL",
                                minorRoom.getRoomNumber(),
                                minorLesson.getId()));
                    } else {
                        logger.warning(String.format("No lecture rooms available for minor %s", minorCourse.getCourseCode()));
                    }
                    minorLessonList.add(minorLesson);
                }

            }
        } else {
            logger.warning("No available time slot for minor lessons");
        }



        logger.info(String.format("Initial solution created with %d total lessons and %d minor lessons", lessonList.size(), minorLessonList.size()));
        return new TimeTable(1L, lessonList, minorLessonList, facultyList, roomList, timeSlotList, minorTimeSlotList);
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
        // Header
        System.out.println("\nTimetable Schedule");
        System.out.println("Score: " + solution.getScore());

        // Create format string for consistent column widths
        String headerFormat = "| %-8s | %-12s | %-8s | %-10s | %-35s | %-8s | %-25s |%n";
        String lineFormat  = "+----------+--------------+----------+------------+-------------------------------------+----------+---------------------------+%n";

        // Print table header
        System.out.format(lineFormat);
        System.out.format(headerFormat, "Day", "Time", "Room", "Batch", "Course", "Type", "Faculty");
        System.out.format(lineFormat);

        // Sort and print lessons
        solution.getLessonList().stream()
                .filter(lesson -> lesson.getTimeSlot() != null &&
                        lesson.getRoom() != null &&
                        lesson.getFaculty() != null)
                .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                        .thenComparing(lesson -> lesson.getStudentBatch().getBatchName())
                        .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                .forEach(lesson -> {
                    System.out.format(headerFormat,
                            lesson.getTimeSlot().getDay(),
                            lesson.getTimeSlot().getStartTime() + "-" + lesson.getTimeSlot().getEndTime(),
                            lesson.getRoom().getRoomNumber(),
                            lesson.getStudentBatch().getBatchName(),
                            lesson.getCourse().getName(),
                            lesson.getLessonType(),
                            lesson.getFaculty().getName()
                    );
                });

        solution.getMinorLessonList().stream()
                .filter(lesson -> lesson.getTimeSlot() != null &&
                        lesson.getRoom() != null &&
                        lesson.getFaculty() != null)
                .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                        .thenComparing(lesson -> lesson.getId())
                        .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                .forEach(lesson -> {
                    System.out.format(headerFormat,
                            lesson.getTimeSlot().getDay(),
                            lesson.getTimeSlot().getStartTime() + "-" + lesson.getTimeSlot().getEndTime(),
                            lesson.getRoom().getRoomNumber(),
                            "ALL",
                            lesson.getCourse().getName(),
                            lesson.getLessonType(),
                            lesson.getFaculty().getName()
                    );
                });

        // Print bottom border
        System.out.format(lineFormat);
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

            solution.getMinorLessonList().stream()
                    .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null && lesson.getFaculty() != null)
                    .sorted(Comparator.comparing((Lesson lesson) -> dayToIndex(lesson.getTimeSlot().getDay()))
                            .thenComparing(lesson -> lesson.getId())
                            .thenComparing(lesson -> lesson.getTimeSlot().getStartTime()))
                    .forEach(lesson -> {
                        try {
                            writer.write(String.format("%s,%s-%s,%s,%s,%s,%s,%s\n",
                                    lesson.getTimeSlot().getDay(),
                                    lesson.getTimeSlot().getStartTime(),
                                    lesson.getTimeSlot().getEndTime(),
                                    lesson.getRoom().getRoomNumber(),
                                    "ALL",
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

    // Mapping weekdays to indices for sorting
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
