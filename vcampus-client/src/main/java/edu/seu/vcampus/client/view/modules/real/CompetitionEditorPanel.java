package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 教务老师比赛发布与编辑的页面内表单。 */
public final class CompetitionEditorPanel extends SectionCard {
    public interface Listener { void onSave(CompetitionSaveRequest request); }
    private final JTextField title = field(); private final DormDateTimeField start = new DormDateTimeField();
    private final DormDateTimeField end = new DormDateTimeField(); private final DormDateTimeField deadline = new DormDateTimeField();
    private final JTextField capacity = field(); private final JTextArea description = UiFactory.textArea(3, 28);
    private final JComboBox<RealUi.CodeOption> status = new JComboBox<RealUi.CodeOption>(
            RealUi.options("DRAFT", "PUBLISHED", "CLOSED", "CANCELLED"));
    private final JLabel error = UiFactory.muted(" "); private final Listener listener; private long id;

    public CompetitionEditorPanel(Listener listener) {
        super("比赛发布与维护", "填写发布时间、报名截止时间和容量。");
        this.listener = listener; status.setFont(DesignTokens.regular(13));
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        add(fields, "比赛名称", title); add(fields, "容量", capacity); add(fields, "开始时间", start);
        add(fields, "结束时间", end); add(fields, "报名截止", deadline); add(fields, "状态", status);
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false);
        body.add(fields, BorderLayout.NORTH); body.add(UiFactory.labelledField("比赛说明", description), BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton("新建"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startNew(); }
        });
        JButton save = new PrimaryButton("保存比赛"); save.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { save(); }
        }); actions.add(clear); actions.add(save); actions.add(error);
        body.add(actions, BorderLayout.SOUTH); setContent(body); startNew();
    }

    public void startNew() {
        id = 0L; title.setText(""); capacity.setText("100"); start.clear(); end.clear(); deadline.clear();
        description.setText(""); status.setSelectedItem(RealUi.option("DRAFT")); error.setText(" ");
    }

    public void showCompetition(CompetitionDto value) {
        if (value == null) { startNew(); return; }
        id = value.getId(); title.setText(RealUi.input(value.getTitle())); capacity.setText(RealUi.input(value.getCapacity()));
        start.setValue(value.getStartAt()); end.setValue(value.getEndAt()); deadline.setValue(value.getRegistrationDeadline());
        description.setText(RealUi.input(value.getDescription())); status.setSelectedItem(RealUi.option(value.getStatus())); error.setText(" ");
    }

    private void save() {
        try {
            Integer seats = Integer.valueOf(RealUi.required(capacity.getText(), "容量"));
            CompetitionSaveRequest request = new CompetitionSaveRequest(id == 0L ? null : Long.valueOf(id),
                    RealUi.required(title.getText(), "比赛名称"), RealUi.optional(description.getText()),
                    start.required("开始时间"), end.required("结束时间"), deadline.required("报名截止"),
                    seats, RealUi.code(status.getSelectedItem()));
            if (listener != null) listener.onSave(request); error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("容量必须是数字"); }
        catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private static JTextField field() { return UiFactory.textField(12); }
    private static void add(JPanel panel, String label, java.awt.Component field) { panel.add(UiFactory.labelledField(label, field)); }
}
