package com.timetable.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.List;
import java.util.Objects;

@PlanningSolution
public class TimeTable {
    private Long id;

    @PlanningEntityCollectionProperty
    private List<Lesson> lessonList;

    @ValueRangeProvider(id = "facultyRange")
    @ProblemFactCollectionProperty
    private List<Faculty> facultyList;

    @ValueRangeProvider(id = "roomRange")
    @ProblemFactCollectionProperty
    private List<Room> roomList;

    @ValueRangeProvider(id = "timeSlotRange")
    @ProblemFactCollectionProperty
    private List<TimeSlot> timeSlotList;

    @PlanningScore
    private HardSoftScore score;

    public TimeTable() {}

    public TimeTable(Long id, List<Lesson> lessonList, List<Faculty> facultyList,
                     List<Room> roomList, List<TimeSlot> timeSlotList) {
        this.id = id;
        this.lessonList = lessonList;
        this.facultyList = facultyList;
        this.roomList = roomList;
        this.timeSlotList = timeSlotList;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public List<Lesson> getLessonList() { return lessonList; }
    public void setLessonList(List<Lesson> lessonList) { this.lessonList = lessonList; }
    public List<Faculty> getFacultyList() { return facultyList; }
    public void setFacultyList(List<Faculty> facultyList) { this.facultyList = facultyList; }
    public List<Room> getRoomList() { return roomList; }
    public void setRoomList(List<Room> roomList) { this.roomList = roomList; }
    public List<TimeSlot> getTimeSlotList() { return timeSlotList; }
    public void setTimeSlotList(List<TimeSlot> timeSlotList) { this.timeSlotList = timeSlotList; }
    public HardSoftScore getScore() { return score; }
    public void setScore(HardSoftScore score) { this.score = score; }


    // Overriding equals and hashCode to compare TimeTables by id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeTable)) return false;
        TimeTable timeTable = (TimeTable) o;
        return Objects.equals(id, timeTable.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
