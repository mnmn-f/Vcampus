package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;

import java.util.HashMap;
import java.util.Map;

/** Current student's enrollment state keyed by course id. */
final class AcademicStudentEnrollmentState {
    private final Map<Long, String> values = new HashMap<Long, String>();

    void replace(StudentEnrollmentListDto source) {
        values.clear();
        if (source == null) return;
        for (StudentEnrollmentDto item : source.getItems()) {
            values.put(Long.valueOf(item.getCourse().getId()), item.getEnrollment().getStatus());
        }
    }

    String get(long courseId) {
        return values.get(Long.valueOf(courseId));
    }

    static String label(String status) {
        if (EnrollmentStatus.ENROLLED.name().equals(status)) return "已选课";
        if (EnrollmentStatus.COMPLETED.name().equals(status)) return "已完成";
        if (EnrollmentStatus.DROPPED.name().equals(status)) return "已退选";
        return "未选课";
    }
}
