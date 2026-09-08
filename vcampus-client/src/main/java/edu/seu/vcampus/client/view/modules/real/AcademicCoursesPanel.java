package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.EnrollmentStatus;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 课程查询、学生选退课和教务课程维护。 */
public final class AcademicCoursesPanel extends JPanel {
    private final BasePage page;
    private final AcademicClientService service;
    private final Role role;
    private final JLabel detail = UiFactory.muted("选择课程查看详情。");
    private final AcademicCourseFilters filters;
    private final AsyncPagedTable<CourseDto> courses;
    private final CourseEditorPanel editor;
    private final CourseScheduleEditorPanel scheduleEditor;
    private final AcademicStudentEnrollmentState enrollmentState = new AcademicStudentEnrollmentState();
    private final Runnable enrollmentChanged;
    private JButton enrollButton;
    private JButton dropButton;

    public AcademicCoursesPanel(BasePage page, AcademicClientService service, Role role) {
        this(page, service, role, null);
    }

    public AcademicCoursesPanel(BasePage page, AcademicClientService service, Role role,
                                Runnable enrollmentChanged) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role;
        this.enrollmentChanged = enrollmentChanged;
        filters = new AcademicCourseFilters(role); courses = table(); filters.attach(courses); add(courses);
        if (role == Role.ACADEMIC_ADMIN) {
            editor = new CourseEditorPanel(new CourseEditorPanel.Listener() {
                @Override public void onSave(CourseSaveRequest request, boolean update) { saveCourse(request, update); }
            }); add(editor);
            scheduleEditor = new CourseScheduleEditorPanel(page, service,
                    new Runnable() { @Override public void run() { courses.refreshCurrentPage(); } });
            add(scheduleEditor);
        } else { editor = null; scheduleEditor = null; }
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
    }

    private AsyncPagedTable<CourseDto> table() {
        AsyncPagedTable<CourseDto> table = new AsyncPagedTable<CourseDto>("课程与课表", "课程、容量、授课教师和状态。",
                "输入课程名称", null,
                columns(),
                new AsyncPagedTable.Loader<CourseDto>() {
                    @Override public PageSlice<CourseDto> load(int p, String keyword, String filter) throws Exception {
                        if (role == Role.STUDENT) enrollmentState.replace(service.studentEnrollments());
                        CourseQuery query = new CourseQuery(p, 20, null,
                                filters.courseCode(), keyword, filters.courseStatus(), filters.courseType());
                        CoursePageDto result = role == Role.TEACHER ? service.teacherCourses(query) : service.queryCourses(query);
                        return new PageSlice<CourseDto>(result.getItems(), result.getTotalElements(), result.getPageNumber(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<CourseDto>() {
                    @Override public Object[] values(CourseDto row) { return row(row); }
                }, new AsyncPagedTable.SelectionListener<CourseDto>() {
                    @Override public void onSelected(CourseDto row) { selectCourse(row); }
                });
        if (role == Role.STUDENT) {
            enrollButton = new PrimaryButton("选课"); enrollButton.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { enroll(); }
            }); table.addAction(enrollButton);
            dropButton = new DangerButton("退选"); dropButton.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { drop(); }
            }); table.addAction(dropButton);
        } else if (role == Role.ACADEMIC_ADMIN) {
            JButton create = new PrimaryButton("新建课程"); create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); table.addAction(create);
        }
        table.setItemKey(new java.util.function.Function<CourseDto, Object>() {
            @Override public Object apply(CourseDto value) { return Long.valueOf(value.getId()); }
        });
        return table;
    }

    private void selectCourse(CourseDto value) {
        updateStudentActions(value);
        if (value == null) {
            detail.setText("选择课程查看详情。");
            if (editor != null) editor.startNew();
            if (scheduleEditor != null) scheduleEditor.showCourse(null);
            return;
        }
        detail.setText("课程详情：" + RealUi.text(value.getCourseCode()) + "　" + RealUi.text(value.getCourseName())
                + "　说明：" + RealUi.text(value.getDescription()) + "　授课教师：" + value.getInstructors().size()
                + " 人　时段：" + value.getSchedules().size() + " 条");
        if (editor != null) editor.showCourse(value);
        if (scheduleEditor != null) scheduleEditor.showCourse(value);
    }

    private void saveCourse(CourseSaveRequest request, boolean update) {
        final CourseSaveRequest finalRequest = request;
        final boolean finalUpdate = update;
        AsyncTask.run(new AsyncTask.Work<CourseDto>() {
                    @Override public CourseDto run() throws Exception {
                        return finalUpdate ? service.updateCourse(finalRequest) : service.createCourse(finalRequest);
                    }
                },
                new AsyncTask.Callback<CourseDto>() {
                    @Override public void onSuccess(CourseDto value) { page.showSuccess(finalUpdate ? "课程已更新。" : "课程已创建。" ); courses.reload(); }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }

    private void enroll() {
        final CourseDto value = courses.selectedItem(); if (value == null) { page.showWarning("请先选择要选修的课程。"); return; }
        String state = enrollmentState.get(value.getId());
        if (EnrollmentStatus.COMPLETED.name().equals(state)) { page.showWarning("该课程已完成，不能重复选课。"); return; }
        if (EnrollmentStatus.ENROLLED.name().equals(state)) { page.showWarning("该课程已经选过。"); return; }
        if (!CourseStatus.PUBLISHED.name().equals(value.getStatus())) { page.showWarning("当前课程未开放选课。"); return; }
        AsyncTask.run(new AsyncTask.Work<edu.seu.vcampus.common.dto.academic.EnrollmentDto>() {
            @Override public edu.seu.vcampus.common.dto.academic.EnrollmentDto run() throws Exception { return service.enroll(value.getId()); }
        }, new AsyncTask.Callback<edu.seu.vcampus.common.dto.academic.EnrollmentDto>() {
            @Override public void onSuccess(edu.seu.vcampus.common.dto.academic.EnrollmentDto result) { changed("选课成功。"); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void drop() {
        final CourseDto value = courses.selectedItem(); if (value == null) { page.showWarning("请先选择要退选的课程。"); return; }
        if (!EnrollmentStatus.ENROLLED.name().equals(enrollmentState.get(value.getId()))) {
            page.showWarning("只有当前已选课程可以退选。"); return;
        }
        if (!RealUi.confirm(this, "确认退选“" + value.getCourseName() + "”？此操作会改变选课记录。")) return;
        AsyncTask.run(new AsyncTask.Work<Boolean>() {
            @Override public Boolean run() throws Exception { service.drop(value.getId()); return Boolean.TRUE; }
        }, new AsyncTask.Callback<Boolean>() {
            @Override public void onSuccess(Boolean result) { changed("已退选课程。"); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void changed(String message) {
        page.showSuccess(message); courses.reload();
        if (enrollmentChanged != null) enrollmentChanged.run();
    }

    private void updateStudentActions(CourseDto value) {
        if (role != Role.STUDENT || enrollButton == null) return;
        String state = value == null ? null : enrollmentState.get(value.getId());
        enrollButton.setEnabled(value != null && !EnrollmentStatus.ENROLLED.name().equals(state)
                && !EnrollmentStatus.COMPLETED.name().equals(state)
                && CourseStatus.PUBLISHED.name().equals(value.getStatus()));
        dropButton.setEnabled(value != null && EnrollmentStatus.ENROLLED.name().equals(state));
    }

    private String[] columns() {
        return role == Role.STUDENT
                ? new String[]{"编号", "课程名称", "类型", "学分", "容量", "课程状态", "选课状态"}
                : new String[]{"编号", "课程名称", "类型", "学分", "容量", "状态"};
    }

    private Object[] row(CourseDto value) {
        Object[] base = new Object[]{value.getCourseCode(), value.getCourseName(),
                RealUi.status(value.getCourseType()), RealUi.text(value.getCredits()),
                value.getEnrolledCount() + "/" + value.getCapacity(), RealUi.status(value.getStatus())};
        if (role != Role.STUDENT) return base;
        return new Object[]{base[0], base[1], base[2], base[3], base[4], base[5],
                AcademicStudentEnrollmentState.label(enrollmentState.get(value.getId()))};
    }

}
