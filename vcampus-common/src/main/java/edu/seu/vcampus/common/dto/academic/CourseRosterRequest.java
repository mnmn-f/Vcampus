package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 任课教师查询本人课程花名册；教师身份始终从服务端会话读取。 */
public final class CourseRosterRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long courseId;

    public CourseRosterRequest(long courseId) {
        this.courseId = courseId;
    }

    public long getCourseId() {
        return courseId;
    }
}
