package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** One of the current student's enrollment records together with course details. */
public final class StudentEnrollmentDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final EnrollmentDto enrollment;
    private final CourseDto course;

    public StudentEnrollmentDto(EnrollmentDto enrollment, CourseDto course) {
        if (enrollment == null || course == null) {
            throw new IllegalArgumentException("enrollment and course are required");
        }
        this.enrollment = enrollment;
        this.course = course;
    }

    public EnrollmentDto getEnrollment() { return enrollment; }
    public CourseDto getCourse() { return course; }
}
