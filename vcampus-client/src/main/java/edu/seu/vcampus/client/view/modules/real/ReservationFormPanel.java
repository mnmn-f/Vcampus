package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

/** 学生自习室预约的行内时段表单。 */
public final class ReservationFormPanel extends SectionCard {
    public interface Listener { void onReserve(StudyRoomReservationRequest request); }

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final JTextField roomId = UiFactory.textField(10);
    private final JTextField date = UiFactory.textField(12);
    private final JComboBox<String> startHour = hourBox();
    private final JComboBox<String> startMinute = minuteBox();
    private final JComboBox<String> endHour = hourBox();
    private final JComboBox<String> endMinute = minuteBox();
    private final JLabel error = UiFactory.muted(" ");
    private final Listener listener;

    public ReservationFormPanel(Listener listener) {
        super("预约自习室", "选中房间，填写预约日期，并从下拉框选择开始和结束时间。");
        this.listener = listener;
        roomId.setEditable(false);
        date.setText(LocalDate.now().plusDays(1).format(DATE_FORMAT));
        startHour.setSelectedItem("08");
        startMinute.setSelectedItem("00");
        endHour.setSelectedItem("09");
        endMinute.setSelectedItem("00");

        JPanel fields = new JPanel(new GridLayout(1, 4, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("自习室编号", roomId));
        fields.add(UiFactory.labelledField("预约日期（yyyy-MM-dd）", date));
        fields.add(UiFactory.labelledField("开始时间", timeSelector(startHour, startMinute)));
        fields.add(UiFactory.labelledField("结束时间", timeSelector(endHour, endMinute)));

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        JPanel actions = UiFactory.horizontal(8);
        javax.swing.JButton submit = new PrimaryButton("提交预约");
        submit.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { submit(); }
        });
        actions.add(submit);
        actions.add(error);
        content.add(actions, BorderLayout.SOUTH);
        setContent(content);
    }

    public void selectRoom(long id) {
        roomId.setText(String.valueOf(id));
        error.setText(" ");
    }

    private void submit() {
        try {
            long id = selectedRoomId();
            LocalDate day = selectedDate();
            LocalDateTime from = day.atTime(selected(startHour), selected(startMinute));
            LocalDateTime to = day.atTime(selected(endHour), selected(endMinute));
            if (from.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("开始时间不能早于当前时间");
            }
            if (!to.isAfter(from)) {
                throw new IllegalArgumentException("结束时间必须晚于开始时间");
            }
            if (listener != null) {
                listener.onReserve(new StudyRoomReservationRequest(id, from, to));
            }
            error.setText(" ");
        } catch (IllegalArgumentException ex) {
            error.setText(ex.getMessage() == null ? "预约时间不正确" : ex.getMessage());
        }
    }

    private long selectedRoomId() {
        String value = roomId.getText().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("请先选择自习室");
        try {
            long id = Long.parseLong(value);
            if (id <= 0L) throw new NumberFormatException("not positive");
            return id;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("自习室编号不正确");
        }
    }

    private LocalDate selectedDate() {
        try {
            return LocalDate.parse(date.getText().trim(), DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("预约日期格式应为 yyyy-MM-dd");
        }
    }

    private static int selected(JComboBox<String> box) {
        Object value = box.getSelectedItem();
        if (value == null) throw new IllegalArgumentException("请选择预约时间");
        return Integer.parseInt(value.toString());
    }

    private static JComboBox<String> hourBox() {
        JComboBox<String> box = new JComboBox<String>(values(24));
        box.setMaximumRowCount(12);
        box.setPreferredSize(new Dimension(74, 36));
        return box;
    }

    private static JComboBox<String> minuteBox() {
        JComboBox<String> box = new JComboBox<String>(values(60));
        box.setMaximumRowCount(12);
        box.setPreferredSize(new Dimension(74, 36));
        return box;
    }

    private static String[] values(int count) {
        String[] values = new String[count];
        for (int i = 0; i < count; i++) {
            values[i] = i < 10 ? "0" + i : String.valueOf(i);
        }
        return values;
    }

    private static JPanel timeSelector(JComboBox<String> hour,
                                       JComboBox<String> minute) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.weightx = 1.0;
        left.fill = GridBagConstraints.HORIZONTAL;
        panel.add(hour, left);

        GridBagConstraints colon = new GridBagConstraints();
        colon.gridx = 1;
        colon.insets = new Insets(0, 6, 0, 6);
        panel.add(UiFactory.body(":"), colon);

        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 2;
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        panel.add(minute, right);
        return panel;
    }
}
