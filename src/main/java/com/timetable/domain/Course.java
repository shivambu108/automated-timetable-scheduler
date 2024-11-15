package com.timetable.domain;

import java.util.List;

public class Course {
    private Long id;
    private String courseCode;
    private String name;
    private String courseType; // "regular" or "elective"
    private List<Integer> batchIds; // Replaced `branch` and `section` with `batchIds`
    private int lectureHours;
    private int theoryHours;
    private int practicalHours; // Previously `hasLab`; now hours for practicals
    private int credits;
    private int hoursPerWeek;
    private List<Faculty> eligibleFaculty;


    public Course(Long id, String courseCode, String name, String courseType, List<Integer> batchIds,
                  int lectureHours, int theoryHours, int practicalHours, int credits, List<Faculty> eligibleFaculty) {
        this.id = id;
        this.courseCode = courseCode;
        this.name = name;
        this.courseType = courseType;
        this.batchIds = batchIds; // Updated to use batchIds
        this.lectureHours = lectureHours;
        this.theoryHours = theoryHours;
        this.practicalHours = practicalHours;
        this.credits = credits;
        this.hoursPerWeek = calculateTotalHours();
        this.eligibleFaculty = eligibleFaculty;
    }

    // Method to calculate total weekly hours
    private int calculateTotalHours() {
        return lectureHours + theoryHours + practicalHours;
    }

    // Getters and Setters for each field
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public List<Integer> getBatchIds() { return batchIds; } // Updated getter
    public void setBatchIds(List<Integer> batchIds) { this.batchIds = batchIds; } // Updated setter

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

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", courseCode='" + courseCode + '\'' +
                ", name='" + name + '\'' +
                ", courseType='" + courseType + '\'' +
                ", batchIds=" + batchIds +  // Updated to display batchIds
                ", lectureHours=" + lectureHours +
                ", theoryHours=" + theoryHours +
                ", practicalHours=" + practicalHours +
                ", credits=" + credits +
                ", hoursPerWeek=" + hoursPerWeek +
                ", eligibleFaculty=" + eligibleFaculty +
                '}';
    }

    public boolean isLabCourse() {
        return this.practicalHours > 0;
    }
}
