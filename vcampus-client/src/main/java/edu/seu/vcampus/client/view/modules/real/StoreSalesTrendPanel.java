package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendPage;
import edu.seu.vcampus.common.dto.store.StoreSalesTrendQuery;
import org.threeten.bp.LocalDate;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** 商店自然日销售趋势与商品销量构成。 */
public final class StoreSalesTrendPanel extends SectionCard {
    private final BasePage page;
    private final StoreClientService service;
    private final DormDateField start = new DormDateField(10);
    private final DormDateField end = new DormDateField(10);
    private final JLabel state = UiFactory.muted(" ");
    private final StoreTrendChart trend = new StoreTrendChart();
    private final StoreSalesPieChart pie = new StoreSalesPieChart();

    public StoreSalesTrendPanel(BasePage page, StoreClientService service) {
        super("销售趋势", "");
        if (page == null || service == null) {
            throw new IllegalArgumentException("趋势依赖不能为空");
        }
        this.page = page;
        this.service = service;
        JPanel content = UiFactory.vertical(10);
        content.add(filters());
        content.add(charts());
        content.add(state);
        setContent(content);
        load();
    }

    public void reload() { load(); }

    private JPanel filters() {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.insets = new Insets(0, 0, 0, 12);
        constraints.gridx = 0;
        fields.add(UiFactory.labelledField("开始日期", start), constraints);
        constraints.gridx = 1;
        fields.add(UiFactory.labelledField("结束日期", end), constraints);
        JButton query = new SecondaryButton("查询");
        query.setPreferredSize(new Dimension(96, 38));
        query.addActionListener(event -> load());
        constraints.gridx = 2;
        constraints.weightx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.SOUTH;
        constraints.insets = new Insets(0, 0, 0, 0);
        fields.add(query, constraints);
        return fields;
    }

    private JPanel charts() {
        JPanel charts = new JPanel(new GridBagLayout());
        charts.setOpaque(false);
        JScrollPane scroll = new JScrollPane(trend,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getHorizontalScrollBar().setUnitIncrement(32);
        scroll.setPreferredSize(new Dimension(720, 330));
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = 0;
        left.weightx = 1;
        left.fill = GridBagConstraints.BOTH;
        left.insets = new Insets(0, 0, 0, 16);
        charts.add(scroll, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = 0;
        right.fill = GridBagConstraints.VERTICAL;
        charts.add(pie, right);
        return charts;
    }

    private void load() {
        final LocalDate from = start.getDate();
        final LocalDate to = end.getDate();
        if (from != null && to != null && to.isBefore(from)) {
            state.setText("结束日期不能早于开始日期");
            return;
        }
        state.setText("正在加载…");
        AsyncTask.run(() -> new Result(
                service.salesTrend(new StoreSalesTrendQuery(from, to)),
                service.salesReport(new StoreSalesQuery(from, to, (Long) null,
                        null, 1, StoreSalesQuery.MAX_PAGE_SIZE))),
                new AsyncTask.Callback<Result>() {
                    @Override public void onSuccess(Result value) {
                        trend.setItems(value.trend == null ? null : value.trend.getItems());
                        pie.setItems(value.sales == null ? null : value.sales.getItems());
                        state.setText(trend.itemCount() == 0
                                ? "暂无销售数据" : "共 " + trend.itemCount() + " 天");
                    }
                    @Override public void onFailure(Throwable error) {
                        state.setText(AsyncTask.message(error));
                        page.showError(AsyncTask.message(error));
                    }
                });
    }

    DormDateField startDateField() { return start; }
    DormDateField endDateField() { return end; }
    StoreTrendChart trendChart() { return trend; }
    StoreSalesPieChart pieChart() { return pie; }

    private static final class Result {
        private final StoreSalesTrendPage trend;
        private final StoreSalesPage sales;
        private Result(StoreSalesTrendPage trend, StoreSalesPage sales) {
            this.trend = trend;
            this.sales = sales;
        }
    }
}
