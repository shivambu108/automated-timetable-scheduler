package com.timetable.score;

import com.timetable.domain.*;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import static org.optaplanner.core.api.score.stream.ConstraintCollectors.count;

public class TimeTableConstraintProvider implements ConstraintProvider {
    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                roomConflict(factory), teacherConflict(factory), studentGroupConflict(factory),
                roomCapacity(factory), teacherQualification(factory), singleCoursePerDayForBatch(factory),
                noClassesDuringLunchHour(factory), onlyOneLabPerCoursePerWeek(factory),
                onlyOneLabPerBatchPerDay(factory), onlyLabCoursesInLabRooms(factory),
                lectureInRegularRooms(factory), teacherPreferredTimeslot(factory),
                consecutiveLectures(factory), roomStability(factory),
                balanceFacultyLoad(factory), balanceRoomLoad(factory), balanceBatchLoad(factory)
        };
    }

    // Hard Constraints
    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                        lesson1.getRoom().equals(lesson2.getRoom()) &&
                        !lesson1.equals(lesson2))
                .penalize("Room conflict", HardSoftScore.ONE_HARD);
    }

    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getFaculty() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                        lesson1.getFaculty().equals(lesson2.getFaculty()) &&
                        !lesson1.equals(lesson2))
                .penalize("Teacher conflict", HardSoftScore.ONE_HARD);
    }

    private Constraint studentGroupConflict(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getStudentBatch() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                        lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                        !lesson1.equals(lesson2))
                .penalize("Student group conflict", HardSoftScore.ONE_HARD);
    }

    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getRoom() != null && lesson.getStudentBatch() != null &&
                        lesson.getStudentBatch().getStrength() > lesson.getRoom().getCapacity())
                .penalize("Room capacity", HardSoftScore.ONE_HARD,
                        lesson -> lesson.getStudentBatch().getStrength() - lesson.getRoom().getCapacity());
    }

    private Constraint teacherQualification(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getFaculty() != null && lesson.getCourse() != null &&
                        !lesson.getCourse().getEligibleFaculty().contains(lesson.getFaculty()))
                .penalize("Teacher qualification", HardSoftScore.ONE_HARD);
    }

    private Constraint singleCoursePerDayForBatch(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                        lesson1.getCourse().equals(lesson2.getCourse()) &&
                        lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                        !lesson1.equals(lesson2))
                .penalize("Single course per day for batch", HardSoftScore.ONE_HARD);
    }

    private Constraint noClassesDuringLunchHour(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null &&
                        lesson.getTimeSlot().getStartTime().equals(LocalTime.of(12, 0)))
                .penalize("No classes during lunch hour", HardSoftScore.ONE_HARD);
    }

    private Constraint onlyOneLabPerCoursePerWeek(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().isLabCourse() &&
                        lesson.getRoom() != null &&
                        (lesson.getRoom().getType() == RoomType.COMPUTER_LAB ||
                                lesson.getRoom().getType() == RoomType.HARDWARE_LAB) &&
                        lesson.getTimeSlot() != null)
                .groupBy(lesson -> new CourseLabKey(lesson.getCourse(), lesson.getStudentBatch()), count())
                .filter((courseLabKey, count) -> count > 1)
                .penalize("Only one lab per course per week", HardSoftScore.ONE_HARD,
                        (courseLabKey, count) -> count - 1);
    }

    private Constraint onlyOneLabPerBatchPerDay(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().isLabCourse() && // Updated method call to isLabCourse
                        lesson.getRoom() != null &&
                        (lesson.getRoom().getType() == RoomType.COMPUTER_LAB ||
                                lesson.getRoom().getType() == RoomType.HARDWARE_LAB)) // Ensured to check for both lab types
                .groupBy(lesson -> new BatchDayKey(lesson.getStudentBatch(),
                        DayOfWeek.valueOf(lesson.getTimeSlot().getDay().toUpperCase())), count()) // Convert String to DayOfWeek
                .filter((batchDayKey, count) -> count > 1)
                .penalize("Only one lab per batch per day", HardSoftScore.ONE_HARD,
                        (batchDayKey, count) -> count - 1);
    }

    //important location



    private Constraint onlyLabCoursesInLabRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null && lesson.getCourse().isLabCourse() &&
                        lesson.getRoom() != null &&
                        !(lesson.getRoom().getType() == RoomType.COMPUTER_LAB ||
                                lesson.getRoom().getType() == RoomType.HARDWARE_LAB))
                .penalize("Lab courses must be in lab rooms", HardSoftScore.ONE_HARD);
    }


    /**
     * New Constraint: Ensure lecture classes are in regular rooms.
     */
    private Constraint lectureInRegularRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null && !lesson.getCourse().isLabCourse() &&
                        lesson.getRoom() != null &&
                        (lesson.getRoom().getType() == RoomType.COMPUTER_LAB ||
                                lesson.getRoom().getType() == RoomType.HARDWARE_LAB))
                .penalize("Lecture classes must not be in lab rooms", HardSoftScore.ONE_HARD);
    }


    // Soft Constraints
    private Constraint teacherPreferredTimeslot(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getFaculty() != null && lesson.getFaculty().getPreferredSlots() != null &&
                        !lesson.getFaculty().getPreferredSlots().contains(lesson.getTimeSlot()))
                .penalize("Teacher preferred timeslot", HardSoftScore.ONE_SOFT, lesson -> 1);
    }

    private Constraint consecutiveLectures(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getStudentBatch() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                        lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                        Math.abs(ChronoUnit.HOURS.between(
                                lesson1.getTimeSlot().getStartTime(),
                                lesson2.getTimeSlot().getStartTime())) == 1)
                .reward("Consecutive lectures", HardSoftScore.ONE_SOFT);
    }

    private Constraint roomStability(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null &&
                        lesson.getStudentBatch() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                        lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                        !lesson1.getRoom().equals(lesson2.getRoom()) &&
                        !lesson1.equals(lesson2))
                .penalize("Room stability", HardSoftScore.ONE_SOFT);
    }

    private Constraint balanceFacultyLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getFaculty, count())
                .penalize("Balance faculty load", HardSoftScore.ONE_SOFT,
                        (faculty, lessonCount) -> Math.abs(lessonCount - 5)); // Example target is 5 lessons/week per faculty
    }

    private Constraint balanceRoomLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getRoom, count())
                .penalize("Balance room load", HardSoftScore.ONE_SOFT,
                        (room, lessonCount) -> Math.abs(lessonCount - 15)); // Example target is 15 lessons/week per room
    }

    private Constraint balanceBatchLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getStudentBatch, count())
                .penalize("Balance batch load", HardSoftScore.ONE_SOFT,
                        (batch, lessonCount) -> Math.abs(lessonCount - 30)); // Example target is 30 lessons/week per batch
    }
}
