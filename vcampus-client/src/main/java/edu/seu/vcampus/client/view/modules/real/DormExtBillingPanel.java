package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingRequest;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.threeten.bp.LocalDate;
/** 宿管员抄表录入与水电出账。 */
public final class DormExtBillingPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<MeterReadingDto> readings;
    private final DormExtBillingOperations operations;
    private List<MeterReadingDto> loaded = Collections.emptyList();
    private final JTextField room = UiFactory.textField(8);
    private final JTextField periodStart = UiFactory.textField(10);
    private final JTextField periodEnd = UiFactory.textField(10);
    private final JTextField electricityUnits = UiFactory.textField(8);
    private final JTextField waterUnits = UiFactory.textField(8);
    private final JTextField electricityPrice = UiFactory.textField(8);
    private final JTextField waterPrice = UiFactory.textField(8);
    public DormExtBillingPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.readings = readingTable();
        this.operations = new DormExtBillingOperations(page, service, readings::reload);
        readings.getTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        readings.addAction(billSelected());
        readings.addAction(billPeriod());
        add(readings);
        add(form());
        resetForm();
    }
    public void reload() { readings.reload(); }
    private AsyncPagedTable<MeterReadingDto> readingTable() {
        return new AsyncPagedTable<MeterReadingDto>("抄表读数",
                "选中一行或多行后出账；已出账的读数不可再改。按住 Ctrl 或 Shift 可多选。",
                "搜索房间或楼栋",
                new String[]{"全部读数", "待出账", "已出账"},
                new String[]{"编号", "楼栋", "房间", "账期起", "账期止", "用电量", "用水量", "应缴", "状态"},
                (p, keyword, filter) -> {
                    DormPage<MeterReadingDto> value = service.meterReadings(new DormPageQuery(p, 20, keyword, null, null, null));
                    List<MeterReadingDto> rows = filtered(value.getItems(), filter); loaded = rows;
                    return new PageSlice<MeterReadingDto>(rows, value.getTotalElements(), value.getPageNumber(), value.getPageSize());
                },
                row -> new Object[]{Long.valueOf(row.getId()), RealUi.text(row.getBuildingCode()), RealUi.text(row.getRoomNo()),
                        RealUi.date(row.getPeriodStart()), RealUi.date(row.getPeriodEnd()), RealUi.text(row.getElectricityUnits()),
                        RealUi.text(row.getWaterUnits()), RealUi.text(row.getTotalAmount()), row.isLocked() ? "已出账" : "待出账"},
                this::showReading);
    }
    private JButton billSelected() {
        JButton button = new PrimaryButton("对选中读数出账");
        button.addActionListener(e -> operations.selected(selectedRows()));
        return button;
    }
    private JButton billPeriod() {
        JButton button = new SecondaryButton("对选中账期全部出账");
        button.addActionListener(e -> operations.wholePeriod(selectedRows()));
        return button;
    }
    private JPanel form() {
        SectionCard card = new SectionCard("登记抄表读数",
                "只负责录入；出账在上面的表格里选行执行。选中一条待出账的读数可以直接改。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("房间编号", room));
        fields.add(UiFactory.labelledField("账期开始（yyyy-MM-dd）", periodStart));
        fields.add(UiFactory.labelledField("账期结束（yyyy-MM-dd）", periodEnd));
        fields.add(UiFactory.labelledField("用电量", electricityUnits));
        fields.add(UiFactory.labelledField("用水量", waterUnits));
        fields.add(UiFactory.labelledField("电费单价", electricityPrice));
        fields.add(UiFactory.labelledField("水费单价", waterPrice));
        JPanel actions = UiFactory.horizontal(8);
        JButton reset = new SecondaryButton("清空");
        reset.addActionListener(e -> resetForm());
        JButton save = new PrimaryButton("保存读数");
        save.addActionListener(e -> saveReading());
        actions.add(reset);
        actions.add(save);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(actions, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }
    private void showReading(MeterReadingDto value) {
        if (value == null) { resetForm(); return; }
        room.setText(String.valueOf(value.getRoomId()));
        periodStart.setText(RealUi.date(value.getPeriodStart()));
        periodEnd.setText(RealUi.date(value.getPeriodEnd()));
        electricityUnits.setText(RealUi.input(value.getElectricityUnits()));
        waterUnits.setText(RealUi.input(value.getWaterUnits()));
        electricityPrice.setText(RealUi.input(value.getElectricityPrice()));
        waterPrice.setText(RealUi.input(value.getWaterPrice()));
    }
    private void resetForm() {
        room.setText("");
        periodStart.setText("");
        periodEnd.setText("");
        electricityUnits.setText("");
        waterUnits.setText("");
        electricityPrice.setText("0.60");
        waterPrice.setText("3.50");
    }
    private void saveReading() {
        final MeterReadingRequest request;
        try {
            request = new MeterReadingRequest(requiredRoom(),
                    requiredDate(periodStart.getText(), "账期开始"),
                    requiredDate(periodEnd.getText(), "账期结束"),
                    requiredAmount(electricityUnits.getText(), "用电量"),
                    requiredAmount(waterUnits.getText(), "用水量"),
                    requiredAmount(electricityPrice.getText(), "电费单价"),
                    requiredAmount(waterPrice.getText(), "水费单价"));
        } catch (IllegalArgumentException ex) {
            page.showWarning(ex.getMessage());
            return;
        }
        AsyncTask.run(new AsyncTask.Work<MeterReadingDto>() {
            @Override public MeterReadingDto run() throws Exception {
                return service.saveMeterReading(request);
            }
        }, new AsyncTask.Callback<MeterReadingDto>() {
            @Override public void onSuccess(MeterReadingDto value) {
                page.showSuccess("读数已保存，应缴 " + value.getTotalAmount() + " 元。");
                readings.reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
    private List<MeterReadingDto> selectedRows() {
        int[] indexes = readings.getTable().getSelectedRows();
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        for (int index : indexes) {
            if (index >= 0 && index < loaded.size()) rows.add(loaded.get(index));
        }
        return rows;
    }
    private static List<MeterReadingDto> filtered(List<MeterReadingDto> items, String filter) {
        Boolean wantLocked = null;
        if ("已出账".equals(filter)) wantLocked = Boolean.TRUE;
        if ("待出账".equals(filter)) wantLocked = Boolean.FALSE;
        if (wantLocked == null) return new ArrayList<MeterReadingDto>(items);
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        for (MeterReadingDto item : items) {
            if (item.isLocked() == wantLocked.booleanValue()) rows.add(item);
        }
        return rows;
    }
    private long requiredRoom() {
        Long value = RealUi.number(RealUi.required(room.getText(), "房间编号"));
        if (value == null || value.longValue() <= 0L) {
            throw new IllegalArgumentException("房间编号必须是正整数。");
        }
        return value.longValue();
    }
    private static LocalDate requiredDate(String text, String label) {
        String value = RealUi.required(text, label);
        try {
            return LocalDate.parse(value.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd。");
        }
    }
    private static BigDecimal requiredAmount(String text, String label) {
        String value = RealUi.required(text, label);
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + "必须是数字。");
        }
    }
}
