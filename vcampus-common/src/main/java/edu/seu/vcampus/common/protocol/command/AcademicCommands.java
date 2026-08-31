package edu.seu.vcampus.common.protocol.command;

/** 教务模块命令和稳定结果码的唯一登记处。 */
public final class AcademicCommands {
    public static final String COURSE_LIST = "academic.course.list";
    public static final String COURSE_CREATE = "academic.course.create";
    public static final String COURSE_UPDATE = "academic.course.update";
    public static final String SCHEDULE_CREATE = "academic.schedule.create";
    public static final String SCHEDULE_UPDATE = "academic.schedule.update";
    public static final String SCHEDULE_DELETE = "academic.schedule.delete";
    public static final String STUDENT_ENROLL = "academic.enrollment.enroll";
    public static final String STUDENT_DROP = "academic.enrollment.drop";
    public static final String STUDENT_SCHEDULE = "academic.student.schedule";
    public static final String TEACHER_COURSES = "academic.teacher.courses";

    public static final String COURSE_NOT_FOUND = "ACADEMIC.COURSE_NOT_FOUND";
    public static final String SCHEDULE_NOT_FOUND = "ACADEMIC.SCHEDULE_NOT_FOUND";
    public static final String STUDENT_NOT_FOUND = "ACADEMIC.STUDENT_NOT_FOUND";
    public static final String TEACHER_NOT_FOUND = "ACADEMIC.TEACHER_NOT_FOUND";
    public static final String CLASSROOM_NOT_FOUND = "ACADEMIC.CLASSROOM_NOT_FOUND";
    public static final String COURSE_NOT_PUBLISHED = "ACADEMIC.COURSE_NOT_PUBLISHED";
    public static final String COURSE_CLOSED = "ACADEMIC.COURSE_CLOSED";
    public static final String COURSE_CAPACITY_FULL = "ACADEMIC.COURSE_CAPACITY_FULL";
    public static final String CAPACITY_TOO_SMALL = "ACADEMIC.CAPACITY_TOO_SMALL";
    public static final String DUPLICATE_ENROLLMENT = "ACADEMIC.DUPLICATE_ENROLLMENT";
    public static final String ENROLLMENT_NOT_FOUND = "ACADEMIC.ENROLLMENT_NOT_FOUND";
    public static final String ENROLLMENT_COMPLETED = "ACADEMIC.ENROLLMENT_COMPLETED";
    public static final String SCHEDULE_CONFLICT = "ACADEMIC.SCHEDULE_CONFLICT";
    public static final String CLASSROOM_CONFLICT = "ACADEMIC.CLASSROOM_CONFLICT";
    public static final String INVALID_COURSE = "ACADEMIC.INVALID_COURSE";
    public static final String INVALID_SCHEDULE = "ACADEMIC.INVALID_SCHEDULE";
    public static final String COURSE_HAS_ENROLLMENTS = "ACADEMIC.COURSE_HAS_ENROLLMENTS";

    /* Compatibility aliases kept as names, not duplicate wire values. */
    public static final String COURSE_QUERY = COURSE_LIST;
    public static final String ENROLL = STUDENT_ENROLL;
    public static final String DROP = STUDENT_DROP;

    private AcademicCommands() {
    }
}
