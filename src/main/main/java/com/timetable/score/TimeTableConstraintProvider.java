package com.timetable.score;

import com.timetable.domain.*;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.Joiners;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Objects;

import static java.util.Locale.filter;
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
    private static final int ALLOWED_VARIANCE = 1;
    private static final int MAX_GAP_MINUTES = 60;

    private static final LocalTime LUNCH_START_JUNIOR = LocalTime.of(13, 14);
    private static final LocalTime LUNCH_END_JUNIOR = LocalTime.of(14, 31);
    private static final LocalTime LUNCH_START_SENIOR = LocalTime.of(12, 14);
    private static final LocalTime LUNCH_END_SENIOR = LocalTime.of(13, 16);

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
//                teacherPreferredTimeslot(factory),
//                consecutiveLectures(factory),
                roomStability(factory),
                minimizeRoomChanges(factory),
                preferContiguousLessons(factory),

                // Additional time-related constraints
                preferredStartTime(factory),
                balanceDailyBatchLoad(factory),
                contiguousLessons(factory),
                minimizeGapsInSchedule(factory),

                minorValidRoom(factory),
                minorFixedTimeslot(factory),
                noRoomConflictForMinors(factory),

                lectureDurationConstraint(factory),
                labDurationConstraint(factory),
                facultyTimeConflictConstraint(factory),
                facultyMultiBatchConstraint(factory),
//                consecutiveClassConstraint(factory),
                studentBatchConflict(factory),
                batchTimeSlotCompatibility(factory),
//                minorTimeSlotCompatibility(factory),
//                minorCourseDaySpread(factory),
//                minorCourseRoomCompatibility(factory)
                teacherMaxTwoClassesPerDayForBatch(factory),
                labRoomConstraint(factory),
                lectureRoomConstraint(factory),

                batchTimeConflict(factory)

        };
    }

    // Essential Hard Constraints
    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))  // Increased weight
                .asConstraint("Room conflict");
    }

    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getFaculty),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))  // Increased weight
                .asConstraint("Teacher conflict");
    }


    private Constraint studentGroupConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))  // Increased weight
                .asConstraint("Student group conflict");
    }

    private Constraint batchTimeConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachIncludingNullVars(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getTimeSlot().getDay() != null) // Ensure non-null values
                .join(Lesson.class,
                        Joiners.equal(l -> l.getTimeSlot().getDay()), // Safe after filter
                        Joiners.equal(Lesson::getTimeSlot),
                        Joiners.equal(Lesson::getStudentBatch),
                        Joiners.filtering((lesson1, lesson2) -> lesson1 != lesson2)) // Avoid self-join
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Batch Time Conflict");
    }


    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getStudentBatch() != null &&
                        lesson.getRoom() != null &&
                        lesson.getStudentBatch().getStrength() > lesson.getRoom().getCapacity())
                .penalize(HardSoftScore.ONE_HARD.multiply(5),
                        lesson -> (lesson.getStudentBatch().getStrength() - lesson.getRoom().getCapacity()) / 5)  // Scaled penalty
                .asConstraint("Room capacity");
    }


    private Constraint teacherQualification(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getFaculty() != null &&
                        lesson.getCourse() != null &&
                        !lesson.getCourse().getEligibleFaculty().contains(lesson.getFaculty()))
                .penalize(HardSoftScore.ONE_HARD.multiply(8))  // High priority but slightly lower than conflicts
                .asConstraint("Teacher qualification");
    }


    // Lab-specific Hard Constraints
    private Constraint weeklyLabScheduling(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(Lesson::hasValidBatchAndCourse)  // Add this method to Lesson class
                .filter(lesson -> lesson.getCourse().isLabCourse())
                .groupBy(Lesson::getStudentBatch,
                        ConstraintCollectors.countDistinct(lesson -> lesson.getTimeSlot().getDay()))
                .filter((batch, distinctDays) -> batch.getRequiredLabsPerWeek() > distinctDays)
                .penalize(HardSoftScore.ONE_HARD.multiply(10),
                        (batch, distinctDays) -> batch.getRequiredLabsPerWeek() - distinctDays)
                .asConstraint("Weekly lab scheduling");
    }

    private Constraint labRoomAssignment(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null &&
                        lesson.getRoom() != null &&
                        lesson.getCourse().isLabCourse() &&
                        !isLabRoom(lesson.getRoom()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Lab room assignment");
    }

    private Constraint onlyOneLabPerBatchPerDay(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null &&
                        lesson.getCourse().isLabCourse())
                .groupBy(Lesson::getStudentBatch,
                        lesson -> lesson.getTimeSlot().getDay(),
                        ConstraintCollectors.count())
                .filter((batch, day, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD.multiply(10),
                        (batch, day, count) -> count - 1)
                .asConstraint("Only one lab per batch per day");
    }

    private Constraint onlyLabCoursesInLabRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getCourse().isLabCourse())
                .filter(lesson -> isLabRoom(lesson.getRoom()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Only lab courses in lab rooms");
    }

    private Constraint lectureInRegularRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !lesson.getCourse().isLabCourse())
                .filter(lesson -> isLabRoom(lesson.getRoom()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Lecture in regular rooms");
    }

    private Constraint predefinedRoomAssignment(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> !isRoomAllowedForBatch(lesson.getRoom(), lesson.getStudentBatch()))
                .penalize( HardSoftScore.ONE_HARD.multiply(10))
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

    private Constraint noClassesDuringLunchHour(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> isLunchHourForYear(lesson))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("No classes during lunch hour per year group");
    }

    private boolean isLunchHourForYear(Lesson lesson) {
        if (lesson.getStudentBatch() == null) return false;

        int year = lesson.getStudentBatch().getYear();
        LocalTime startTime = lesson.getTimeSlot().getStartTime();

        // Use cached time values for comparison
        return switch (year) {
            case 2024, 2023 -> startTime.isAfter(LUNCH_START_JUNIOR) &&
                    startTime.isBefore(LUNCH_END_JUNIOR);
            case 2022, 2021 -> startTime.isAfter(LUNCH_START_SENIOR) &&
                    startTime.isBefore(LUNCH_END_SENIOR);
            default -> false;
        };
    }

    private Integer  extractYearFromBatch(StudentBatch batch) {
        if (batch == null) {
            return null;
        }
        return batch.getYear();
    }

    private Constraint singleCoursePerDayForBatch(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getStudentBatch,
                        lesson -> lesson.getTimeSlot().getDay(),
                        lesson -> lesson.getCourse(),
                        ConstraintCollectors.count())
                .filter((batch, day, course, count) -> count > 1)
                .penalize(HardSoftScore.ONE_HARD.multiply(10),
                        (batch, day, course, count) -> count - 1)
                .asConstraint("Single course per day for batch");
    }

    private Constraint labTimeSlotConstraint(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .filter(lesson -> "LAB".equals(lesson.getLessonType()))
                .filter(lesson -> !isLabInCorrectTimeSlot(lesson))
                .penalize("Lab classes must be scheduled in designated time slots per batch",
                        HardSoftScore.ONE_HARD.multiply(10));
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

    private Constraint teacherMaxTwoClassesPerDayForBatch(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(
                        Lesson::getFaculty, // Group by teacher
                        Lesson::getStudentBatch, // Group by batch
                        lesson -> lesson.getTimeSlot().getDay(), // Group by day
                        ConstraintCollectors.count() // Count number of lessons
                )
                .filter((teacher, batch, day, classCount) -> classCount > 2) // Filter where count > 2
                .penalize(
                        HardSoftScore.ONE_HARD.multiply(10), // Hard constraint
                        (teacher, batch, day, classCount) -> classCount - 2 // Penalize for every excess class
                )
                .asConstraint("Max two classes per day for a teacher per batch");
    }


    // Load Balancing Soft Constraints
    private Constraint balanceBatchLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getStudentBatch, ConstraintCollectors.count())
                .filter((batch, count) -> count < MIN_CLASSES_PER_BATCH || count > MAX_CLASSES_PER_BATCH)
                .penalize(HardSoftScore.ONE_SOFT.multiply(10),
                        (batch, count) -> Math.abs(count - ((MIN_CLASSES_PER_BATCH + MAX_CLASSES_PER_BATCH) / 2)))
                .asConstraint("Balance batch load");
    }

    private Constraint balanceFacultyLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getFaculty, ConstraintCollectors.count())
                .filter((faculty, count) -> Math.abs(count - TARGET_FACULTY_LESSONS) > 2) // Allow small variance
                .penalize(HardSoftScore.ONE_SOFT.multiply(10),
                        (faculty, count) -> Math.abs(count - TARGET_FACULTY_LESSONS))
                .asConstraint("Balance faculty load");
    }

    private Constraint balanceRoomLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(Lesson::getRoom, ConstraintCollectors.count())
                .filter((room, count) ->
                        count > room.getIdealDailyUsage() ||
                                count < Math.max(1, room.getIdealDailyUsage() - 1)) // Symmetric penalization
                .penalize(HardSoftScore.ONE_SOFT,
                        (room, count) -> Math.abs(count - room.getIdealDailyUsage()))
                .asConstraint("Balance room load");
    }

//    // Preference and Convenience Soft Constraints
//    private Constraint teacherPreferredTimeslot(ConstraintFactory factory) {
//        return factory.forEach(Lesson.class)
//                .filter(lesson -> !lesson.getFaculty().getPreferredSlots().contains(lesson.getTimeSlot()))
//                .penalize(HardSoftScore.ONE_SOFT)
//                .asConstraint("Teacher preferred timeslot");
//    }

//    private Constraint teacherPreferredTimeslot(ConstraintFactory factory) {
//        return factory.forEach(Lesson.class)
//                .filter(lesson -> {
//                    // Null safety checks
//                    Faculty faculty = lesson.getFaculty();
//                    TimeSlot timeSlot = lesson.getTimeSlot();
//                    if (faculty == null || timeSlot == null) return false;
//
//                    List<TimeSlot> preferredSlots = faculty.getPreferredSlots();
//                    return preferredSlots != null && !preferredSlots.contains(timeSlot);
//                })
//                .penalize(HardSoftScore.ONE_SOFT)
//                .asConstraint("Teacher preferred timeslot");
//    }

//    private Constraint consecutiveLectures(ConstraintFactory factory) {
//        return factory.forEachUniquePair(Lesson.class,
//                        Joiners.equal(Lesson::getStudentBatch),
//                        Joiners.equal(lesson -> lesson.getTimeSlot().getDay()))
//                .filter((lesson1, lesson2) -> isConsecutive(lesson1, lesson2))
//                .reward(HardSoftScore.ONE_SOFT)
//                .asConstraint("Consecutive lectures");
//    }

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
                        lesson -> (int) Math.abs(ChronoUnit.MINUTES.between(
                                PREFERRED_START_TIME,
                                lesson.getTimeSlot().getStartTime())))
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

    private boolean isConsecutive(Lesson lesson1, Lesson lesson2) {
        LocalTime endTime1 = lesson1.getTimeSlot().getEndTime();
        LocalTime startTime2 = lesson2.getTimeSlot().getStartTime();
        return endTime1.equals(startTime2) ||
                ChronoUnit.MINUTES.between(endTime1, startTime2) <= 5; // 5-minute buffer
    }

    private Constraint minorValidRoom(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .filter(lesson -> lesson.getCourse().getCourseType().equals("minor"))
                .filter(lesson -> !lesson.getCourse().getLectureRoomIDs().contains(lesson.getRoom().getId()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Minors must be assigned to valid rooms");
    }

    private Constraint minorFixedTimeslot(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .filter(lesson -> lesson.getCourse().getCourseType().equals("minor"))
                .filter(lesson -> !lesson.getTimeSlot().getStartTime().equals(LocalTime.of(18, 0)))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Minor courses must be scheduled at 6:00 PM");
    }

    private Constraint noRoomConflictForMinors(ConstraintFactory constraintFactory) {
        return constraintFactory.from(Lesson.class)
                .filter(lesson -> lesson.getCourse().getCourseType().equals("minor"))
                .join(Lesson.class,
                        Joiners.equal(Lesson::getRoom),
                        Joiners.equal(Lesson::getTimeSlot))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("No room conflicts for minors");
    }

    private Constraint lectureDurationConstraint(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getLessonType().equals("LECTURE") &&
                        lesson.getTimeSlot().getDurationInMinutes() > 90)
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Lecture classes should not be in 2-hour slots");
    }

    private Constraint labDurationConstraint(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getLessonType().equals("LAB") &&
                        lesson.getTimeSlot().getDurationInMinutes() != 120)
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Lab classes must be scheduled in 2-hour slots");
    }

    private Constraint labRoomConstraint(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null &&
                        lesson.getTimeSlot().getDurationInMinutes() == 120)
                .filter(lesson -> !"LAB".equals(lesson.getLessonType()) ||
                        !lesson.getStudentBatch().getPracticalRoomIDs().contains(lesson.getRoom().getId()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Two hour slots must be LAB type in practical rooms");
    }

    private Constraint lectureRoomConstraint(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null &&
                        lesson.getTimeSlot().getDurationInMinutes() < 120)
                .filter(lesson -> !"LECTURE".equals(lesson.getLessonType()) ||
                        !lesson.getStudentBatch().getLectureRoomIDs().contains(lesson.getRoom().getId()))
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Sessions under 2 hours must be LECTURE type in lecture rooms");
    }

    private Constraint facultyTimeConflictConstraint(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getFaculty),
                        Joiners.equal(l -> l.getTimeSlot() != null ? l.getTimeSlot().getDay() : null))
                .filter((lesson1, lesson2) -> {
                    // Comprehensive null checks
                    if (lesson1 == null || lesson2 == null ||
                            lesson1.getFaculty() == null || lesson2.getFaculty() == null ||
                            lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) {
                        return false;
                    }

                    return isTimeSlotOverlapping(lesson1.getTimeSlot(), lesson2.getTimeSlot()) ||
                            isInterwokenTimeSlot(lesson1.getTimeSlot(), lesson2.getTimeSlot()) ||
                            isInsufficientBreakBetweenClasses(lesson1.getTimeSlot(), lesson2.getTimeSlot());
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Faculty Time Conflict");
    }

    private Constraint batchTimeConflictConstraint(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getFaculty),
                        Joiners.equal(l -> l.getTimeSlot() != null ? l.getTimeSlot().getDay() : null))
                .filter((lesson1, lesson2) -> {
                    // Comprehensive null checks
                    if (lesson1 == null || lesson2 == null ||
                            lesson1.getFaculty() == null || lesson2.getFaculty() == null ||
                            lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null) {
                        return false;
                    }

                    return isTimeSlotOverlapping(lesson1.getTimeSlot(), lesson2.getTimeSlot()) ||
                            isInterwokenTimeSlot(lesson1.getTimeSlot(), lesson2.getTimeSlot()) ||
                            isInsufficientBreakBetweenClasses(lesson1.getTimeSlot(), lesson2.getTimeSlot());
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Faculty Time Conflict");
    }

    private Constraint facultyMultiBatchConstraint(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getFaculty),
                        Joiners.equal(l -> l.getTimeSlot() != null ? l.getTimeSlot().getDay() : null))
                .filter((lesson1, lesson2) -> {
                    // Consolidated null and conflict checks
                    return lesson1.getStudentBatch() != null &&
                            lesson2.getStudentBatch() != null &&
                            lesson1.getFaculty() != null &&
                            lesson2.getFaculty() != null &&
                            lesson1.getTimeSlot() != null &&
                            lesson2.getTimeSlot() != null &&
                            !lesson1.equals(lesson2) &&
                            !lesson1.getStudentBatch().equals(lesson2.getStudentBatch());
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Faculty Teaching Multiple Batches Simultaneously");
    }



//    private Constraint consecutiveClassConstraint(ConstraintFactory factory) {
//        return factory.forEachUniquePair(Lesson.class,
//                        Joiners.equal(l -> l.getCourse()),
//                        Joiners.equal(l -> l.getStudentBatch()),
//                        Joiners.equal(l -> l.getTimeSlot() != null ? l.getTimeSlot().getDay() : null))
//                .filter((lesson1, lesson2) -> {
//                    // Comprehensive null and time difference checks
//                    if (lesson1 == null || lesson2 == null ||
//                            lesson1.getTimeSlot() == null || lesson2.getTimeSlot() == null ||
//                            lesson1.getStudentBatch() == null || lesson2.getStudentBatch() == null ||
//                            lesson1.getCourse() == null || lesson2.getCourse() == null) {
//                        return false;
//                    }
//
//                    // Ensure time slots are different
//                    if (lesson1.getTimeSlot().equals(lesson2.getTimeSlot())) {
//                        return false;
//                    }
//
//                    // Optimize time difference calculation
//                    long timeDifference = Math.abs(Duration.between(
//                            lesson1.getTimeSlot().getEndTime(),
//                            lesson2.getTimeSlot().getStartTime()
//                    ).toMinutes());
//
//                    // Penalize if time between same subject classes is more than 1.5 hours
//                    return timeDifference > 90;
//                })
//                .penalize(HardSoftScore.ONE_SOFT)
//                .asConstraint("Consecutive Subject Class Spacing");
//    }






    // Helper Methods
    private boolean isTimeSlotOverlapping(TimeSlot slot1, TimeSlot slot2) {
        if (slot1 == null || slot2 == null) return false;
        return !(slot1.getEndTime().isBefore(slot2.getStartTime()) ||
                slot1.getStartTime().isAfter(slot2.getEndTime()));
    }

    private boolean isInterwokenTimeSlot(TimeSlot slot1, TimeSlot slot2) {
        LocalTime start1 = slot1.getStartTime();
        LocalTime end1 = slot1.getEndTime();
        LocalTime start2 = slot2.getStartTime();
        LocalTime end2 = slot2.getEndTime();

        return (start2.isAfter(start1) && start2.isBefore(end1)) ||
                (end2.isAfter(start1) && end2.isBefore(end1)) ||
                (start1.isAfter(start2) && start1.isBefore(end2)) ||
                (end1.isAfter(start2) && end1.isBefore(end2));
    }

    private boolean isInsufficientBreakBetweenClasses(TimeSlot slot1, TimeSlot slot2) {
        Duration timeBetweenClasses = Duration.between(
                slot1.getEndTime(),
                slot2.getStartTime()
        );
        return Math.abs(timeBetweenClasses.toMinutes()) < 15;
    }

    private Constraint studentBatchConflict(ConstraintFactory factory) {
        return factory.forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentBatch))
                .filter((lesson1, lesson2) -> {
                    // Null safety and day check
                    TimeSlot slot1 = lesson1.getTimeSlot();
                    TimeSlot slot2 = lesson2.getTimeSlot();

                    return slot1 != null &&
                            slot2 != null &&
                            slot1.getDay().equals(slot2.getDay()) &&
                            (isTimeSlotOverlapping(slot1, slot2) ||
                                    isInterwokenTimeSlot(slot1, slot2));
                })
                .penalize(HardSoftScore.ONE_HARD.multiply(10))
                .asConstraint("Student batch time conflict");
    }

    // Constraint to ensure minor courses are only in minor time slots
    private Constraint minorTimeSlotCompatibility(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson ->
                        "MINOR".equals(lesson.getLessonType()) &&
                                lesson.getTimeSlot() != null &&
                                !isValidMinorTimeSlot(lesson.getTimeSlot())
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("MinorTimeSlotCompatibility");
    }

    // Constraint to spread minor courses across different days
    private Constraint minorCourseDaySpread(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(Lesson.class)
                .filter((lesson1, lesson2) ->
                        "MINOR".equals(lesson1.getLessonType()) &&
                                "MINOR".equals(lesson2.getLessonType()) &&
                                lesson1.getCourse().equals(lesson2.getCourse()) &&
                                lesson1.getTimeSlot() != null &&
                                lesson2.getTimeSlot() != null &&
                                lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay())
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("MinorCourseDaySpread");
    }

    // Constraint to ensure minor courses use designated rooms
    private Constraint minorCourseRoomCompatibility(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson ->
                        "MINOR".equals(lesson.getLessonType()) &&
                                lesson.getRoom() != null &&
                                !isValidMinorRoom(lesson)
                )
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("MinorCourseRoomCompatibility");
    }

    // Original batch time slot compatibility constraint
    private Constraint batchTimeSlotCompatibility(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson ->
                        !"MINOR".equals(lesson.getLessonType()) &&
                                lesson.getTimeSlot() != null &&
                                lesson.getStudentBatch() != null &&
                                !isTimeSlotValidForBatch(lesson.getStudentBatch(), lesson.getTimeSlot())
                )
                .penalize(HardSoftScore.ONE_HARD.multiply(100))
                .asConstraint("BatchTimeSlotCompatibility");
    }

    // Helper method to check if a time slot is valid for minor courses
    private boolean isValidMinorTimeSlot(TimeSlot slot) {
        return slot != null &&
                ((slot.getStartTime().equals(LocalTime.of(8, 0)) &&
                        slot.getEndTime().equals(LocalTime.of(9, 0)) &&
                        "MINOR".equals(slot.getSlotType())) ||
                        (slot.getStartTime().equals(LocalTime.of(18, 0)) &&
                                slot.getEndTime().equals(LocalTime.of(19, 30)) &&
                                "MINOR".equals(slot.getSlotType())));
    }

    // Helper method to check if a room is valid for a minor course
    private boolean isValidMinorRoom(Lesson lesson) {
        return lesson.getCourse() != null &&
                lesson.getRoom() != null &&
                lesson.getCourse().getLectureRoomIDs().contains(lesson.getRoom().getId());
    }

    // Original time slot validation for regular batches
    private boolean isTimeSlotValidForBatch(StudentBatch batch, TimeSlot slot) {
        if (slot.getSlotType().equals("MINOR")) {
            return false;  // Regular batches should not get minor slots
        }

        int batchYear = batch.getYear();
        LocalTime startTime = slot.getStartTime();
        LocalTime endTime = slot.getEndTime();
        String slotType = slot.getSlotType();

        switch (batchYear) {
            case 2021: return isValidTimeSlotForYear4(startTime, endTime, slotType);
            case 2022: return isValidTimeSlotForYear3(startTime, endTime, slotType);
            case 2023: return isValidTimeSlotForYear2(startTime, endTime, slotType);
            case 2024: return isValidTimeSlotForYear1(startTime, endTime, slotType);
            default: return false;
        }
    }

    // Existing time slot validation methods remain the same
    private boolean isValidTimeSlotForYear4(LocalTime startTime, LocalTime endTime, String slotType) {
        return (startTime.equals(LocalTime.of(9, 0)) && endTime.equals(LocalTime.of(10, 30)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(13, 30)) && endTime.equals(LocalTime.of(14, 30)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(14, 45)) && endTime.equals(LocalTime.of(16, 15)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(16, 30)) && endTime.equals(LocalTime.of(18, 0)) && slotType.equals("LECTURE"));
    }

    // Other year-specific validation methods remain unchanged...
    private boolean isValidTimeSlotForYear3(LocalTime startTime, LocalTime endTime, String slotType) {
        return (startTime.equals(LocalTime.of(9, 0)) && endTime.equals(LocalTime.of(10, 30)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(11, 15)) && endTime.equals(LocalTime.of(12, 15)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(13, 30)) && endTime.equals(LocalTime.of(15, 0)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(15, 15)) && endTime.equals(LocalTime.of(16, 45)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(17, 0)) && endTime.equals(LocalTime.of(18, 0)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(9, 0)) && endTime.equals(LocalTime.of(11, 0)) && slotType.equals("LAB"));
    }

    private boolean isValidTimeSlotForYear2(LocalTime startTime, LocalTime endTime, String slotType) {
        return (startTime.equals(LocalTime.of(9, 0)) && endTime.equals(LocalTime.of(10, 30)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(10, 45)) && endTime.equals(LocalTime.of(12, 15)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(12, 15)) && endTime.equals(LocalTime.of(13, 15)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(14, 30)) && endTime.equals(LocalTime.of(16, 0)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(14, 30)) && endTime.equals(LocalTime.of(16, 30)) && slotType.equals("LAB"));
    }

    private boolean isValidTimeSlotForYear1(LocalTime startTime, LocalTime endTime, String slotType) {
        return (startTime.equals(LocalTime.of(9, 0)) && endTime.equals(LocalTime.of(10, 30)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(10, 45)) && endTime.equals(LocalTime.of(12, 15)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(12, 15)) && endTime.equals(LocalTime.of(13, 15)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(14, 30)) && endTime.equals(LocalTime.of(16, 0)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(16, 15)) && endTime.equals(LocalTime.of(17, 45)) && slotType.equals("LECTURE")) ||
                (startTime.equals(LocalTime.of(11, 15)) && endTime.equals(LocalTime.of(13, 15)) && slotType.equals("LAB")) ||
                (startTime.equals(LocalTime.of(14, 30)) && endTime.equals(LocalTime.of(16, 30)) && slotType.equals("LAB"));
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