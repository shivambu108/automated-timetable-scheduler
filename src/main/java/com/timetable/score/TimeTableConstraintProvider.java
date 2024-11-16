package com.timetable.score;

import com.timetable.domain.*;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;

import static java.util.stream.Collectors.filtering;
import static java.util.stream.Collectors.toList;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.count;
import static org.optaplanner.core.api.score.stream.ConstraintCollectors.sum;

public class TimeTableConstraintProvider implements ConstraintProvider {
    // Constants
    private static final int MIN_CLASSES_PER_BATCH = 20;
    private static final int MAX_CLASSES_PER_BATCH = 25;
    private static final int TARGET_FACULTY_LESSONS = 15;
    private static final LocalTime LUNCH_START = LocalTime.of(13, 15);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 30);
    private static final LocalTime PREFERRED_START_TIME = LocalTime.of(9, 0);
    private static final int TARGET_DAILY_LESSONS_PER_BATCH = 4;
    private static final int ALLOWED_VARIANCE = 2;
    private static final int MAX_GAP_MINUTES = 30;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                // Essential Hard Constraints
                roomConflict(factory),
                teacherConflict(factory),
                studentGroupConflict(factory),
                roomCapacity(factory),
                teacherQualification(factory),

                // Lab-specific Hard Constraints
                weeklyLabScheduling(factory),
                labRoomAssignment(factory),
                onlyOneLabPerBatchPerDay(factory),
                onlyLabCoursesInLabRooms(factory),
                lectureInRegularRooms(factory),
                predefinedRoomAssignment(factory),

                // Time-related Hard Constraints
                noClassesDuringLunchHour(factory),
                singleCoursePerDayForBatch(factory),
                labTimeSlotConstraint(factory),

                // Load Balancing Soft Constraints
                balanceBatchLoad(factory),
                balanceFacultyLoad(factory),
                balanceRoomLoad(factory),

                // Preference and Convenience Soft Constraints
                teacherPreferredTimeslot(factory),
                consecutiveLectures(factory),
                roomStability(factory),
                minimizeRoomChanges(factory),
                preferContiguousLessons(factory),

                // Additional time-related constraints
                preferredStartTime(factory),
                balanceDailyBatchLoad(factory),
                contiguousLessons(factory),
                minimizeGapsInSchedule(factory)
        };
    }

    // Essential Hard Constraints
    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getFaculty),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    private Constraint studentGroupConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student group conflict");
    }

    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getStudentBatch().getStrength() > lesson.getRoom().getCapacity())
                .penalize(HardSoftScore.ONE_HARD,
                        lesson -> lesson.getStudentBatch().getStrength() - lesson.getRoom().getCapacity())
                .asConstraint("Room capacity");
    }

    private Constraint teacherQualification(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getCourse().getEligibleFaculty().contains(lesson.getFaculty()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher qualification");
    }

    // Lab-specific Hard Constraints
    private Constraint weeklyLabScheduling(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse().isLabCourse())
                .groupBy(Lesson::getStudentBatch,
                        ConstraintCollectors.countDistinct(lesson -> lesson.getTimeSlot().getDay()))
                .filter((batch, distinctDays) -> distinctDays < batch.getRequiredLabsPerWeek())
                .penalize(HardSoftScore.ONE_HARD,
                        (batch, distinctDays) -> batch.getRequiredLabsPerWeek() - distinctDays)
                .asConstraint("Weekly lab scheduling");
    }

    private Constraint labRoomAssignment(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse().isLabCourse())
                .filter(lesson -> !isLabRoom(lesson.getRoom()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lab room assignment");
    }

    private Constraint onlyOneLabPerBatchPerDay(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse().isLabCourse())
                .groupBy(Lesson::getStudentBatch,
                        lesson -> lesson.getTimeSlot().getDay(),
                        ConstraintCollectors.count())
                .filter((batch, day, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD,
                        (batch, day, count) -> count - 1)
                .asConstraint("Only one lab per batch per day");
    }

    private Constraint onlyLabCoursesInLabRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getCourse().isLabCourse())
                .filter(lesson -> isLabRoom(lesson.getRoom()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Only lab courses in lab rooms");
    }

    private Constraint lectureInRegularRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getCourse().isLabCourse())
                .filter(lesson -> isLabRoom(lesson.getRoom()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lecture in regular rooms");
    }

    private Constraint predefinedRoomAssignment(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !isRoomAllowedForBatch(lesson.getRoom(), lesson.getStudentBatch()))
                .penalize( HardSoftScore.ONE_HARD)
                .asConstraint("Predefined room assignment");
    }

    private boolean isRoomAllowedForBatch(Room room, StudentBatch batch) {
        if (room == null || batch == null) return false;

        if (room.isLectureRoom()) {
            return batch.getLectureRoomIDs().contains(room.getId());
        } else if (room.isLabRoom()) {
            return batch.getPracticalRoomIDs().contains(room.getId());
        }
        return false;
    }

    // Time-related Hard Constraints
//    private Constraint noClassesDuringLunchHour(ConstraintFactory factory) {
//        return factory.forEach(Lesson.class)
//                .filter(this::isLunchHour)
//                .penalize(HardSoftScore.ONE_HARD)
//                .asConstraint("No classes during lunch hour");
//    }

    private Constraint noClassesDuringLunchHour(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> isLunchHourForYear(lesson))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No classes during lunch hour per year group");
    }

    private boolean isLunchHourForYear(Lesson lesson) {
        // Get the year from the batch
        int year = extractYearFromBatch(lesson.getStudentBatch());

        // Get the lesson's time slot
        LocalTime startTime = lesson.getTimeSlot().getStartTime();

        // Check if the time falls within the lunch period for the corresponding year
        if (year == 2024 || year == 2023) {
            // 13:15-14:30 for 1st and 2nd year
            return startTime.isAfter(LocalTime.of(13, 14)) &&
                    startTime.isBefore(LocalTime.of(14, 31));
        } else if (year == 2022 || year == 2021) {
            // 12:15-13:15 for 3rd and 4th year
            return startTime.isAfter(LocalTime.of(12, 14)) &&
                    startTime.isBefore(LocalTime.of(13, 16));
        }

        return false;
    }

    private int extractYearFromBatch(StudentBatch batch) {
        // Extract year from batch.year
        // Assuming the year is stored as an integer in the Batch class
        return batch.getYear();
    }

//    // Additional helper method if needed to check if a room is assigned to a specific year
//    private boolean isRoomAssignedToYear(Room room, int year) {
//        // Implementation would depend on how rooms are mapped to years in your system
//        // This could check lectureRoomIDs or practicalRoomIDs from your CSV data
//        return room.getBatchYears().contains(year);
//    }

    private Constraint singleCoursePerDayForBatch(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getStudentBatch,
                        lesson -> lesson.getTimeSlot().getDay(),
                        lesson -> lesson.getCourse(),
                        ConstraintCollectors.count())
                .filter((batch, day, course, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD,
                        (batch, day, course, count) -> count - 1)
                .asConstraint("Single course per day for batch");
    }

//    private Constraint labTimeSlotConstraint(ConstraintFactory constraintFactory) {
//        return constraintFactory.from(Lesson.class)
//                .filter(lesson -> "LAB".equals(lesson.getLessonType()))
//                .filter(lesson -> !isLabInCorrectTimeSlot(lesson))
//                .penalize("Lab classes must be scheduled 14:30–16:30", HardSoftScore.ONE_HARD);
//    }

//    private boolean isLabInCorrectTimeSlot(Lesson lesson) {
//        TimeSlot timeSlot = lesson.getTimeSlot();
//        return timeSlot != null
//                && timeSlot.getStartTime().equals(LocalTime.of(14, 30))
//                && timeSlot.getEndTime().equals(LocalTime.of(16, 30));
//    }

    private Constraint labTimeSlotConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .filter(lesson -> "LAB".equals(lesson.getLessonType()))
                .filter(lesson -> !isLabInCorrectTimeSlot(lesson))
                .penalize("Lab classes must be scheduled in designated time slots per batch",
                        HardSoftScore.ONE_HARD);
    }

    private boolean isLabInCorrectTimeSlot(Lesson lesson) {
        TimeSlot timeSlot = lesson.getTimeSlot();
        if (timeSlot == null) return false;

        int batchYear = extractYearFromBatch(lesson.getStudentBatch());
        LocalTime startTime = timeSlot.getStartTime();
        LocalTime endTime = timeSlot.getEndTime();

        // Lab slot 1: 9:00-11:00 (2022 and 2024 batch)
        boolean isSlot1 = startTime.equals(LocalTime.of(9, 0)) &&
                endTime.equals(LocalTime.of(11, 0));

        // Lab slot 2: 11:15-13:15 (2024 batch only)
        boolean isSlot2 = startTime.equals(LocalTime.of(11, 15)) &&
                endTime.equals(LocalTime.of(13, 15));

        // Lab slot 3: 14:30-16:30 (2023 and 2024 batch)
        boolean isSlot3 = startTime.equals(LocalTime.of(14, 30)) &&
                endTime.equals(LocalTime.of(16, 30));

        // Check if the time slot is valid for the specific batch
        switch (batchYear) {
            case 2021:

            case 2022:
                return isSlot1;  // Only 9:00-11:00 allowed

            case 2023:
                return isSlot3;  // Only 14:30-16:30 allowed

            case 2024:
                return isSlot2 || isSlot3;  // Two slots allowed

            default:
                return false;
        }
    }


    // Load Balancing Soft Constraints
    private Constraint balanceBatchLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getStudentBatch, ConstraintCollectors.count())
                .filter((batch, count) -> count < MIN_CLASSES_PER_BATCH || count > MAX_CLASSES_PER_BATCH)
                .penalize(HardSoftScore.ONE_SOFT,
                        (batch, count) -> Math.abs(count - ((MIN_CLASSES_PER_BATCH + MAX_CLASSES_PER_BATCH) / 2)))
                .asConstraint("Balance batch load");
    }

    private Constraint balanceFacultyLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getFaculty, ConstraintCollectors.count())
                .filter((faculty, count) -> count != TARGET_FACULTY_LESSONS)
                .penalize(HardSoftScore.ONE_SOFT,
                        (faculty, count) -> Math.abs(count - TARGET_FACULTY_LESSONS))
                .asConstraint("Balance faculty load");
    }

    private Constraint balanceRoomLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getRoom, ConstraintCollectors.count())
                .filter((room, count) -> count > room.getIdealDailyUsage())
                .penalize(HardSoftScore.ONE_SOFT,
                        (room, count) -> count - room.getIdealDailyUsage())
                .asConstraint("Balance room load");
    }

    // Preference and Convenience Soft Constraints
    private Constraint teacherPreferredTimeslot(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getFaculty().getPreferredSlots().contains(lesson.getTimeSlot()))
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher preferred timeslot");
    }

    private Constraint consecutiveLectures(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) -> isConsecutive(lesson1, lesson2))
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Consecutive lectures");
    }

    private Constraint roomStability(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) -> isConsecutive(lesson1, lesson2))
                .filter((lesson1, lesson2) -> lesson1.getRoom() != lesson2.getRoom())
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Room stability");
    }

    private Constraint minimizeRoomChanges(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) ->
                        Math.abs(ChronoUnit.MINUTES.between(
                                lesson1.getTimeSlot().getEndTime(),
                                lesson2.getTimeSlot().getStartTime())) <= MAX_GAP_MINUTES)
                .filter((lesson1, lesson2) -> lesson1.getRoom() != lesson2.getRoom())
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Minimize room changes");
    }

    private Constraint preferContiguousLessons(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) -> isConsecutive(lesson1, lesson2))
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Prefer contiguous lessons");
    }

    private Constraint preferredStartTime(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getTimeSlot().getStartTime().equals(PREFERRED_START_TIME))
                .penalize(HardSoftScore.ONE_SOFT,
                        lesson -> (int) ChronoUnit.MINUTES.between(
                                PREFERRED_START_TIME,
                                lesson.getTimeSlot().getStartTime()))
                .asConstraint("Preferred start time");
    }

    private Constraint balanceDailyBatchLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getStudentBatch,
                        lesson -> lesson.getTimeSlot().getDay(),
                        ConstraintCollectors.count())
                .filter((batch, day, count) ->
                        Math.abs(count - TARGET_DAILY_LESSONS_PER_BATCH) > ALLOWED_VARIANCE)
                .penalize(HardSoftScore.ONE_SOFT,
                        (batch, day, count) ->
                                Math.abs(count - TARGET_DAILY_LESSONS_PER_BATCH))
                .asConstraint("Balance daily batch load");
    }

    private Constraint contiguousLessons(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) -> !isConsecutive(lesson1, lesson2))
                .filter((lesson1, lesson2) ->
                        Math.abs(ChronoUnit.MINUTES.between(
                                lesson1.getTimeSlot().getEndTime(),
                                lesson2.getTimeSlot().getStartTime())) <= MAX_GAP_MINUTES)
                .penalize(HardSoftScore.ONE_SOFT,
                        (lesson1, lesson2) -> (int) Math.abs(ChronoUnit.MINUTES.between(
                                lesson1.getTimeSlot().getEndTime(),
                                lesson2.getTimeSlot().getStartTime())))
                .asConstraint("Contiguous lessons");
    }

    private Constraint minimizeGapsInSchedule(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
                .filter((lesson1, lesson2) ->
                        ChronoUnit.MINUTES.between(
                                lesson1.getTimeSlot().getEndTime(),
                                lesson2.getTimeSlot().getStartTime()) > MAX_GAP_MINUTES)
                .penalize(HardSoftScore.ONE_SOFT,
                        (lesson1, lesson2) -> (int) ChronoUnit.MINUTES.between(
                                lesson1.getTimeSlot().getEndTime(),
                                lesson2.getTimeSlot().getStartTime()))
                .asConstraint("Minimize gaps in schedule");
    }

    // Utility methods
    private boolean isLabRoom(Room room) {
        return room.getType() == RoomType.COMPUTER_LAB ||
                room.getType() == RoomType.HARDWARE_LAB;
    }

//    private boolean isLunchHour(Lesson lesson) {
//        LocalTime startTime = lesson.getTimeSlot().getStartTime();
//        LocalTime endTime = lesson.getTimeSlot().getEndTime();
//        return (startTime.isAfter(LUNCH_START) || startTime.equals(LUNCH_START)) &&
//                (endTime.isBefore(LUNCH_END) || endTime.equals(LUNCH_END));
//    }

    private boolean isConsecutive(Lesson lesson1, Lesson lesson2) {
        LocalTime endTime1 = lesson1.getTimeSlot().getEndTime();
        LocalTime startTime2 = lesson2.getTimeSlot().getStartTime();
        return endTime1.equals(startTime2) ||
                ChronoUnit.MINUTES.between(endTime1, startTime2) <= 5; // 5-minute buffer
    }

    private LocalTime getStartTime(Lesson lesson) {
        return lesson.getTimeSlot().getStartTime();
    }

    private LocalTime getEndTime(Lesson lesson) {
        return lesson.getTimeSlot().getEndTime();
    }

    private boolean hasTimeGap(Lesson lesson1, Lesson lesson2) {
        return ChronoUnit.MINUTES.between(
                getEndTime(lesson1),
                getStartTime(lesson2)) > MAX_GAP_MINUTES;
    }

    private int calculateTimeGap(Lesson lesson1, Lesson lesson2) {
        return (int) ChronoUnit.MINUTES.between(
                getEndTime(lesson1),
                getStartTime(lesson2));
    }

    private boolean isSameDay(Lesson lesson1, Lesson lesson2) {
        return lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay());
    }

    private boolean isOverlapping(Lesson lesson1, Lesson lesson2) {
        LocalTime start1 = getStartTime(lesson1);
        LocalTime end1 = getEndTime(lesson1);
        LocalTime start2 = getStartTime(lesson2);
        LocalTime end2 = getEndTime(lesson2);

        return (start1.isBefore(end2) || start1.equals(end2)) &&
                (start2.isBefore(end1) || start2.equals(end1));
    }
}