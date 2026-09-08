package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.ui.components.TimeSpinnerField;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.FlowLayout;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/**
 * 日期 + 时刻输入：日期点日历选，时刻用滚轮拨。
 *
 * <p>时刻用 {@link JSpinner} 而不是下拉框：来访时间常常要精确到几点几分，下拉框
 * 一屏放不下 24×60 个选项，滚轮拨两下就到了。分钟按 5 分钟一档——登记来访没有
 * 精确到分的必要，档位少一半，拨起来快一倍。</p>
 */
public final class DormDateTimeField extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int MINUTE_STEP = 5;

    private final DormDateField date = new DormDateField(10);
    private final JSpinner hour = new JSpinner(new SpinnerNumberModel(8, 0, 23, 1));
    private final JSpinner minute = new JSpinner(new SpinnerNumberModel(0, 0, 55, MINUTE_STEP));

    public DormDateTimeField() {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        add(date);
        add(TimeSpinnerField.unit(hour, "时", 15, 58, 33));
        add(TimeSpinnerField.unit(minute, "分", 15, 58, 33));
    }

    public void setValue(LocalDateTime value) {
        if (value == null) { date.clear(); return; }
        date.setDate(value.toLocalDate());
        hour.setValue(Integer.valueOf(value.getHour()));
        minute.setValue(Integer.valueOf(value.getMinute() / MINUTE_STEP * MINUTE_STEP));
    }

    public void clear() { date.clear(); }

    /** 取值并在日期没选时报错；时刻永远有值，所以只校验日期。 */
    public LocalDateTime required(String label) {
        LocalDate day = date.required(label);
        return day.atTime(((Number) hour.getValue()).intValue(), ((Number) minute.getValue()).intValue());
    }
}
