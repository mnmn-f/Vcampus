package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/** 学生或教师申请教室的行内时段表单。 */
public final class CampusClassroomApplyPanel extends SectionCard {
    public interface Listener { void onApply(ClassroomReservationRequest request); }
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JTextField room = UiFactory.textField(10); private final JTextField purpose = UiFactory.textField(14);
    private final JTextField start = UiFactory.textField(16); private final JTextField end = UiFactory.textField(16);
    private final JLabel error = UiFactory.muted(" "); private final Listener listener;

    public CampusClassroomApplyPanel(Listener listener) {
        super("申请教室", "填写用途和使用时间。"); this.listener = listener;
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        add(fields, "教室编号", room); add(fields, "申请用途", purpose); add(fields, "开始时间", start); add(fields, "结束时间", end);
        JPanel body = new JPanel(new BorderLayout(0, 10)); body.setOpaque(false); body.add(fields, BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8); JButton submit = new PrimaryButton("提交申请"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        }); actions.add(submit); actions.add(error);
        body.add(actions, BorderLayout.SOUTH); setContent(body);
    }

    public void selectRoom(long id) { room.setText(String.valueOf(id)); }

    private void submit() {
        try {
            long roomId = Long.parseLong(RealUi.required(room.getText(), "教室编号"));
            LocalDateTime from = parse(start.getText(), "开始时间"); LocalDateTime to = parse(end.getText(), "结束时间");
            if (!to.isAfter(from)) throw new IllegalArgumentException("结束时间必须晚于开始时间");
            if (listener != null) listener.onApply(new ClassroomReservationRequest(roomId, RealUi.required(purpose.getText(), "申请用途"), from, to));
            error.setText(" ");
        } catch (NumberFormatException ex) { error.setText("教室编号必须是数字"); }
        catch (IllegalArgumentException ex) { error.setText(ex.getMessage()); }
    }

    private static LocalDateTime parse(String value, String label) {
        try { return LocalDateTime.parse(RealUi.required(value, label), FORMAT); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd HH:mm"); }
    }
    private static void add(JPanel panel, String label, java.awt.Component component) { panel.add(UiFactory.labelledField(label, component)); }
}
