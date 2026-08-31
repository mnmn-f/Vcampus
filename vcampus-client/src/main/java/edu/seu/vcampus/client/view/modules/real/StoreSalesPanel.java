package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.StoreSalesDto;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import org.threeten.bp.LocalDate;

/** 商店管理员销售统计；仅已支付和已完成订单计入汇总。 */
public final class StoreSalesPanel extends JPanel {
    private final StoreClientService service;
    private final JTextField startDate = UiFactory.textField(10);
    private final JTextField endDate = UiFactory.textField(10);
    private final JTextField productId = UiFactory.textField(8);
    private final JLabel error = UiFactory.muted(" ");
    private final JLabel summary = UiFactory.body("筛选汇总：销量 -- · 销售额 --");
    private final AsyncPagedTable<StoreSalesDto> sales;

    public StoreSalesPanel(BasePage page, StoreClientService service) {
        super();
        if (page == null || service == null) throw new IllegalArgumentException("销售统计依赖不能为空");
        this.service = service;
        setOpaque(false);
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        add(filters());
        sales = table();
        add(sales);
    }

    public void reload() { sales.reload(); }

    private JPanel filters() {
        SectionCard card = new SectionCard("销售统计", "按自然日查询；仅统计已支付和已完成的订单，退款不计入销售统计。");
        JPanel fields = new JPanel(new GridLayout(0, 3, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("开始日期", startDate));
        fields.add(UiFactory.labelledField("结束日期", endDate));
        fields.add(UiFactory.labelledField("商品 ID（可选）", productId));
        JPanel actions = UiFactory.horizontal(8);
        JButton query = new SecondaryButton("查询统计");
        query.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { reload(); }
        });
        actions.add(query); actions.add(summary); actions.add(error);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false); content.add(fields, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private AsyncPagedTable<StoreSalesDto> table() {
        return new AsyncPagedTable<StoreSalesDto>("按商品汇总", "按商品关键字筛选。",
                "商品关键字", new String[0],
                new String[]{"商品 ID", "编码", "商品", "销量", "销售额"},
                new AsyncPagedTable.Loader<StoreSalesDto>() {
                    @Override public PageSlice<StoreSalesDto> load(int page, String keyword, String filter) throws Exception { return StoreSalesPanel.this.load(page, keyword); }
                }, new AsyncPagedTable.RowMapper<StoreSalesDto>() {
                    @Override public Object[] values(StoreSalesDto row) { return new Object[]{row.getProductId(), RealUi.text(row.getSku()),
                            RealUi.text(row.getProductName()), row.getQuantitySold(), money(row.getSalesAmount())}; }
                }, null);
    }

    private PageSlice<StoreSalesDto> load(int page, String keyword) throws Exception {
        StoreSalesQuery query = query(page, keyword);
        StoreSalesPage value = service.salesReport(query);
        if (value == null) {
            updateSummary(0L, BigDecimal.ZERO);
            return new PageSlice<StoreSalesDto>(null, 0L, page, query.getPageSize());
        }
        updateSummary(value.getTotalQuantity(), value.getTotalAmount());
        return new PageSlice<StoreSalesDto>(value.getItems(), value.getTotal(), value.getPage(), value.getPageSize());
    }

    private StoreSalesQuery query(int page, String keyword) {
        try {
            LocalDate start = date(startDate.getText(), "开始日期");
            LocalDate end = date(endDate.getText(), "结束日期");
            if (start != null && end != null && end.isBefore(start)) {
                throw new IllegalArgumentException("结束日期不能早于开始日期");
            }
            Long id = RealUi.number(productId.getText());
            if (id == null && productId.getText().trim().length() > 0) {
                throw new IllegalArgumentException("商品 ID 必须是整数");
            }
            clearError();
            return new StoreSalesQuery(start, end, id, RealUi.optional(keyword), page, 20);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            throw ex;
        }
    }

    private static LocalDate date(String text, String label) {
        String value = RealUi.optional(text);
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd"); }
    }

    private void updateSummary(final long quantity, final BigDecimal amount) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { summary.setText("筛选汇总：销量 " + quantity + " · 销售额 " + money(amount)); }
        });
    }

    private void showError(final String text) { SwingUtilities.invokeLater(new Runnable() {
        @Override public void run() { error.setText(RealUi.text(text)); }
    }); }
    private void clearError() { SwingUtilities.invokeLater(new Runnable() {
        @Override public void run() { error.setText(" "); }
    }); }
    private static String money(BigDecimal value) { return value == null ? "¥0.00" : "¥" + value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
}
