package com.timetable.domain;

import java.util.Objects;

public class CourseLabKey {
    private final Course course;
    private final StudentBatch studentBatch;

    public CourseLabKey(Course course, StudentBatch studentBatch) {
        this.course = course;
        this.studentBatch = studentBatch;
    }

    // Override equals and hashCode for proper grouping
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseLabKey that = (CourseLabKey) o;
        return course.equals(that.course) && studentBatch.equals(that.studentBatch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(course, studentBatch);
    }
}
