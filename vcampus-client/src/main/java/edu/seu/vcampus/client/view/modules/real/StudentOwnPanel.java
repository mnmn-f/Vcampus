package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 学生本人档案和成绩查询。 */
public final class StudentOwnPanel extends JPanel {
    private final BasePage page;
    private final StudentRecordClientService service;
    private final JLabel profileState = UiFactory.muted("正在加载档案…");
    private final JLabel profile = UiFactory.body("");

    public StudentOwnPanel(BasePage page, StudentRecordClientService service) {
        super();
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service;
        add(profileCard());
        add(grades());
        loadProfile();
    }

    private SectionCard profileCard() {
        SectionCard card = new SectionCard("我的学籍档案", "仅显示本人档案和成绩。");
        JPanel content = new JPanel(new BorderLayout(12, 8)); content.setOpaque(false);
        profile.setForeground(DesignTokens.TEXT_PRIMARY); profile.setFont(DesignTokens.regular(14));
        content.add(profile, BorderLayout.CENTER); content.add(profileState, BorderLayout.SOUTH);
        card.setContent(content); return card;
    }

    private AsyncPagedTable<StudentGradeDto> grades() {
        return new AsyncPagedTable<StudentGradeDto>("我的成绩", "仅显示本人成绩档案。",
                "输入课程编号筛选", null,
                new String[]{"课程编号", "课程名称", "成绩", "绩点", "状态", "登记时间"},
                new AsyncPagedTable.Loader<StudentGradeDto>() {
                    @Override public PageSlice<StudentGradeDto> load(int pageNumber, String keyword, String filter) throws Exception {
                        Long courseId = RealUi.number(keyword);
                        StudentGradePage result = service.getOwnGrades(new StudentGradeQuery(courseId, pageNumber, StudentGradeQuery.firstPage().getPageSize()));
                        return new PageSlice<StudentGradeDto>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<StudentGradeDto>() {
                    @Override public Object[] values(StudentGradeDto row) { return new Object[]{row.getCourseCode(), row.getCourseName(),
                            RealUi.text(row.getScore()), RealUi.text(row.getGradePoint()), RealUi.status(row.getEnrollmentStatus()), RealUi.dateTime(row.getRecordedAt())}; }
                }, null);
    }

    private void loadProfile() {
        AsyncTask.run(new AsyncTask.Work<StudentProfileDto>() {
            @Override public StudentProfileDto run() throws Exception { return service.getOwnProfile(); }
        }, new AsyncTask.Callback<StudentProfileDto>() {
            @Override public void onSuccess(StudentProfileDto value) {
                profile.setText("学号：" + RealUi.text(value.getStudentNo()) + "　姓名："
                        + RealUi.text(value.getDisplayName()) + "　学院：" + RealUi.text(value.getCollege())
                        + "　专业：" + RealUi.text(value.getMajor()) + "　班级："
                        + RealUi.text(value.getClassName()) + "　状态：" + RealUi.status(RealUi.text(value.getStatus())));
                profileState.setText("档案已加载"); page.showSuccess("已加载你的档案和成绩。");
            }
            @Override public void onFailure(Throwable error) {
                profileState.setText("档案加载失败：" + AsyncTask.message(error));
                page.showError(AsyncTask.message(error));
            }
        });
    }
}
