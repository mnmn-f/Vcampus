package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.student.StudentGradeRecordRequest;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** 任课教师成绩登记的行内表单。 */
public final class TeacherGradePanel extends SectionCard {
    private final BasePage page;
    private final StudentRecordClientService service;
    private final JTextField enrollmentId = UiFactory.textField(12);
    private final JTextField score = UiFactory.textField(12);
    private final JTextArea remark = UiFactory.textArea(3, 24);
    private final JLabel state = UiFactory.muted("填写选课记录编号后提交成绩。");

    public TeacherGradePanel(BasePage page, StudentRecordClientService service) {
        super("成绩登记", "仅登记本人授课课程。");
        this.page = page; this.service = service;
        JPanel form = new JPanel(new GridLayout(0, 2, 12, 8)); form.setOpaque(false);
        form.add(UiFactory.labelledField("选课记录编号", enrollmentId));
        form.add(UiFactory.labelledField("成绩（0-100）", score));
        form.add(UiFactory.labelledField("备注", remark));
        JPanel content = new JPanel(new java.awt.BorderLayout(0, 10)); content.setOpaque(false);
        content.add(form, java.awt.BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton save = new PrimaryButton("提交成绩");
        save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        }); actions.add(state); actions.add(save);
        content.add(actions, java.awt.BorderLayout.SOUTH); setContent(content);
    }

    private void submit() {
        try {
            long id = Long.parseLong(enrollmentId.getText().trim());
            BigDecimal value = new BigDecimal(score.getText().trim());
            final StudentGradeRecordRequest request = new StudentGradeRecordRequest(id, value,
                    null, remark.getText());
            state.setText("正在提交…");
            AsyncTask.run(new AsyncTask.Work<Object>() {
                @Override public Object run() throws Exception { return service.recordGrade(request); }
            }, new AsyncTask.Callback<Object>() {
                @Override public void onSuccess(Object value) {
                    state.setText("已登记"); page.showSuccess("成绩已提交并保存。");
                }
                @Override public void onFailure(Throwable error) {
                    state.setText("提交失败"); page.showError(AsyncTask.message(error));
                }
            });
        } catch (NumberFormatException ex) { state.setText("编号和成绩必须是数字"); }
    }
}
