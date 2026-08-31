package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

/** SRTP 项目创建和编辑表单，学生与教务老师共享字段但身份由系统自动确定。 */
public final class CampusSrtpEditorPanel extends SectionCard {
    public interface Listener { void onSave(SrtpSaveRequest request); }
    private final JTextField code = field(); private final JTextField student = field();
    private final JTextField title = field(); private final JTextField credits = field();
    private final JTextArea description = UiFactory.textArea(3, 28);
    private final JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(
            RealUi.options("SUBMITTED"));
    private final JLabel error = UiFactory.muted(" "); private final Listener listener; private final boolean admin;
    private long id;

    public CampusSrtpEditorPanel(boolean admin, Listener listener) {
        super(admin ? "SRTP 项目维护" : "提交 SRTP 项目", admin ? "维护项目成员并处理审核。" : "仅可提交或修改本人未审核项目。");
        this.admin = admin; this.listener = listener; status.setFont(DesignTokens.regular(13));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        add(fields, "项目编号", code); if (admin) add(fields, "学生编号", student);
        add(fields, "项目名称", title); add(fields, "学分", credits); add(fields, "状态", status);
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false);
        body.add(fields, BorderLayout.NORTH); body.add(UiFactory.labelledField("项目说明", description), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton("新建"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        });
        JButton save = new PrimaryButton("保存项目"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); actions.add(clear); actions.add(save); actions.add(error);
        body.add(actions, BorderLayout.SOUTH); setContent(body); startNew();
    }

    public void startNew() {
        id = 0L; code.setText(""); student.setText(""); title.setText(""); credits.setText(""); description.setText("");
        status.setSelectedItem(RealUi.option("SUBMITTED")); error.setText(" ");
    }

    public void showRecord(SrtpRecordDto value) {
        if (value == null) { startNew(); return; }
        id = value.getId(); code.setText(RealUi.input(value.getProjectCode())); student.setText(String.valueOf(value.getStudentUserId()));
        title.setText(RealUi.input(value.getTitle())); credits.setText(RealUi.input(value.getCredits())); description.setText(RealUi.input(value.getDescription()));
        status.setSelectedItem(RealUi.option(value.getStatus())); error.setText(" ");
    }

    private void save() {
        try {
            BigDecimal credit = credits.getText().trim().isEmpty() ? null : new BigDecimal(credits.getText().trim());
            SrtpSaveRequest request;
            if (admin) {
                Long studentId = RealUi.number(student.getText()); if (studentId == null || studentId.longValue() <= 0L) throw new IllegalArgumentException("学生编号必须是正整数");
                request = new SrtpSaveRequest(id == 0L ? null : Long.valueOf(id), RealUi.required(code.getText(), "项目编号"), studentId,
                        RealUi.required(title.getText(), "项目名称"), RealUi.optional(description.getText()), credit, "SUBMITTED");
            } else {
                request = new SrtpSaveRequest(id == 0L ? null : Long.valueOf(id), RealUi.required(code.getText(), "项目编号"),
                        RealUi.required(title.getText(), "项目名称"), RealUi.optional(description.getText()), credit, "SUBMITTED");
            }
            if (listener != null) listener.onSave(request); error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("学分必须是数字"); }
        catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private static JTextField field() { return UiFactory.textField(12); }
    private static void add(JPanel panel, String label, java.awt.Component field) { panel.add(UiFactory.labelledField(label, field)); }
}
