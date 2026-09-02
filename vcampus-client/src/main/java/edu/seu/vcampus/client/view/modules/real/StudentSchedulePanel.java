package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.ClassroomDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseInstructorDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** 学生本人按学期查看课表，并点击课程查看教师、教室及时段详情。 */
public final class StudentSchedulePanel extends SectionCard {
    private static final String[] WEEKDAYS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private final BasePage page;
    private final AcademicClientService service;
    private final JTextField semester = UiFactory.textField(12);
    private final JLabel detail = UiFactory.body("选择课程查看教师、教室和上课时段。");
    private AsyncPagedTable<CourseDto> table;

    public StudentSchedulePanel(BasePage page, AcademicClientService service) {
        super("我的课表", "按学期查看本人已选课程；点击课程查看详细安排。");
        if (page == null || service == null) throw new IllegalArgumentException("schedule dependencies are required");
        this.page = page; this.service = service;
        setContent(content());
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(0, 10)); root.setOpaque(false);
        JPanel filter = UiFactory.horizontal(8);
        filter.add(UiFactory.body("学期编号（留空为全部）")); filter.add(semester);
        javax.swing.JButton query = new PrimaryButton("查询课表");
        query.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { table.reload(); }
        });
        filter.add(query); root.add(filter, BorderLayout.NORTH);
        table = scheduleTable(); root.add(table, BorderLayout.CENTER);
        JPanel detailBox = new JPanel(new BorderLayout()); detailBox.setOpaque(false);
        detailBox.add(detail, BorderLayout.CENTER); root.add(detailBox, BorderLayout.SOUTH);
        semester.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { table.reload(); }
        });
        return root;
    }

    private AsyncPagedTable<CourseDto> scheduleTable() {
        return new AsyncPagedTable<CourseDto>("课程列表", "选中一行后，下方显示完整课程详情。",
                "搜索课程编号或名称", null,
                new String[]{"课程编号", "课程名称", "学分", "授课教师", "上课时段"},
                new AsyncPagedTable.Loader<CourseDto>() {
                    @Override public PageSlice<CourseDto> load(int page, String keyword,
                                                                  String filter) throws Exception {
                        StudentScheduleDto result = service.studentSchedule(
                                new StudentScheduleQuery(semester.getText()));
                        List<CourseDto> rows = filter(result.getCourses(), keyword);
                        return new PageSlice<CourseDto>(rows, rows.size(), 1,
                                Math.max(1, rows.size()));
                    }
                }, new AsyncPagedTable.RowMapper<CourseDto>() {
                    @Override public Object[] values(CourseDto row) {
                        return new Object[]{row.getCourseCode(), row.getCourseName(),
                                RealUi.text(row.getCredits()), instructorNames(row), scheduleNames(row)};
                    }
                }, new AsyncPagedTable.SelectionListener<CourseDto>() {
                    @Override public void onSelected(CourseDto row) { showDetail(row); }
                });
    }

    private void showDetail(CourseDto course) {
        if (course == null) { detail.setText("选择课程查看教师、教室和上课时段。"); return; }
        StringBuilder text = new StringBuilder("课程：").append(RealUi.text(course.getCourseCode()))
                .append("　").append(RealUi.text(course.getCourseName())).append("　学期：")
                .append(RealUi.text(course.getSemesterCode())).append("\n授课教师：")
                .append(instructorDetails(course));
        if (course.getSchedules().isEmpty()) text.append("\n上课安排：暂无");
        for (CourseScheduleDto schedule : course.getSchedules()) {
            text.append("\n上课安排：").append(weekday(schedule.getWeekday())).append(" ")
                    .append(schedule.getStartPeriod()).append("-").append(schedule.getEndPeriod())
                    .append("节，日期 ").append(RealUi.date(schedule.getStartDate())).append(" 至 ")
                    .append(RealUi.date(schedule.getEndDate())).append("，教室 ")
                    .append(classroomDetails(schedule.getClassroom()));
        }
        detail.setText("<html>" + text.toString().replace("\n", "<br>") + "</html>");
        page.showInfo("已显示课程详情。");
    }

    private static List<CourseDto> filter(List<CourseDto> values, String keyword) {
        List<CourseDto> rows = new ArrayList<CourseDto>();
        String key = keyword == null ? "" : keyword.trim().toLowerCase();
        for (CourseDto value : values) if (key.length() == 0
                || contains(value.getCourseCode(), key) || contains(value.getCourseName(), key)) rows.add(value);
        return rows;
    }

    private static boolean contains(String value, String key) {
        return value != null && value.toLowerCase().contains(key);
    }

    private static String instructorNames(CourseDto course) {
        return course.getInstructors().isEmpty() ? "--" : course.getInstructors().get(0).getDisplayName();
    }

    private static String instructorDetails(CourseDto course) {
        StringBuilder result = new StringBuilder();
        for (CourseInstructorDto teacher : course.getInstructors()) {
            if (result.length() > 0) result.append("；");
            result.append(RealUi.text(teacher.getDisplayName())).append("（工号 ")
                    .append(teacher.getEmployeeNo() == null ? teacher.getTeacherUserId()
                            : teacher.getEmployeeNo()).append("）");
        }
        return result.length() == 0 ? "暂无" : result.toString();
    }

    private static String scheduleNames(CourseDto course) {
        StringBuilder result = new StringBuilder();
        for (CourseScheduleDto schedule : course.getSchedules()) {
            if (result.length() > 0) result.append("；");
            result.append(weekday(schedule.getWeekday())).append(" ")
                    .append(schedule.getStartPeriod()).append("-").append(schedule.getEndPeriod()).append("节");
        }
        return result.length() == 0 ? "暂无" : result.toString();
    }

    private static String classroomDetails(ClassroomDto classroom) {
        return classroom == null ? "待定" : RealUi.text(classroom.getBuildingName()) + " "
                + RealUi.text(classroom.getRoomNo()) + "（" + RealUi.text(classroom.getClassroomType()) + "）";
    }

    private static String weekday(int value) { return value > 0 && value < WEEKDAYS.length ? WEEKDAYS[value] : "星期?"; }
}
