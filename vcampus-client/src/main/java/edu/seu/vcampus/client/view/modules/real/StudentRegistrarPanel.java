package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeReviewQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;

import javax.swing.JLabel;
import javax.swing.JPanel;

/** 学籍管理员实时维护档案并核对成绩。 */
public final class StudentRegistrarPanel extends JPanel {
    private final BasePage page;
    private final StudentRecordClientService service;
    private final StudentProfileEditorPanel editor;
    private final JLabel detail = UiFactory.muted("选择学生查看详情。");
    private final AsyncPagedTable<StudentProfileDto> profiles;
    private final AsyncPagedTable<StudentGradeDto> grades;
    private long detailSerial;

    public StudentRegistrarPanel(BasePage page, StudentRecordClientService service) {
        super(); setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service;
        profiles = profileTable();
        editor = new StudentProfileEditorPanel(new StudentProfileEditorPanel.Listener() {
            @Override public void onSave(StudentProfileWriteRequest request, boolean update) { saveProfile(request, update); }
        });
        PrimaryButton newProfile = new PrimaryButton("新建档案");
        newProfile.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
        });
        profiles.addAction(newProfile);
        grades = gradeTable();
        add(profiles); add(editor); add(grades);
    }

    private AsyncPagedTable<StudentProfileDto> profileTable() {
        AsyncPagedTable<StudentProfileDto> table = new AsyncPagedTable<StudentProfileDto>(
                "学生档案", "按学号、姓名、学院、专业和状态查询，支持分页浏览。", "搜索学号或姓名",
                new String[]{"全部状态", "在读", "休学", "毕业", "退学"},
                new String[]{"学号", "姓名", "学院", "专业", "班级", "状态"},
                new AsyncPagedTable.Loader<StudentProfileDto>() {
                    @Override public PageSlice<StudentProfileDto> load(int p, String keyword, String filter) throws Exception {
                        StudentStatus status = status(filter); String value = keyword == null ? null : keyword.trim();
                        String no = value != null && value.matches("\\d+") ? value : null; String name = no == null ? value : null;
                        StudentProfilePage result = service.searchProfiles(new StudentProfileQuery(no, name, null, null, null, status, p, 20));
                        return new PageSlice<StudentProfileDto>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<StudentProfileDto>() {
                    @Override public Object[] values(StudentProfileDto row) { return new Object[]{row.getStudentNo(), row.getDisplayName(), row.getCollege(), row.getMajor(), row.getClassName(), RealUi.status(RealUi.text(row.getStatus()))}; }
                }, new AsyncPagedTable.SelectionListener<StudentProfileDto>() {
                    @Override public void onSelected(StudentProfileDto row) { selectProfile(row); }
                });
        table.getTable().getColumnModel().getColumn(0).setPreferredWidth(110);
        return table;
    }

    private AsyncPagedTable<StudentGradeDto> gradeTable() {
        return new AsyncPagedTable<StudentGradeDto>("成绩档案核对", "按学生用户编号查看已登记成绩。",
                "搜索学生用户编号", null,
                new String[]{"学生用户编号", "课程编号", "课程名称", "成绩", "状态", "登记时间"},
                new AsyncPagedTable.Loader<StudentGradeDto>() {
                    @Override public PageSlice<StudentGradeDto> load(int p, String keyword, String filter) throws Exception {
                        Long studentId = RealUi.number(keyword); StudentGradePage result = service.reviewGrades(new StudentGradeReviewQuery(studentId, null, p, 20));
                        return new PageSlice<StudentGradeDto>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<StudentGradeDto>() {
                    @Override public Object[] values(StudentGradeDto row) { return new Object[]{row.getStudentUserId(), row.getCourseCode(), row.getCourseName(),
                            RealUi.text(row.getScore()), RealUi.status(row.getEnrollmentStatus()), RealUi.dateTime(row.getRecordedAt())}; }
                }, null);
    }

    private void selectProfile(final StudentProfileDto value) {
        if (value == null) { detail.setText("选择学生查看详情。"); editor.startNew(); return; }
        editor.showProfile(value); detail.setText("正在加载 " + value.getDisplayName() + " 的详情…");
        final long serial = ++detailSerial;
        AsyncTask.run(new AsyncTask.Work<edu.seu.vcampus.common.dto.student.StudentDetailDto>() {
            @Override public edu.seu.vcampus.common.dto.student.StudentDetailDto run() throws Exception { return service.getProfileDetail(value.getUserId()); }
        },
                new AsyncTask.Callback<edu.seu.vcampus.common.dto.student.StudentDetailDto>() {
                    @Override public void onSuccess(edu.seu.vcampus.common.dto.student.StudentDetailDto result) {
                        if (serial != detailSerial) return;
                        detail.setText("详情：" + RealUi.text(result.getProfile().getDisplayName())
                                + "，成绩记录 " + result.getGrades().size() + " 条");
                    }
                    @Override public void onFailure(Throwable error) { if (serial == detailSerial)
                        detail.setText("详情加载失败：" + AsyncTask.message(error)); }
                });
    }

    private void saveProfile(final StudentProfileWriteRequest request, final boolean update) {
        AsyncTask.run(new AsyncTask.Work<StudentProfileDto>() {
                    @Override public StudentProfileDto run() throws Exception { return update ? service.updateProfile(request) : service.createProfile(request); }
                },
                new AsyncTask.Callback<StudentProfileDto>() {
                    @Override public void onSuccess(StudentProfileDto value) {
                        page.showSuccess(update ? "学生档案已更新。" : "学生档案已创建。");
                        profiles.reload();
                    }
                    @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
                });
    }

    private static StudentStatus status(String value) {
        if ("在读".equals(value)) return StudentStatus.ENROLLED;
        if ("休学".equals(value)) return StudentStatus.SUSPENDED;
        if ("毕业".equals(value)) return StudentStatus.GRADUATED;
        if ("退学".equals(value)) return StudentStatus.WITHDRAWN;
        return null;
    }
}
