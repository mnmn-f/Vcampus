package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

/** 图书管理员线上资源访问日志，支持资源、用户和时间筛选。 */
public final class LibraryResourceAccessLogsPanel extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final BasePage page;
    private final LibraryClientService service;
    private final JTextField resourceId = UiFactory.textField(10);
    private final JTextField from = UiFactory.textField(16);
    private final JTextField to = UiFactory.textField(16);
    private final AsyncPagedTable<OnlineResourceAccessLogDto> table;
    private volatile FilterState filter = FilterState.empty();

    public LibraryResourceAccessLogsPanel(BasePage page, LibraryClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        this.page = page; this.service = service;
        add(filters());
        table = new AsyncPagedTable<OnlineResourceAccessLogDto>("线上资源访问日志",
                "",
                "请使用上方条件筛选", new String[0],
                new String[]{"资源", "资源编号", "账号", "姓名", "访问时间"},
                new AsyncPagedTable.Loader<OnlineResourceAccessLogDto>() {
                    @Override public PageSlice<OnlineResourceAccessLogDto> load(int number, String keyword, String ignored) throws Exception { return LibraryResourceAccessLogsPanel.this.load(number); }
                }, new AsyncPagedTable.RowMapper<OnlineResourceAccessLogDto>() {
                    @Override public Object[] values(OnlineResourceAccessLogDto row) { return new Object[]{RealUi.text(row.getResourceTitle()), row.getResourceId(),
                            RealUi.text(row.getAccount()), RealUi.text(row.getDisplayName()), RealUi.dateTime(row.getAccessedAt())}; }
                }, null);
        add(table);
    }

    private SectionCard filters() {
        SectionCard card = new SectionCard("访问日志筛选", "按资源、用户和访问时间筛选。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("资源编号", resourceId));
        fields.add(UiFactory.labelledField("开始时间", from));
        fields.add(UiFactory.labelledField("结束时间", to));
        JPanel actions = UiFactory.horizontal(8);
        JButton clear = new SecondaryButton("清除筛选"); clear.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { clear(); }
        });
        JButton apply = new PrimaryButton("应用筛选"); apply.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { apply(); }
        });
        actions.add(clear); actions.add(apply);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH);
        card.setContent(content); return card;
    }

    private PageSlice<OnlineResourceAccessLogDto> load(int pageNumber) throws Exception {
        OnlineResourceAccessLogPage result = service.searchResourceAccessLogs(
                new OnlineResourceAccessLogQuery(filter.resourceId, null,
                        filter.from, filter.to, pageNumber, 20));
        return new PageSlice<OnlineResourceAccessLogDto>(result.getItems(), result.getTotal(),
                result.getPage(), result.getPageSize());
    }

    private void apply() {
        try {
            filter = new FilterState(number(resourceId.getText(), "资源编号"),
                    time(from.getText(), "开始时间"),
                    time(to.getText(), "结束时间"));
            table.reload();
        } catch (IllegalArgumentException ex) { page.showError(ex.getMessage()); }
    }

    private void clear() {
        resourceId.setText(""); from.setText(""); to.setText("");
        filter = FilterState.empty(); table.reload();
    }

    private static Long number(String value, String label) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            long result = Long.parseLong(value.trim());
            if (result <= 0L) throw new NumberFormatException();
            return Long.valueOf(result);
        } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + "必须是正整数"); }
    }

    private static LocalDateTime time(String value, String label) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return LocalDateTime.parse(value.trim(), TIME); }
        catch (RuntimeException ex) { throw new IllegalArgumentException(label + "格式应为 yyyy-MM-dd HH:mm"); }
    }

    private static final class FilterState {
        private final Long resourceId;
        private final LocalDateTime from; private final LocalDateTime to;
        private FilterState(Long resourceId, LocalDateTime from, LocalDateTime to) {
            if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("开始时间不能晚于结束时间");
            this.resourceId = resourceId; this.from = from; this.to = to;
        }
        private static FilterState empty() { return new FilterState(null, null, null); }
    }
}
