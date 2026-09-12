package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidatePage;
import edu.seu.vcampus.common.dto.student.StudentAccountCandidateQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentProfilePage;
import edu.seu.vcampus.common.dto.student.StudentProfileQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.dto.student.StudentStatus;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 学籍管理员查询档案、从待建档账号建立档案。 */
public final class StudentRegistrarPanel extends JPanel {
    private final BasePage page;
    private final StudentRecordClientService service;
    private final StudentProfileEditorPanel editor;
    private final JTextField studentNo = UiFactory.textField(12);
    private final JComboBox<String> college = filterBox(StudentDirectoryOptions.colleges(), "全部学院");
    private final JComboBox<String> major = filterBox(StudentDirectoryOptions.majors(), "全部专业");
    private final JComboBox<String> className = filterBox(StudentDirectoryOptions.classes(), "全部班级");
    private final JTextField candidateKeyword = UiFactory.textField(12);
    private final AsyncPagedTable<StudentProfileDto> profiles;

    public StudentRegistrarPanel(BasePage page, StudentRecordClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service;
        editor = new StudentProfileEditorPanel(new StudentProfileEditorPanel.Listener() {
            @Override public void onCreate(StudentProfileCreateRequest request) { saveNew(request); }
            @Override public void onUpdate(StudentProfileWriteRequest request) { saveUpdate(request); }
        });
        profiles = profileTable(); add(profiles); add(accountToolbar()); add(editor); loadCandidates();
    }

    private AsyncPagedTable<StudentProfileDto> profileTable() {
        AsyncPagedTable<StudentProfileDto> table = new AsyncPagedTable<StudentProfileDto>(
                "学生档案", "", "输入姓名", statusOptions(),
                new String[]{"学号", "姓名", "学院", "专业", "班级", "入学年份", "状态"},
                new AsyncPagedTable.Loader<StudentProfileDto>() {
                    @Override public PageSlice<StudentProfileDto> load(int p, String keyword, String filter) throws Exception {
                        StudentProfilePage result = service.searchProfiles(new StudentProfileQuery(
                                studentNo.getText(), keyword, selected(college), selected(major),
                                selected(className), status(filter), p, 20));
                        return new PageSlice<StudentProfileDto>(result.getItems(), result.getTotal(),
                                result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<StudentProfileDto>() {
                    @Override public Object[] values(StudentProfileDto row) {
                        return new Object[]{row.getStudentNo(), row.getDisplayName(), row.getCollege(),
                                row.getMajor(), row.getClassName(), RealUi.text(row.getEnrollmentYear()),
                                RealUi.status(RealUi.text(row.getStatus()))};
                    }
                }, new AsyncPagedTable.SelectionListener<StudentProfileDto>() {
                    @Override public void onSelected(StudentProfileDto row) { editor.showProfile(row); }
                });
        table.setAdditionalFilters(profileFilters(), new AsyncPagedTable.FilterCondition() {
            @Override public boolean isActive() { return hasText(studentNo) || hasText(college)
                    || hasText(major) || hasText(className); }
        });
        JButton query = new PrimaryButton("查询");
        query.addActionListener(e -> table.reload()); table.addAction(query);
        studentNo.addActionListener(e -> table.reload()); college.addActionListener(e -> table.reload());
        major.addActionListener(e -> table.reload()); className.addActionListener(e -> table.reload());
        table.getTable().getColumnModel().getColumn(0).setPreferredWidth(110);
        return table;
    }

    private JPanel accountToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0)); bar.setOpaque(false);
        bar.add(UiFactory.body("待建档账号"), BorderLayout.WEST);
        candidateKeyword.setToolTipText("输入账号或姓名"); bar.add(candidateKeyword, BorderLayout.CENTER);
        JButton refresh = new PrimaryButton("刷新账号"); refresh.addActionListener(e -> loadCandidates());
        bar.add(refresh, BorderLayout.EAST);
        candidateKeyword.addActionListener(e -> loadCandidates()); return bar;
    }

    private JPanel profileFilters() {
        JPanel filters = new JPanel(new GridLayout(2, 2, 12, 6)); filters.setOpaque(false);
        filters.add(filter("学号", studentNo)); filters.add(UiFactory.labelledField("学院", college));
        filters.add(UiFactory.labelledField("专业", major)); filters.add(UiFactory.labelledField("班级", className)); return filters;
    }

    private JPanel filter(String label, JTextField field) {
        JPanel value = new JPanel(new BorderLayout(8, 0)); value.setOpaque(false);
        value.add(UiFactory.body(label), BorderLayout.WEST); value.add(field, BorderLayout.CENTER); return value;
    }

    private void loadCandidates() {
        AsyncTask.run(new AsyncTask.Work<StudentAccountCandidatePage>() {
            @Override public StudentAccountCandidatePage run() throws Exception {
                return service.searchPendingAccounts(new StudentAccountCandidateQuery(
                        candidateKeyword.getText(), 1, StudentAccountCandidateQuery.MAX_PAGE_SIZE));
            }
        }, new AsyncTask.Callback<StudentAccountCandidatePage>() {
            @Override public void onSuccess(StudentAccountCandidatePage value) { editor.setCandidates(value.getItems()); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void saveNew(final StudentProfileCreateRequest request) {
        AsyncTask.run(new AsyncTask.Work<StudentProfileDto>() {
            @Override public StudentProfileDto run() throws Exception { return service.createProfile(request); }
        }, saveCallback("学生档案已创建。", false));
    }

    private void saveUpdate(final StudentProfileWriteRequest request) {
        AsyncTask.run(new AsyncTask.Work<StudentProfileDto>() {
            @Override public StudentProfileDto run() throws Exception { return service.updateProfile(request); }
        }, saveCallback("学生档案已更新。", true));
    }

    private AsyncTask.Callback<StudentProfileDto> saveCallback(final String success, final boolean update) {
        return new AsyncTask.Callback<StudentProfileDto>() {
            @Override public void onSuccess(StudentProfileDto value) {
                page.showSuccess(success); profiles.reload(); if (!update) loadCandidates();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        };
    }

    private static boolean hasText(JTextField field) { return field.getText() != null
            && !field.getText().trim().isEmpty(); }
    private static boolean hasText(JComboBox<String> box) { return box.getSelectedIndex() > 0; }
    private static String selected(JComboBox<String> box) { return box.getSelectedIndex() <= 0
            ? null : String.valueOf(box.getSelectedItem()); }
    private static JComboBox<String> filterBox(String[] values, String all) {
        JComboBox<String> box = new JComboBox<String>(); box.addItem(all);
        for (int i = 1; i < values.length; i++) box.addItem(values[i]);
        return box;
    }
    private static StudentStatus status(String value) {
        for (StudentStatus item : StudentStatus.values()) if (RealUi.status(item.name()).equals(value)) return item;
        return null;
    }
    private static String[] statusOptions() {
        StudentStatus[] values = StudentStatus.values(); String[] result = new String[values.length + 1];
        result[0] = "全部状态"; for (int i = 0; i < values.length; i++) result[i + 1] = RealUi.status(values[i].name());
        return result;
    }
}
