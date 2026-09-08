package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.identity.IdentityPage;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.Locale;

/** 实时页面共用的轻量显示和高风险确认工具。 */
public final class RealUi {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private RealUi() {
    }

    public static String text(Object value) {
        return value == null ? "--" : String.valueOf(value);
    }

    public static String input(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME.format(value);
    }

    public static String date(LocalDate value) {
        return value == null ? "--" : DATE.format(value);
    }

    public static String time(LocalTime value) {
        return value == null ? "--" : TIME.format(value);
    }

    public static String status(String value) {
        return RealUiLabels.status(value);
    }

    public static CodeOption option(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return new CodeOption(normalized, status(normalized));
    }

    public static CodeOption[] options(String... codes) {
        CodeOption[] result = new CodeOption[codes == null ? 0 : codes.length];
        for (int i = 0; i < result.length; i++) result[i] = option(codes[i]);
        return result;
    }

    public static String code(Object value) {
        return value instanceof CodeOption ? ((CodeOption) value).getCode() : text(value);
    }

    /** 让仍以枚举/字符串存储的旧表单显示中文，同时保持提交值不变。 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void codeRenderer(JComboBox<?> box) {
        if (box == null) return;
        ((JComboBox) box).setRenderer(new DefaultListCellRenderer() {
            @Override public java.awt.Component getListCellRendererComponent(JList list, Object value,
                    int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                String code = value instanceof Enum ? ((Enum) value).name() : String.valueOf(value);
                setText(status(code)); return this;
            }
        });
    }

    /** 下拉框只显示用户文案，提交时保留内部值。 */
    public static final class CodeOption {
        private final String code;
        private final String label;

        private CodeOption(String code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getCode() { return code; }
        @Override public String toString() { return label; }
        @Override public boolean equals(Object other) {
            return other instanceof CodeOption && code.equals(((CodeOption) other).code);
        }
        @Override public int hashCode() { return code.hashCode(); }
    }

    public static Long number(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return Long.valueOf(value.trim()); } catch (NumberFormatException ex) { return null; }
    }

    public static String required(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    public static String optional(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    public static <T> PageSlice<T> page(DormPage<T> value) {
        if (value == null) return new PageSlice<T>(null, 0L, 1, 20);
        return new PageSlice<T>(value.getItems(), value.getTotalElements(),
                value.getPageNumber(), value.getPageSize());
    }

    public static <T> PageSlice<T> page(CampusPage<T> value) {
        if (value == null) return new PageSlice<T>(null, 0L, 1, 20);
        return new PageSlice<T>(value.getItems(), value.getTotalElements(),
                value.getPageNumber(), value.getPageSize());
    }

    public static <T> PageSlice<T> page(IdentityPage<T> value) {
        if (value == null) return new PageSlice<T>(null, 0L, 1, 20);
        return new PageSlice<T>(value.getItems(), value.getTotalElements(),
                value.getPageNumber(), value.getPageSize());
    }

    public static JPanel editorFields() {
        JPanel fields = new JPanel(new java.awt.GridLayout(0, 2, 12, 8)); fields.setOpaque(false); return fields;
    }

    public static JPanel editorContent(JPanel fields, JComponent description, JPanel actions) {
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.NORTH);
        content.add(UiFactory.labelledField("说明", description), BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        return content;
    }

    public interface EditorActions {
        void startNew();
        void save();
    }

    public static JPanel editorActions(String saveLabel, final EditorActions target, JLabel error) {
        JPanel actions = UiFactory.horizontal(8);
        JButton clear = new SecondaryButton("新建");
        clear.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { target.startNew(); }
        });
        JButton save = new PrimaryButton(saveLabel);
        save.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent event) { target.save(); }
        });
        actions.add(clear);
        actions.add(save);
        actions.add(error);
        return actions;
    }

    public static Object[] leaveRow(LeaveRequestDto row, boolean includeStudent) {
        Object[] values = new Object[includeStudent ? 8 : 7];
        int index = 0;
        values[index++] = row.getId();
        if (includeStudent) values[index++] = row.getStudentUserId();
        values[index++] = status(row.getLeaveType());
        values[index++] = dateTime(row.getStartAt());
        values[index++] = dateTime(row.getEndAt());
        values[index++] = text(row.getReason());
        values[index++] = status(row.getStatus());
        values[index] = text(row.getReviewRemark());
        return values;
    }

    public static void accent(JComponent component) {
        component.setForeground(DesignTokens.TEXT_PRIMARY);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "请确认此操作",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }
}
