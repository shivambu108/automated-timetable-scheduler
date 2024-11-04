package com.timetable.domain;

import java.time.DayOfWeek;
import java.util.Objects;

public class BatchDayKey {
    private final StudentBatch batch;
    private final DayOfWeek day;

    public BatchDayKey(StudentBatch batch, DayOfWeek day) {
        this.batch = batch;
        this.day = day;
    }

    public StudentBatch getBatch() {
        return batch;
    }

    public DayOfWeek getDay() {
        return day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchDayKey that = (BatchDayKey) o;
        return Objects.equals(batch, that.batch) && day == that.day;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batch, day);
    }
}
