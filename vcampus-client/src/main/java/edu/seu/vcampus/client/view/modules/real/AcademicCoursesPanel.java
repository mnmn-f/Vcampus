package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseStatus;
import edu.seu.vcampus.common.dto.academic.CourseType;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/** 课程查询、学生选退课和教务课程维护。 */
public final class AcademicCoursesPanel extends JPanel {
    private final BasePage page;
    private final AcademicClientService service;
    private final Role role;
    private final JLabel detail = UiFactory.muted("选择课程查看详情。");
    private final JTextField courseCode = UiFactory.textField(10);
    private final JComboBox<Object> courseType = combo("全部类型", CourseType.values());
    private final JComboBox<Object> courseStatus = combo("全部状态", CourseStatus.values());
    private final AsyncPagedTable<CourseDto> courses;
    private final CourseEditorPanel editor;
    private final CourseScheduleEditorPanel scheduleEditor;

    public AcademicCoursesPanel(BasePage page, AcademicClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service; this.role = role;
        courses = table(); configureFilters(); add(courses);
        if (role == Role.ACADEMIC_ADMIN) {
            editor = new CourseEditorPanel(new CourseEditorPanel.Listener() {
                @Override public void onSave(CourseSaveRequest request, boolean update) { saveCourse(request, update); }
            }); add(editor);
            scheduleEditor = new CourseScheduleEditorPanel(page, service); add(scheduleEditor);
        } else { editor = null; scheduleEditor = null; }
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
    }

    private AsyncPagedTable<CourseDto> table() {
        AsyncPagedTable<CourseDto> table = new AsyncPagedTable<CourseDto>("课程与课表", "课程、容量、授课教师和状态。",
                "输入课程名称", null,
                new String[]{"编号", "课程名称", "类型", "学分", "容量", "状态"},
                new AsyncPagedTable.Loader<CourseDto>() {
                    @Override public PageSlice<CourseDto> load(int p, String keyword, String filter) throws Exception {
                        CourseQuery query = new CourseQuery(p, 20, null,
                                courseCode.getText(), keyword, selected(courseStatus),
                                selected(courseType));
                        CoursePageDto result = role == Role.TEACHER ? service.teacherCourses(query) : service.queryCourses(query);
                        return new PageSlice<CourseDto>(result.getItems(), result.getTotalElements(), result.getPageNumber(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<CourseDto>() {
                    @Override public Object[] values(CourseDto row) { return new Object[]{row.getCourseCode(), row.getCourseName(), RealUi.status(row.getCourseType()),
                            RealUi.text(row.getCredits()), row.getEnrolledCount() + "/" + row.getCapacity(), RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<CourseDto>() {
                    @Override public void onSelected(CourseDto row) { selectCourse(row); }
                });
        if (role == Role.STUDENT) {
            JButton enroll = new PrimaryButton("选课"); enroll.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { enroll(); }
            }); table.addAction(enroll);
            JButton drop = new DangerButton("退选"); drop.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { drop(); }
            }); table.addAction(drop);
        } else if (role == Role.ACADEMIC_ADMIN) {
            JButton create = new PrimaryButton("新建课程"); create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); table.addAction(create);
        }
        return table;
    }

    private void configureFilters() {
        JPanel filters = UiFactory.horizontal(8);
        filters.add(UiFactory.body("课程编号")); filters.add(courseCode);
        filters.add(UiFactory.body("课程类型")); filters.add(courseType);
        filters.add(UiFactory.body("课程状态")); filters.add(courseStatus);
        JButton reset = new SecondaryButton("重置");
        reset.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                courseCode.setText(""); courseType.setSelectedIndex(0);
                courseStatus.setSelectedIndex(0); courses.resetFilters();
            }
        });
        filters.add(reset);
        courses.setAdditionalFilters(filters, new AsyncPagedTable.FilterCondition() {
            @Override public boolean isActive() {
                return courseCode.getText().trim().length() > 0
                        || courseType.getSelectedIndex() > 0
                        || courseStatus.getSelectedIndex() > 0;
            }
        });
        java.awt.event.ActionListener reload = new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                courses.reload();
            }
        };
        courseCode.addActionListener(reload);
        courseType.addActionListener(reload);
        courseStatus.addActionListener(reload);
    }

    private void selectCourse(CourseDto value) {
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
        AsyncTask.run(new AsyncTask.Work<edu.seu.vcampus.common.dto.academic.EnrollmentDto>() {
            @Override public edu.seu.vcampus.common.dto.academic.EnrollmentDto run() throws Exception { return service.enroll(value.getId()); }
        }, new AsyncTask.Callback<edu.seu.vcampus.common.dto.academic.EnrollmentDto>() {
            @Override public void onSuccess(edu.seu.vcampus.common.dto.academic.EnrollmentDto result) { page.showSuccess("选课成功。"); courses.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void drop() {
        final CourseDto value = courses.selectedItem(); if (value == null) { page.showWarning("请先选择要退选的课程。"); return; }
        if (!RealUi.confirm(this, "确认退选“" + value.getCourseName() + "”？此操作会改变选课记录。")) return;
        AsyncTask.run(new AsyncTask.Work<Boolean>() {
            @Override public Boolean run() throws Exception { service.drop(value.getId()); return Boolean.TRUE; }
        }, new AsyncTask.Callback<Boolean>() {
            @Override public void onSuccess(Boolean result) { page.showSuccess("已退选课程。"); courses.reload(); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private static JComboBox<Object> combo(String all, Enum<?>[] values) {
        Object[] items = new Object[values.length + 1];
        items[0] = all;
        System.arraycopy(values, 0, items, 1, values.length);
        JComboBox<Object> result = new JComboBox<Object>(items);
        RealUi.codeRenderer(result);
        return result;
    }

    private static String selected(JComboBox<Object> box) {
        Object value = box.getSelectedItem();
        return value instanceof Enum ? ((Enum<?>) value).name() : null;
    }
}
