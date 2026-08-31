package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.math.BigDecimal;

/** 统一水电账单表格；学生可缴费，宿管只读台账。 */
final class DormUtilityBillsTable extends JPanel {
    private final BasePage page;
    private final DormClientService service;
    private final AsyncPagedTable<UtilityBillDto> bills;
    private final boolean canPay;

    DormUtilityBillsTable(BasePage page, DormClientService service, boolean canPay) {
        super();
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.canPay = canPay;
        bills = createTable();
        add(bills);
    }

    void reload() { bills.reload(); }

    private AsyncPagedTable<UtilityBillDto> createTable() {
        String title = canPay ? "我的水电账单" : "水电账单台账";
        String subtitle = canPay
                ? "仅显示当前住宿关系下的本人分摊，缴费前会再次确认金额。"
                : "查看房间账期和分摊金额；缴费动作由学生本人完成。";
        String[] columns = canPay
                ? new String[]{"分摊编号", "房间", "账期", "电量", "水量", "应缴金额", "状态"}
                : new String[]{"分摊编号", "学生", "房间", "账期", "电量", "水量", "应缴金额", "状态"};
        AsyncPagedTable<UtilityBillDto> table = new AsyncPagedTable<UtilityBillDto>(title,
                subtitle, "搜索房间或月份", new String[]{"全部状态", "待缴费", "已缴费", "已减免"},
                columns,
                new AsyncPagedTable.Loader<UtilityBillDto>() {
                    @Override public PageSlice<UtilityBillDto> load(int p, String keyword, String filter) throws Exception { return loadPage(p, keyword, filter); }
                }, new AsyncPagedTable.RowMapper<UtilityBillDto>() {
                    @Override public Object[] values(UtilityBillDto value) { return row(value, canPay); }
                }, null);
        if (canPay) {
            JButton pay = new PrimaryButton("缴纳选中账单");
            pay.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { pay(); }
            });
            table.addAction(pay);
        }
        return table;
    }

    private PageSlice<UtilityBillDto> loadPage(int pageNumber, String keyword, String filter) throws Exception {
        if (canPay) {
            DormPageQuery query = new DormPageQuery(pageNumber, 20, keyword, status(filter), null, null);
            return RealUi.page(service.bills(query));
        }
        UtilityBillQuery query = new UtilityBillQuery(pageNumber, 20, keyword, status(filter),
                null, null, null);
        return RealUi.page(service.managerBills(query));
    }

    private void pay() {
        UtilityBillDto value = bills.selectedItem();
        if (value == null) {
            page.showWarning("请先选择账单。");
            return;
        }
        if (!RealUi.confirm(this, "确认缴纳 ¥" + moneyValue(value.getAllocatedAmount()) + " 的水电费？")) return;
        final UtilityPaymentRequest request = new UtilityPaymentRequest(value.getAllocationId(),
                "desktop-bill-pay-" + value.getAllocationId());
        AsyncTask.run(new AsyncTask.Work<UtilityBillDto>() {
            @Override public UtilityBillDto run() throws Exception { return service.payBill(request); }
        }, new AsyncTask.Callback<UtilityBillDto>() {
            @Override public void onSuccess(UtilityBillDto result) {
                page.showSuccess("水电费缴纳成功。");
                bills.reload();
            }

            @Override public void onFailure(Throwable error) {
                page.showError(AsyncTask.message(error));
            }
        });
    }

    private static Object[] row(UtilityBillDto value, boolean studentView) {
        Object[] common = new Object[]{value.getAllocationId(), RealUi.text(value.getRoomNo()),
                RealUi.date(value.getPeriodStart()) + " ~ " + RealUi.date(value.getPeriodEnd()),
                RealUi.text(value.getElectricityUnits()), RealUi.text(value.getWaterUnits()),
                money(value.getAllocatedAmount()), RealUi.status(value.getAllocationStatus())};
        if (studentView) return common;
        return new Object[]{common[0], RealUi.text(value.getStudentUserId()), common[1], common[2],
                common[3], common[4], common[5], common[6]};
    }

    private static String status(String filter) {
        if ("待缴费".equals(filter)) return "UNPAID";
        if ("已缴费".equals(filter)) return "PAID";
        if ("已减免".equals(filter)) return "WAIVED";
        return null;
    }

    private static String money(BigDecimal value) {
        return value == null ? "--" : "¥" + moneyValue(value);
    }

    private static String moneyValue(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
