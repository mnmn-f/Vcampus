package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 选课或退课请求；学生身份始终从服务端会话读取。 */
public final class EnrollmentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long courseId;

    public EnrollmentRequest(long courseId) {
        this.courseId = courseId;
    }

    public long getCourseId() {
        return courseId;
    }
}
