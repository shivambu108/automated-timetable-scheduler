//package com.timetable.domain;
//
//import java.util.Objects;
//
///**
// * Represents a composite key for associating a Course with a StudentBatch in a lab context.
// */
//public class CourseLabKey {
//    private final Course course;
//    private final StudentBatch studentBatch;
//
//    // Constructor to initialize CourseLabKey with a course and a student batch
//    public CourseLabKey(Course course, StudentBatch studentBatch) {
//        this.course = course;
//        this.studentBatch = studentBatch;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true; // Self-check
//        if (o == null || getClass() != o.getClass()) return false; // Null and type check
//        CourseLabKey that = (CourseLabKey) o;
//        return course.equals(that.course) && studentBatch.equals(that.studentBatch); // Field-wise comparison
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(course, studentBatch); // Generate hash based on course and student batch
//    }
//}
