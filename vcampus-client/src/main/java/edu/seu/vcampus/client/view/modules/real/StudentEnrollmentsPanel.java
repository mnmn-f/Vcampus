package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentDto;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** Read-only current and historical enrollment view for the authenticated student. */
public final class StudentEnrollmentsPanel extends JPanel {
    private final AcademicClientService service;
    private final AsyncPagedTable<StudentEnrollmentDto> enrollments;

    public StudentEnrollmentsPanel(BasePage page, AcademicClientService service) {
        super(new BorderLayout());
        if (page == null || service == null) {
            throw new IllegalArgumentException("enrollment panel dependencies are required");
        }
        setOpaque(false);
        this.service = service;
        enrollments = table(); add(enrollments, BorderLayout.CENTER);
    }

    public void reload() { enrollments.reload(); }

    private AsyncPagedTable<StudentEnrollmentDto> table() {
        return new AsyncPagedTable<StudentEnrollmentDto>("我的选课记录",
                "当前选课与退选、已完成课程均保留展示。",
                "搜索课程编号或名称", statusOptions(),
                new String[]{"课程编号", "课程名称", "类型", "学分", "状态",
                        "选课时间", "退课时间", "上课安排"},
                new AsyncPagedTable.Loader<StudentEnrollmentDto>() {
                    @Override public PageSlice<StudentEnrollmentDto> load(int page,
                            String keyword, String filter) throws Exception {
                        StudentEnrollmentListDto value = service.studentEnrollments();
                        List<StudentEnrollmentDto> rows = select(value.getItems(), keyword,
                                statusCode(filter));
                        return new PageSlice<StudentEnrollmentDto>(rows, rows.size(), 1,
                                Math.max(1, rows.size()));
                    }
                }, new AsyncPagedTable.RowMapper<StudentEnrollmentDto>() {
                    @Override public Object[] values(StudentEnrollmentDto value) {
                        CourseDto course = value.getCourse();
                        return new Object[]{course.getCourseCode(), course.getCourseName(),
                                RealUi.status(course.getCourseType()), RealUi.text(course.getCredits()),
                                statusLabel(value.getEnrollment().getStatus()),
                                RealUi.dateTime(value.getEnrollment().getEnrolledAt()),
                                RealUi.dateTime(value.getEnrollment().getDroppedAt()),
                                schedule(course)};
                    }
                }, null);
    }

    private static List<StudentEnrollmentDto> select(List<StudentEnrollmentDto> values,
                                                       String keyword, String status) {
        List<StudentEnrollmentDto> result = new ArrayList<StudentEnrollmentDto>();
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        for (StudentEnrollmentDto value : values) {
            CourseDto course = value.getCourse();
            boolean text = key.length() == 0 || contains(course.getCourseCode(), key)
                    || contains(course.getCourseName(), key);
            boolean state = status == null || status.equals(value.getEnrollment().getStatus());
            if (text && state) result.add(value);
        }
        return result;
    }

    private static String[] statusOptions() {
        EnrollmentStatus[] values = EnrollmentStatus.values();
        String[] result = new String[values.length + 1];
        result[0] = "全部状态";
        for (int i = 0; i < values.length; i++) result[i + 1] = statusLabel(values[i].name());
        return result;
    }

    private static String statusCode(String label) {
        for (EnrollmentStatus value : EnrollmentStatus.values()) {
            if (statusLabel(value.name()).equals(label)) return value.name();
        }
        return null;
    }

    private static boolean contains(String value, String key) {
        return value != null && value.toLowerCase().contains(key);
    }

    private static String statusLabel(String code) {
        if (EnrollmentStatus.ENROLLED.name().equals(code)) return "当前选课";
        if (EnrollmentStatus.DROPPED.name().equals(code)) return "已退选";
        if (EnrollmentStatus.COMPLETED.name().equals(code)) return "已完成";
        return RealUi.status(code);
    }

    private static String schedule(CourseDto course) {
        if (course.getSchedules().isEmpty()) return "暂无";
        CourseScheduleDto value = course.getSchedules().get(0);
        ClassroomDto room = value.getClassroom();
        String location = room == null ? "待定"
                : RealUi.text(room.getBuildingName()) + " " + RealUi.text(room.getRoomNo());
        return "周" + weekday(value.getWeekday()) + " " + value.getStartPeriod() + "-"
                + value.getEndPeriod() + "节 / " + location;
    }

    private static String weekday(int value) {
        String[] names = {"?", "一", "二", "三", "四", "五", "六", "日"};
        return value > 0 && value < names.length ? names[value] : "?";
    }
}
