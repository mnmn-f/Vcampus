package edu.seu.vcampus.server.student.repository;

/** 成绩登记用的内部选课关系快照。 */
public final class EnrollmentRecord {
    private final long id;
    private final long studentUserId;
    private final long courseId;
    private final String status;

    public EnrollmentRecord(long id, long studentUserId, long courseId, String status) {
        this.id = id;
        this.studentUserId = studentUserId;
        this.courseId = courseId;
        this.status = status;
    }

    public long getId() { return id; }
    public long getStudentUserId() { return studentUserId; }
    public long getCourseId() { return courseId; }
    public String getStatus() { return status; }
}
