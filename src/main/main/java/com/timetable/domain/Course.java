package com.timetable.domain;

import java.util.List;

/**
 * Represents a course with details such as type, hours, eligible faculty, and associated batches.
 */
public class Course {
    private Long id;
    private String courseCode;
    private String name;
    private String courseType; // Regular or elective
    private List<Integer> batchIds; // Batch identifiers
    private int lectureHours;
    private int theoryHours;
    private int practicalHours; // Hours for practical sessions
    private int credits;
    private int hoursPerWeek; // Calculated from lecture, theory, and practical hours
    private List<Faculty> eligibleFaculty; // Faculty eligible to teach the course
    private List<Long> lectureRoomIDs; // Specific to minors

    private boolean isMinor;

    // Constructor to initialize course details
    public Course(Long id, String courseCode, String name, String courseType, List<Integer> batchIds,
                  int lectureHours, int theoryHours, int practicalHours, int credits, List<Faculty> eligibleFaculty) {
        this.id = id;
        this.courseCode = courseCode;
        this.name = name;
        this.courseType = courseType;
        this.batchIds = batchIds;
        this.lectureHours = lectureHours;
        this.theoryHours = theoryHours;
        this.practicalHours = practicalHours;
        this.credits = credits;
        this.hoursPerWeek = calculateTotalHours(); // Calculate total weekly hours
        this.eligibleFaculty = eligibleFaculty;
    }

    public Course(Long id, String courseCode, String name, String courseType, List<Integer> batchIds,
                  int lectureHours, int theoryHours, int practicalHours, int credits,
                  List<Faculty> eligibleFaculty, List<Long> lectureRoomIDs) {
        this.id = id;
        this.courseCode = courseCode;
        this.name = name;
        this.courseType = courseType;
        this.batchIds = batchIds;
        this.lectureHours = lectureHours;
        this.theoryHours = theoryHours;
        this.practicalHours = practicalHours;
        this.credits = credits;
        this.eligibleFaculty = eligibleFaculty;
        this.lectureRoomIDs = lectureRoomIDs; // New field for minors
    }


    // Calculates total weekly hours
    private int calculateTotalHours() {
        return lectureHours + theoryHours + practicalHours;
    }

    // Getters and Setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public List<Integer> getBatchIds() { return batchIds; }
    public void setBatchIds(List<Integer> batchIds) { this.batchIds = batchIds; }

    public int getLectureHours() { return lectureHours; }
    public void setLectureHours(int lectureHours) {
        this.lectureHours = lectureHours;
        this.hoursPerWeek = calculateTotalHours();
    }

    public int getTheoryHours() { return theoryHours; }
    public void setTheoryHours(int theoryHours) {
        this.theoryHours = theoryHours;
        this.hoursPerWeek = calculateTotalHours();
    }

    public int getPracticalHours() { return practicalHours; }
    public void setPracticalHours(int practicalHours) {
        this.practicalHours = practicalHours;
        this.hoursPerWeek = calculateTotalHours();
    }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public int getHoursPerWeek() { return hoursPerWeek; }

    public List<Faculty> getEligibleFaculty() { return eligibleFaculty; }
    public void setEligibleFaculty(List<Faculty> eligibleFaculty) { this.eligibleFaculty = eligibleFaculty; }

    // Checks if the course involves practical sessions
    public boolean isLabCourse() {
        return this.practicalHours > 0;
    }

    public boolean isMinor() {
        return isMinor;
    }

    public void setMinor(boolean minor) {
        isMinor = minor;
    }

    public List<Long> getLectureRoomIDs() {
        return lectureRoomIDs;
    }

    public void setLectureRoomIDs(List<Long> lectureRoomIDs) {
        this.lectureRoomIDs = lectureRoomIDs;
    }
    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", courseCode='" + courseCode + '\'' +
                ", name='" + name + '\'' +
                ", courseType='" + courseType + '\'' +
                ", batchIds=" + batchIds +
                ", lectureHours=" + lectureHours +
                ", theoryHours=" + theoryHours +
                ", practicalHours=" + practicalHours +
                ", credits=" + credits +
                ", hoursPerWeek=" + hoursPerWeek +
                ", eligibleFaculty=" + eligibleFaculty +
                '}';
    }

}
