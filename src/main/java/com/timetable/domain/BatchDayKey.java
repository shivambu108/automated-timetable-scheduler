//package com.timetable.domain;
//
//import java.time.DayOfWeek;
//import java.util.Objects;
//
///**
// * Represents a composite key for a StudentBatch and a specific day of the week.
// * Used for identifying batch-specific schedules.
// */
//public class BatchDayKey {
//    private final StudentBatch batch;
//    private final DayOfWeek day;
//
//    // Constructor to initialize BatchDayKey with a batch and day
//    public BatchDayKey(StudentBatch batch, DayOfWeek day) {
//        this.batch = batch;
//        this.day = day;
//    }
//
//    public StudentBatch getBatch() {
//        return batch;
//    }
//
//    public DayOfWeek getDay() {
//        return day;
//    }
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true; // Self-check
//        if (o == null || getClass() != o.getClass()) return false; // Null and class type check
//        BatchDayKey that = (BatchDayKey) o;
//        return Objects.equals(batch, that.batch) && day == that.day; // Field-wise comparison
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(batch, day); // Generate hash based on batch and day
//    }
//}
