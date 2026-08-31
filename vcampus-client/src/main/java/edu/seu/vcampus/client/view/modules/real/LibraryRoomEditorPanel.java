package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.threeten.bp.LocalTime;

/** 图书管理员的自习室开放状态与时段表单。 */
public final class LibraryRoomEditorPanel extends SectionCard implements RealUi.EditorActions {
    public interface Listener { void onSave(StudyRoomUpsertRequest request); }
    private final JTextField building = field(); private final JTextField room = field(); private final JTextField capacity = field();
    private final JTextField open = field(); private final JTextField close = field(); private final JTextArea description = UiFactory.textArea(3, 28);
    private final JComboBox<String> status = new JComboBox<String>(new String[]{"OPEN", "MAINTENANCE", "CLOSED"});
    private final JLabel error = UiFactory.muted(" "); private final Listener listener; private long id;

    public LibraryRoomEditorPanel(Listener listener) {
        super("自习室详情与维护", "维护开放状态和每日开放时段；预约检查时段冲突。"); this.listener = listener; status.setFont(DesignTokens.regular(13)); RealUi.codeRenderer(status);
        JPanel fields = RealUi.editorFields(); add(fields, "楼栋", building); add(fields, "房间号", room); add(fields, "容量", capacity); add(fields, "开放时间（HH:mm）", open); add(fields, "关闭时间（HH:mm）", close); add(fields, "状态", status);
        JPanel actions = RealUi.editorActions("保存自习室", this, error); JPanel content = RealUi.editorContent(fields, description, actions); setContent(content); startNew();
    }

    public void startNew() { id = 0; building.setText(""); room.setText(""); capacity.setText(""); open.setText("08:00"); close.setText("22:00"); description.setText(""); status.setSelectedItem("OPEN"); error.setText(" "); }
    public void showRoom(StudyRoomView value) { if (value == null) { startNew(); return; } id = value.getId(); building.setText(RealUi.input(value.getBuildingName())); room.setText(RealUi.input(value.getRoomNo())); capacity.setText(String.valueOf(value.getCapacity())); open.setText(RealUi.time(value.getOpenTime())); close.setText(RealUi.time(value.getCloseTime())); status.setSelectedItem(value.getStatus()); description.setText(value.getDescription() == null ? "" : value.getDescription()); error.setText(" "); }

    @Override public void save() {
        try { int seats = Integer.parseInt(RealUi.required(capacity.getText(), "容量")); LocalTime from = LocalTime.parse(RealUi.required(open.getText(), "开放时间")); LocalTime to = LocalTime.parse(RealUi.required(close.getText(), "关闭时间")); if (seats <= 0 || !to.isAfter(from)) throw new IllegalArgumentException("容量或开放时段不正确");
            if (listener != null) listener.onSave(new StudyRoomUpsertRequest(id, RealUi.required(building.getText(), "楼栋"), RealUi.required(room.getText(), "房间号"), seats, from, to, String.valueOf(status.getSelectedItem()), RealUi.optional(description.getText()))); error.setText(" ");
        } catch (Exception ex) { error.setText(ex.getMessage() == null ? "参数不正确" : ex.getMessage()); }
    }
    private static JTextField field() { return UiFactory.textField(12); }
    private static void add(JPanel p, String label, java.awt.Component c) { p.add(UiFactory.labelledField(label, c)); }
}
