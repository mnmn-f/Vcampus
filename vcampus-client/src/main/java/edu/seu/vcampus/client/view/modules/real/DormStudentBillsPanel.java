package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.DormHomeSummaryDto;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.math.BigDecimal;

/**
 * 学生本人水电分摊账单。
 *
 * <p>表格上面先给一行「本月应缴」的基本信息：进这一页的人第一个问题是「我要交多少」，
 * 而这个数字在表里是要自己把几行加起来才得到的。摘要和账单表来自两次查询，摘要那份
 * 由服务端算好，避免界面上出现分页只取了前 20 行、合计却按整月算的矛盾。</p>
 */
public final class DormStudentBillsPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DormExtClientService ext;
    private final DormUtilityBillsTable bills;
    private final JPanel statsRow = new JPanel(new BorderLayout());

    public DormStudentBillsPanel(BasePage page, DormClientService service, DormExtClientService ext) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.ext = ext;
        statsRow.setOpaque(false);
        renderStats(null);
        add(DormUi.header("本月应缴", "只显示当前住宿关系下的本人分摊；缴费前会再次确认金额。", null, false));
        add(statsRow);
        add(Box.createVerticalStrut(22));
        bills = new DormUtilityBillsTable(page, service, true);
        add(bills);
        loadSummary();
    }

    public void reload() {
        bills.reload();
        loadSummary();
    }

    private void loadSummary() {
        AsyncTask.run(new AsyncTask.Work<DormHomeSummaryDto>() {
            @Override public DormHomeSummaryDto run() throws Exception { return ext.homeSummary(); }
        }, new AsyncTask.Callback<DormHomeSummaryDto>() {
            @Override public void onSuccess(DormHomeSummaryDto value) { renderStats(value); }
            @Override public void onFailure(Throwable error) { renderStats(null); }
        });
    }

    private void renderStats(DormHomeSummaryDto summary) {
        statsRow.removeAll();
        boolean owing = summary != null && summary.getUnpaidBills() > 0;
        String room = summary == null || !summary.isResident() ? "—"
                : RealUi.text(summary.getBuildingName()) + " " + RealUi.text(summary.getRoomNo());
        String amount = summary == null ? "—" : money(summary.getUnpaidAmount());
        String count = summary == null ? "—" : String.valueOf(summary.getUnpaidBills());
        JPanel stats = DormUi.stats(new Color[]{owing ? DesignTokens.WARNING : DesignTokens.SUCCESS, null, null},
                "未缴金额", amount, owing ? "待缴" : "已结清",
                "未缴笔数", count, "笔",
                "宿舍", room, "");
        statsRow.add(stats, BorderLayout.CENTER);
        statsRow.revalidate();
        statsRow.repaint();
    }

    private static String money(BigDecimal value) {
        if (value == null) return "￥0";
        BigDecimal stripped = value.stripTrailingZeros();
        return "￥" + (stripped.scale() <= 0 ? stripped.toBigInteger().toString() : stripped.toPlainString());
    }
}
