package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeReportDto;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

/** 学生本人档案、学期成绩和服务端计算的成绩指标。 */
public final class StudentOwnPanel extends JPanel {
    private final BasePage page;
    private final StudentRecordClientService service;
    private final JLabel profileState = UiFactory.muted("正在加载档案…");
    private final JLabel profile = UiFactory.body("");
    private final JLabel weightedGpa = UiFactory.body("—");
    private final JLabel averageGpa = UiFactory.body("—");
    private final JLabel weightedScore = UiFactory.body("—");
    private final JLabel averageScore = UiFactory.body("—");
    private final JTextField semester = UiFactory.textField(12);
    private final AsyncPagedTable<StudentGradeDto> grades;

    public StudentOwnPanel(BasePage page, StudentRecordClientService service) {
        super();
        if (page == null || service == null) throw new IllegalArgumentException("student panel dependencies are required");
        this.page = page; this.service = service;
        setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        add(profileCard()); add(metricsCard());
        JPanel filter = UiFactory.horizontal(8);
        filter.add(UiFactory.body("学期编号（留空为累计）")); filter.add(semester);
        javax.swing.JButton refresh = new PrimaryButton("查询");
        refresh.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { grades.reload(); }
        });
        filter.add(refresh); add(filter);
        grades = grades(); add(grades); loadProfile();
        semester.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { grades.reload(); }
        });
    }

    private SectionCard profileCard() {
        SectionCard card = new SectionCard("我的学籍档案", "仅显示本人档案和成绩。");
        JPanel content = new JPanel(new BorderLayout(12, 8)); content.setOpaque(false);
        content.add(profile, BorderLayout.CENTER); content.add(profileState, BorderLayout.SOUTH);
        card.setContent(content); return card;
    }

    private SectionCard metricsCard() {
        SectionCard card = new SectionCard("成绩指标", "按东南大学4.8制映射计算；不计入课程由服务端标记。");
        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 0)); grid.setOpaque(false);
        grid.add(metric("加权绩点/平均学分绩点", weightedGpa));
        grid.add(metric("平均绩点", averageGpa));
        grid.add(metric("加权均分", weightedScore)); grid.add(metric("平均均分", averageScore));
        card.setContent(grid); return card;
    }

    private JPanel metric(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 4)); panel.setOpaque(false);
        panel.add(UiFactory.muted(title), BorderLayout.NORTH); panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private AsyncPagedTable<StudentGradeDto> grades() {
        AsyncPagedTable<StudentGradeDto> table = new AsyncPagedTable<StudentGradeDto>(
                "我的成绩", "成绩按学期筛选，留空查看累计指标。", "输入课程编号筛选", null,
                new String[]{"学期", "课程编号", "课程名称", "学分", "成绩", "绩点", "状态"},
                new AsyncPagedTable.Loader<StudentGradeDto>() {
                    @Override public PageSlice<StudentGradeDto> load(int pageNumber,
                            String keyword, String filter) throws Exception {
                        Long courseId = RealUi.number(keyword);
                        StudentGradeQuery query = new StudentGradeQuery(semester.getText(),
                                courseId, pageNumber, StudentGradeQuery.firstPage().getPageSize());
                        final StudentGradeReportDto report = service.getOwnGradeReport(query);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override public void run() { showMetrics(report.getMetrics()); }
                        });
                        StudentGradePage result = report.getGrades();
                        return new PageSlice<StudentGradeDto>(result.getItems(), result.getTotal(),
                                result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<StudentGradeDto>() {
                    @Override public Object[] values(StudentGradeDto row) {
                        return new Object[]{RealUi.text(row.getSemesterCode()), row.getCourseCode(),
                                row.getCourseName(), RealUi.text(row.getCredits()),
                                 RealUi.text(row.getScore()), RealUi.text(row.getGradePoint()),
                                 row.isGpaIncluded() ? RealUi.status(row.getEnrollmentStatus())
                                         : "不计入指标"};
                    }
                }, null);
        javax.swing.JButton export = new PrimaryButton("导出当前范围");
        export.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { exportGrades(); }
        });
        table.addAction(export); return table;
    }

    private void showMetrics(StudentGradeMetricsDto value) {
        if (value == null) return;
        weightedGpa.setText(metricText(value.getWeightedGpa()));
        averageGpa.setText(metricText(value.getAverageGpa()));
        weightedScore.setText(metricText(value.getWeightedAverageScore()));
        averageScore.setText(metricText(value.getAverageScore()));
    }

    private void exportGrades() {
        final JFileChooser chooser = new JFileChooser(); chooser.setSelectedFile(new File("成绩导出.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        final File file = chooser.getSelectedFile();
        if (file.exists() && !RealUi.confirm(this, "文件已存在，确认覆盖吗？")) return;
        AsyncTask.run(new AsyncTask.Work<StudentGradeExportDto>() {
            @Override public StudentGradeExportDto run() throws Exception {
                StudentGradeExportDto data = service.exportOwnGrades(
                        new StudentGradeExportQuery(semester.getText(), null));
                StudentGradeCsvExporter.write(file, data); return data;
            }
        }, new AsyncTask.Callback<StudentGradeExportDto>() {
            @Override public void onSuccess(StudentGradeExportDto value) { page.showSuccess("成绩和指标已导出。"); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void loadProfile() {
        AsyncTask.run(new AsyncTask.Work<StudentProfileDto>() {
            @Override public StudentProfileDto run() throws Exception { return service.getOwnProfile(); }
        }, new AsyncTask.Callback<StudentProfileDto>() {
            @Override public void onSuccess(StudentProfileDto value) {
                profile.setText("学号：" + RealUi.text(value.getStudentNo()) + "　姓名："
                        + RealUi.text(value.getDisplayName()) + "　学院：" + RealUi.text(value.getCollege())
                        + "　专业：" + RealUi.text(value.getMajor()) + "　班级："
                        + RealUi.text(value.getClassName()) + "　状态："
                        + RealUi.status(RealUi.text(value.getStatus())));
                profileState.setText("档案已加载"); page.showSuccess("已加载你的档案和成绩。");
            }
            @Override public void onFailure(Throwable error) { profileState.setText("档案加载失败：" + AsyncTask.message(error)); page.showError(AsyncTask.message(error)); }
        });
    }

    private static String metricText(Object value) { return value == null ? "—" : String.valueOf(value); }
}
