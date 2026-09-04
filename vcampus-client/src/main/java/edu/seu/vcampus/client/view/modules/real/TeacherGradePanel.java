package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.academic.AcademicClientService;
import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterEntryDto;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/** 任课教师从本人课程花名册选择学生后登记成绩。 */
public final class TeacherGradePanel extends SectionCard {
    private final BasePage page;
    private final AcademicClientService academic;
    private final StudentRecordClientService grades;
    private final JComboBox<CourseOption> courses = new JComboBox<CourseOption>();
    private final DefaultTableModel rosterModel = new DefaultTableModel(
            new String[]{"学号", "姓名", "学院", "专业", "班级", "选课状态"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable roster = new JTable(rosterModel);
    private final JTextField score = UiFactory.textField(12);
    private final JTextArea remark = UiFactory.textArea(3, 24);
    private final JLabel state = UiFactory.muted("正在加载本人授课课程…");
    private List<CourseRosterEntryDto> rosterEntries =
            Collections.<CourseRosterEntryDto>emptyList();
    private CourseRosterEntryDto selectedEnrollment;
    private int rosterSerial;

    public TeacherGradePanel(BasePage page, AcademicClientService academic,
                             StudentRecordClientService grades) {
        super("成绩登记", "选择本人授课课程及未退课学生，成绩保存复用现有登记链路。");
        if (page == null || academic == null || grades == null) {
            throw new IllegalArgumentException("grade panel dependencies are required");
        }
        this.page = page; this.academic = academic; this.grades = grades;
        configureRoster();
        courses.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { loadRoster(); }
        });

        JPanel form = new JPanel(new GridLayout(0, 2, 12, 8)); form.setOpaque(false);
        form.add(UiFactory.labelledField("成绩（0-100）", score));
        form.add(UiFactory.labelledField("备注", remark));

        JPanel footer = new JPanel(new BorderLayout(0, 10)); footer.setOpaque(false);
        footer.add(form, BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton save = new PrimaryButton("登记 / 修改成绩");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        actions.add(state); actions.add(save); footer.add(actions, BorderLayout.SOUTH);

        JScrollPane rosterScroll = new JScrollPane(roster);
        rosterScroll.setPreferredSize(new Dimension(0, 210));
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(UiFactory.labelledField("本人授课课程", courses), BorderLayout.NORTH);
        content.add(rosterScroll, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        setContent(content);
        loadCourses();
    }

    private void configureRoster() {
        roster.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roster.setRowHeight(34);
        roster.setFillsViewportHeight(true);
        roster.getSelectionModel().addListSelectionListener(
                new javax.swing.event.ListSelectionListener() {
                    @Override public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) return;
                        int row = roster.getSelectedRow();
                        selectedEnrollment = row < 0 || row >= rosterEntries.size()
                                ? null : rosterEntries.get(row);
                        if (selectedEnrollment != null) {
                            score.setText(selectedEnrollment.getScore() == null ? ""
                                    : selectedEnrollment.getScore().toPlainString());
                            remark.setText(RealUi.text(selectedEnrollment.getGradeRemark()));
                            state.setText("已选择 " + RealUi.text(selectedEnrollment.getDisplayName())
                                    + "（" + RealUi.text(selectedEnrollment.getStudentNo()) + "）");
                        }
                    }
                });
    }

    private void loadCourses() {
        courses.setEnabled(false); state.setText("正在加载本人授课课程…");
        AsyncTask.run(new AsyncTask.Work<CoursePageDto>() {
            @Override public CoursePageDto run() throws Exception {
                return academic.teacherCourses(new CourseQuery(1, CourseQuery.MAX_PAGE_SIZE,
                        null, null, null));
            }
        }, new AsyncTask.Callback<CoursePageDto>() {
            @Override public void onSuccess(CoursePageDto value) {
                courses.removeAllItems();
                for (CourseDto course : value.getItems()) courses.addItem(new CourseOption(course));
                courses.setEnabled(courses.getItemCount() > 0);
                if (courses.getItemCount() == 0) {
                    clearRoster(); state.setText("当前没有授课课程");
                } else {
                    courses.setSelectedIndex(0);
                }
            }
            @Override public void onFailure(Throwable error) {
                clearRoster(); courses.setEnabled(false); state.setText("课程加载失败");
                page.showError(AsyncTask.message(error));
            }
        });
    }

    private void loadRoster() {
        final CourseOption selected = (CourseOption) courses.getSelectedItem();
        if (selected == null) return;
        final long courseId = selected.course.getId();
        final int serial = ++rosterSerial;
        clearRoster(); state.setText("正在加载课程花名册…");
        AsyncTask.run(new AsyncTask.Work<CourseRosterDto>() {
            @Override public CourseRosterDto run() throws Exception {
                return academic.courseRoster(courseId);
            }
        }, new AsyncTask.Callback<CourseRosterDto>() {
            @Override public void onSuccess(CourseRosterDto value) {
                if (serial != rosterSerial || selectedCourseId() != courseId) return;
                rosterEntries = value.getEntries(); rosterModel.setRowCount(0);
                for (CourseRosterEntryDto entry : rosterEntries) {
                    rosterModel.addRow(new Object[]{entry.getStudentNo(), entry.getDisplayName(),
                            entry.getCollege(), entry.getMajor(), entry.getClassName(),
                            RealUi.status(entry.getEnrollmentStatus())});
                }
                state.setText(rosterEntries.isEmpty()
                        ? "该课程暂无未退课学生" : "请选择一名学生登记成绩");
            }
            @Override public void onFailure(Throwable error) {
                if (serial != rosterSerial) return;
                clearRoster(); state.setText("花名册加载失败");
                page.showError(AsyncTask.message(error));
            }
        });
    }

    private void clearRoster() {
        rosterEntries = Collections.emptyList(); selectedEnrollment = null;
        rosterModel.setRowCount(0); roster.clearSelection();
        score.setText(""); remark.setText("");
    }

    private long selectedCourseId() {
        CourseOption selected = (CourseOption) courses.getSelectedItem();
        return selected == null ? -1L : selected.course.getId();
    }

    private void submit() {
        if (selectedEnrollment == null) {
            state.setText("请先从花名册选择学生"); return;
        }
        try {
            BigDecimal value = new BigDecimal(score.getText().trim());
            final StudentGradeRecordRequest request = new StudentGradeRecordRequest(
                    selectedEnrollment.getEnrollmentId(), value, null, remark.getText());
            state.setText("正在提交…");
            AsyncTask.run(new AsyncTask.Work<Object>() {
                @Override public Object run() throws Exception { return grades.recordGrade(request); }
            }, new AsyncTask.Callback<Object>() {
                @Override public void onSuccess(Object value) {
                    state.setText("已登记"); page.showSuccess("成绩已提交并保存。");
                }
                @Override public void onFailure(Throwable error) {
                    state.setText("提交失败"); page.showError(AsyncTask.message(error));
                }
            });
        } catch (NumberFormatException ex) {
            state.setText("成绩必须是数字");
        }
    }
    private static final class CourseOption {
        private final CourseDto course;
        private CourseOption(CourseDto course) { this.course = course; }
        @Override public String toString() {
            return course.getCourseCode() + "　" + course.getCourseName();
        }
    }
}
