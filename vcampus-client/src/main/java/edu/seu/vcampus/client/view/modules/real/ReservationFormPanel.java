package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/** 学生自习室预约的行内时段表单。 */
public final class ReservationFormPanel extends SectionCard {
    public interface Listener { void onReserve(StudyRoomReservationRequest request); }
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final JTextField roomId = UiFactory.textField(12); private final JTextField start = UiFactory.textField(16); private final JTextField end = UiFactory.textField(16);
    private final JLabel error = UiFactory.muted(" "); private final Listener listener;

    public ReservationFormPanel(Listener listener) {
        super("预约自习室", "选中房间后填写同一天的预约起止时间（yyyy-MM-dd HH:mm）。"); this.listener = listener;
        JPanel fields = new JPanel(new GridLayout(1, 3, 12, 8)); fields.setOpaque(false); fields.add(UiFactory.labelledField("自习室编号", roomId)); fields.add(UiFactory.labelledField("开始时间", start)); fields.add(UiFactory.labelledField("结束时间", end));
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.CENTER); JPanel actions = UiFactory.horizontal(8); javax.swing.JButton submit = new PrimaryButton("提交预约"); submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        }); actions.add(submit); actions.add(error); content.add(actions, BorderLayout.SOUTH); setContent(content);
    }

    public void selectRoom(long id) { roomId.setText(String.valueOf(id)); }
    private void submit() {
        try { long id = Long.parseLong(roomId.getText().trim()); LocalDateTime from = LocalDateTime.parse(start.getText().trim(), FORMAT); LocalDateTime to = LocalDateTime.parse(end.getText().trim(), FORMAT); if (!to.isAfter(from)) throw new IllegalArgumentException("结束时间必须晚于开始时间"); if (listener != null) listener.onReserve(new StudyRoomReservationRequest(id, from, to)); error.setText(" "); }
        catch (Exception ex) { error.setText(ex.getMessage() == null ? "预约时间格式不正确" : ex.getMessage()); }
    }
}
