package com.timetable.util;

import com.timetable.domain.*;
import com.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CSVDataLoader {
    private static final Logger logger = Logger.getLogger(CSVDataLoader.class.getName());
    // Removes lines starting with "#" and empty lines from the CSV content
    private static String removeComments(String csvFile) {
        StringBuilder filteredContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip lines that are comments or are empty
                if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
                    filteredContent.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error while removing comments from file: " + csvFile, e);
        }
        return filteredContent.toString();
    }

    public static List<Faculty> loadFaculty(String csvFile) {
        List<Faculty> facultyList = new ArrayList<>();
        String csvContent = removeComments(csvFile);

        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            List<String[]> rows = reader.readAll();
            if (rows.size() <= 1) {
                logger.warning("No faculty data found in CSV file");
                return facultyList;
            }
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 6) {
                    logger.warning("Invalid row at line " + i + ": insufficient columns");
                    continue;
                }
                try {
                    facultyList.add(getFaculty(row));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing faculty row " + i, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading faculty data", e);
        }
        return facultyList;
    }

    public static List<Room> loadRooms(String csvFile) {
        List<Room> roomList = new ArrayList<>();
        String csvContent = removeComments(csvFile);

        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            List<String[]> rows = reader.readAll();
            if (rows.size() <= 1) {
                logger.warning("No room data found in CSV file");
                return roomList;
            }
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 4) {
                    logger.warning("Invalid row at line " + i + ": insufficient columns");
                    continue;
                }
                try {
                    Room room = new Room(
                            Long.parseLong(row[0].trim()),
                            row[1].trim(),
                            Integer.parseInt(row[2].trim()),
                            RoomType.valueOf(row[3].trim().replace(" ", "_").toUpperCase()) // Adjusted for new room types
                    );
                    roomList.add(room);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing room row " + i, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading room data", e);
        }
        return roomList;
    }

    // Updated to handle new Course attributes based on the revised CSV structure
    public static List<Course> loadCourses(String csvFile, List<Faculty> facultyList) {
        List<Course> courseList = new ArrayList<>();
        String csvContent = removeComments(csvFile);

        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            List<String[]> rows = reader.readAll();
            if (rows.size() <= 1) {
                logger.warning("No course data found in CSV file");
                return courseList;
            }
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 11) {
                    logger.warning("Invalid row at line " + i + ": insufficient columns");
                    continue;
                }
                try {
                    List<Faculty> eligibleFaculty = new ArrayList<>();
                    for (String facultyId : row[10].trim().split(";")) {
                        facultyList.stream()
                                .filter(f -> f.getId().equals(Long.parseLong(facultyId.trim())))
                                .findFirst()
                                .ifPresent(eligibleFaculty::add);
                    }

                    // Parse batchIds (changed from branch and section to batchIds)
                    List<Integer> batchIds = new ArrayList<>();
                    for (String batchId : row[4].trim().split(";")) {
                        batchIds.add(Integer.parseInt(batchId.trim()));
                    }

                    Course course = new Course(
                            Long.parseLong(row[0].trim()),                // id
                            row[1].trim(),                                // courseCode
                            row[2].trim(),                                // name
                            row[3].trim(),                                // courseType (regular or elective)
                            batchIds,                                     // List of batch IDs
                            Integer.parseInt(row[5].trim()),              // lecture hours
                            Integer.parseInt(row[6].trim()),              // theory hours
                            Integer.parseInt(row[7].trim()),              // practical hours
                            Integer.parseInt(row[8].trim()),              // credits
                            eligibleFaculty                               // eligible faculty list
                    );

                    courseList.add(course);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing course row " + i, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading course data", e);
        }
        return courseList;
    }

    public static List<Course> loadMinors(String csvFile, List<Faculty> facultyList) {
        List<Course> minorCourses = new ArrayList<>();
        String csvContent = removeComments(csvFile);

        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            List<String[]> rows = reader.readAll();
            if (rows.size() <= 1) {
                logger.warning("No minor data found in CSV file");
                return minorCourses;
            }
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 12) {
                    logger.warning("Invalid row at line " + i + ": insufficient columns");
                    continue;
                }
                try {
                    List<Faculty> eligibleFaculty = new ArrayList<>();
                    for (String facultyId : row[10].trim().split(";")) {
                        facultyList.stream()
                                .filter(f -> f.getId().equals(Long.parseLong(facultyId.trim())))
                                .findFirst()
                                .ifPresent(eligibleFaculty::add);
                    }

                    List<Long> lectureRoomIDs = parseRoomIDs(row[11]);

                    Course minor = new Course(
                            Long.parseLong(row[0].trim()),               // id
                            row[1].trim(),                               // courseCode
                            row[2].trim(),                               // name
                            row[3].trim(),                               // courseType
                            List.of(-1),                             // batchId (-1 for ALL)
                            Integer.parseInt(row[5].trim()),             // lecture hours
                            Integer.parseInt(row[6].trim()),             // theory hours
                            Integer.parseInt(row[7].trim()),             // practical hours
                            Integer.parseInt(row[8].trim()),             // credits
                            eligibleFaculty,                             // eligible faculty list
                            lectureRoomIDs                               // lectureRoomIDs
                    );

                    minorCourses.add(minor);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing minor row " + i, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading minor data", e);
        }
        return minorCourses;
    }

    public static List<StudentBatch> loadStudentBatches(String csvFile, List<Course> courseList) {
        List<StudentBatch> batchList = new ArrayList<>();
        String csvContent = removeComments(csvFile);

        try (CSVReader reader = new CSVReader(new StringReader(csvContent))) {
            List<String[]> rows = reader.readAll();
            if (rows.size() <= 1) {
                logger.warning("No batch data found in CSV file");
                return batchList;
            }
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length < 7) {
                    logger.warning("Invalid row at line " + i + ": insufficient columns");
                    continue;
                }
                try {
                    List<Course> courses = new ArrayList<>();
                    for (String courseId : row[4].trim().split(";")) {
                        courseList.stream()
                                .filter(c -> c.getId().equals(Long.parseLong(courseId.trim())))
                                .findFirst()
                                .ifPresent(courses::add);
                    }

                    List<Long> lectureRoomIDs = parseRoomIDs(row[5]);
                    List<Long> practicalRoomIDs = parseRoomIDs(row[6]);

                    batchList.add(new StudentBatch(
                            Long.parseLong(row[0].trim()), row[1].trim(),
                            Integer.parseInt(row[2].trim()), Integer.parseInt(row[3].trim()),
                            courses, lectureRoomIDs, practicalRoomIDs
                    ));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error processing batch row " + i, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading batch data", e);
        }
        return batchList;
    }

    // Helper method to parse room IDs
    private static List<Long> parseRoomIDs(String roomIDs) {
        return Stream.of(roomIDs.split(";"))
                .map(id -> Long.parseLong(id.trim()))
                .collect(Collectors.toList());
    }


    private static Faculty getFaculty(String[] row) {
        return new Faculty(
                Long.parseLong(row[0].trim()), row[1].trim(), row[2].trim(),
                row[3].trim(), List.of(row[4].trim().split(";")),
                Integer.parseInt(row[5].trim())
        );
    }
}
