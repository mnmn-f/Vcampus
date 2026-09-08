package edu.seu.vcampus.common.dto.academic;

/** 课程生命周期状态，与 MySQL courses.status 保持一致。 */
public enum CourseStatus {
    DRAFT,
    PUBLISHED,
    CLOSED,
    ARCHIVED
}
