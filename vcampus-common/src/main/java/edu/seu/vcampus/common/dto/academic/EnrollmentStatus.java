package edu.seu.vcampus.common.dto.academic;

/** 选课关系状态，与 MySQL enrollments.status 保持一致。 */
public enum EnrollmentStatus {
    ENROLLED,
    DROPPED,
    COMPLETED
}
