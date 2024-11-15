package com.timetable.score;

import com.timetable.domain.*;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class TimeTableConstraintProvider implements ConstraintProvider {

    // Constants for configuration
    private static final int MIN_CLASSES_PER_BATCH = 20;
    private static final int MAX_CLASSES_PER_BATCH = 25;
    private static final int TARGET_FACULTY_LESSONS = 15;
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    private static final LocalTime PREFERRED_START_TIME = LocalTime.of(9, 0);
    private static final int TARGET_DAILY_LESSONS_PER_BATCH = 4; // Target number of lessons per day per batch
    private static final int ALLOWED_VARIANCE = 2; // Allowed variance in daily lessons between batches


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

                // Time-related Hard Constraints
                noClassesDuringLunchHour(factory),
                singleCoursePerDayForBatch(factory),

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

                // New and modified time-related constraints
                preferredStartTime(factory),
                balanceDailyBatchLoad(factory),
                contiguousLessons(factory),
                minimizeGapsInSchedule(factory)
        };
    }

    // Hard Constraints

    private Constraint roomConflict(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getRoom() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                            lesson1.getRoom().equals(lesson2.getRoom()) &&
                            !lesson1.equals(lesson2);
                })
                .penalize("Room conflict", HardSoftScore.ONE_HARD);
    }

    private Constraint teacherConflict(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getFaculty() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                            lesson1.getFaculty().equals(lesson2.getFaculty()) &&
                            !lesson1.equals(lesson2);
                })
                .penalize("Teacher conflict", HardSoftScore.ONE_HARD);
    }

    private Constraint studentGroupConflict(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null && lesson.getStudentBatch() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getTimeSlot().equals(lesson2.getTimeSlot()) &&
                            lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                            !lesson1.equals(lesson2);
                })
                .penalize("Student group conflict", HardSoftScore.ONE_HARD);
    }

    private Constraint roomCapacity(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> {
                    return lesson.getRoom() != null &&
                            lesson.getStudentBatch() != null &&
                            lesson.getStudentBatch().getStrength() > lesson.getRoom().getCapacity();
                })
                .penalize("Room capacity", HardSoftScore.ONE_HARD,
                        lesson -> lesson.getStudentBatch().getStrength() - lesson.getRoom().getCapacity());
    }

    private Constraint teacherQualification(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> {
                    return lesson.getFaculty() != null &&
                            lesson.getCourse() != null &&
                            !lesson.getCourse().getEligibleFaculty().contains(lesson.getFaculty());
                })
                .penalize("Teacher qualification", HardSoftScore.ONE_HARD);
    }

    // Lab-specific Constraints

    private Constraint weeklyLabScheduling(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null &&
                        lesson.getCourse().isLabCourse() &&
                        lesson.getTimeSlot() != null)
                .groupBy(
                        lesson -> lesson.getCourse(),
                        lesson -> lesson.getStudentBatch(),
                        ConstraintCollectors.count()
                )
                .filter((course, batch, count) -> count != 1)
                .penalize("Weekly lab scheduling",
                        HardSoftScore.ONE_HARD,
                        (course, batch, count) -> Math.abs(1 - count) * 10);
    }

    private Constraint labRoomAssignment(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null &&
                        lesson.getCourse().isLabCourse() &&
                        lesson.getRoom() != null)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getCourse().equals(lesson2.getCourse()) &&
                            lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                            !lesson1.equals(lesson2) &&
                            !lesson1.getRoom().equals(lesson2.getRoom());
                })
                .penalize("Consistent lab room assignment", HardSoftScore.ONE_HARD);
    }

    private Constraint onlyOneLabPerBatchPerDay(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getCourse() != null &&
                        lesson.getCourse().isLabCourse() &&
                        lesson.getTimeSlot() != null)
                .groupBy(
                        lesson -> lesson.getStudentBatch(),
                        lesson -> lesson.getTimeSlot().getDay(),
                        ConstraintCollectors.count()
                )
                .filter((batch, day, count) -> count > 1)
                .penalize("Only one lab per batch per day",
                        HardSoftScore.ONE_HARD,
                        (batch, day, count) -> (count - 1) * 5);
    }

    private Constraint onlyLabCoursesInLabRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> {
                    return lesson.getCourse() != null &&
                            lesson.getCourse().isLabCourse() &&
                            lesson.getRoom() != null &&
                            lesson.getRoom().getType() != RoomType.COMPUTER_LAB &&
                            lesson.getRoom().getType() != RoomType.HARDWARE_LAB;
                })
                .penalize("Lab courses must be in lab rooms", HardSoftScore.ONE_HARD);
    }

    private Constraint lectureInRegularRooms(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> {
                    return lesson.getCourse() != null &&
                            !lesson.getCourse().isLabCourse() &&
                            lesson.getRoom() != null &&
                            (lesson.getRoom().getType() == RoomType.COMPUTER_LAB ||
                                    lesson.getRoom().getType() == RoomType.HARDWARE_LAB);
                })
                .penalize("Lectures must be in regular rooms", HardSoftScore.ONE_HARD);
    }

    // Time-related Constraints

    private Constraint noClassesDuringLunchHour(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> {
                    LocalTime startTime = lesson.getTimeSlot().getStartTime();
                    return !startTime.isBefore(LUNCH_START) &&
                            !startTime.isAfter(LUNCH_END);
                })
                .penalize("No classes during lunch hour", HardSoftScore.ONE_HARD);
    }

    private Constraint singleCoursePerDayForBatch(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                            lesson1.getCourse().equals(lesson2.getCourse()) &&
                            lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                            !lesson1.equals(lesson2);
                })
                .penalize("Single course per day for batch", HardSoftScore.ONE_HARD);
    }

    // Load Balancing Constraints

    private Constraint balanceBatchLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(
                        lesson -> lesson.getStudentBatch(),
                        ConstraintCollectors.count()
                )
                .filter((batch, count) -> count < MIN_CLASSES_PER_BATCH ||
                        count > MAX_CLASSES_PER_BATCH)
                .penalize("Balance batch load",
                        HardSoftScore.ONE_SOFT,
                        (batch, count) -> {
                            if (count < MIN_CLASSES_PER_BATCH) {
                                return (MIN_CLASSES_PER_BATCH - count) * 2;
                            }
                            return (count - MAX_CLASSES_PER_BATCH) * 2;
                        });
    }

    private Constraint balanceFacultyLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(
                        lesson -> lesson.getFaculty(),
                        ConstraintCollectors.count()
                )
                .penalize("Balance faculty load",
                        HardSoftScore.ONE_SOFT,
                        (faculty, count) -> Math.abs(count - TARGET_FACULTY_LESSONS));
    }

    private Constraint balanceRoomLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(
                        lesson -> lesson.getRoom(),
                        ConstraintCollectors.count()
                )
                .penalize("Balance room load",
                        HardSoftScore.ONE_SOFT,
                        (room, count) -> Math.abs(count - 20));
    }

    // Soft Constraints

    private Constraint teacherPreferredTimeslot(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> {
                    return lesson.getFaculty() != null &&
                            lesson.getFaculty().getPreferredSlots() != null &&
                            !lesson.getFaculty().getPreferredSlots().contains(lesson.getTimeSlot());
                })
                .penalize("Teacher preferred timeslot", HardSoftScore.ONE_SOFT);
    }

    private Constraint consecutiveLectures(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    if (!lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) ||
                            !lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay())) {
                        return false;
                    }
                    long timeDiff = ChronoUnit.HOURS.between(
                            lesson1.getTimeSlot().getStartTime(),
                            lesson2.getTimeSlot().getStartTime()
                    );
                    return Math.abs(timeDiff) == 1;
                })
                .reward("Consecutive lectures", HardSoftScore.ONE_SOFT);
    }

    private Constraint roomStability(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                            lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                            !lesson1.getRoom().equals(lesson2.getRoom()) &&
                            !lesson1.equals(lesson2);
                })
                .penalize("Room stability", HardSoftScore.ONE_SOFT);
    }

    private Constraint minimizeRoomChanges(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                            lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                            Math.abs(ChronoUnit.HOURS.between(
                                    lesson1.getTimeSlot().getStartTime(),
                                    lesson2.getTimeSlot().getStartTime()
                            )) == 1 &&
                            !lesson1.getRoom().equals(lesson2.getRoom());
                })
                .penalize("Minimize room changes between consecutive lessons",
                        HardSoftScore.ONE_SOFT);
    }

    private Constraint preferContiguousLessons(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(
                        lesson -> lesson.getStudentBatch(),
                        lesson -> lesson.getTimeSlot().getDay(),
                        ConstraintCollectors.count()
                )
                .filter((batch, day, count) -> count > 1)
                .reward("Prefer contiguous lessons",
                        HardSoftScore.ONE_SOFT,
                        (batch, day, count) -> count - 1);
    }

    // New constraint to encourage 9 AM start time
    private Constraint preferredStartTime(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeSlot() != null)
                .groupBy(
                        lesson -> lesson.getStudentBatch(),
                        lesson -> lesson.getTimeSlot().getDay(),
                        ConstraintCollectors.min((Lesson lesson) ->
                                lesson.getTimeSlot().getStartTime())
                )
                .filter((batch, day, firstLessonTime) ->
                        !firstLessonTime.equals(PREFERRED_START_TIME))
                .penalize("Should start at 9 AM",
                        HardSoftScore.ONE_HARD,
                        (batch, day, firstLessonTime) -> 1);
    }

    // New constraint to balance daily load across batches
    private Constraint balanceDailyBatchLoad(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .groupBy(
                        lesson -> lesson.getTimeSlot().getDay(), // Group by day
                        lesson -> lesson.getStudentBatch(),     // Group by batch
                        ConstraintCollectors.count()            // Count lessons per batch per day
                )
                .groupBy(
                        (day, batchCount) -> day,               // Group by day
                        ConstraintCollectors.min((batchCount) -> batchCount), // Minimum batch count for the day
                        ConstraintCollectors.max((batchCount) -> batchCount)  // Maximum batch count for the day
                )
                .filter((day, minCount, maxCount) ->
                        (maxCount - minCount) > ALLOWED_VARIANCE) // Apply allowed variance condition
                .penalize("Balance daily batch load",
                        HardSoftScore.ONE_HARD,
                        (day, minCount, maxCount) ->
                                (maxCount - minCount - ALLOWED_VARIANCE) * 5);
    }


    // Enhanced constraint for contiguous lessons
    private Constraint contiguousLessons(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    return lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) &&
                            lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay()) &&
                            !lesson1.equals(lesson2);
                })
                .filter((lesson1, lesson2) -> {
                    LocalTime time1 = lesson1.getTimeSlot().getStartTime();
                    LocalTime time2 = lesson2.getTimeSlot().getStartTime();
                    return !time1.plusHours(1).equals(time2) &&
                            !time2.plusHours(1).equals(time1);
                })
                .penalize("Lessons should be contiguous",
                        HardSoftScore.ONE_SOFT,
                        (lesson1, lesson2) -> 2);
    }

    // New constraint to minimize gaps in schedule
    private Constraint minimizeGapsInSchedule(ConstraintFactory factory) {
        return factory.forEach(Lesson.class)
                .join(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    if (!lesson1.getStudentBatch().equals(lesson2.getStudentBatch()) ||
                            !lesson1.getTimeSlot().getDay().equals(lesson2.getTimeSlot().getDay())) {
                        return false;
                    }
                    LocalTime time1 = lesson1.getTimeSlot().getStartTime();
                    LocalTime time2 = lesson2.getTimeSlot().getStartTime();
                    return time2.isAfter(time1.plusHours(1));
                })
                .penalize("Minimize gaps in schedule",
                        HardSoftScore.ONE_SOFT,
                        (lesson1, lesson2) -> {
                            LocalTime time1 = lesson1.getTimeSlot().getStartTime();
                            LocalTime time2 = lesson2.getTimeSlot().getStartTime();
                            return (int) java.time.Duration.between(
                                    time1.plusHours(1), time2).toHours();
                        });
    }

}