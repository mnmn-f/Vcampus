package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * 学生端在宿状态与进出记录。
 *
 * <p>在宿状态不是库里存的字段，而是服务端按最近一次进出流水加请假记录实时算出来
 * 的；晚归标记同理，按宿管当前设定的门禁时段判定。因此这两项都会随门禁策略调整
 * 而变化，页面每次刷新看到的都是按当前策略重算的结果。</p>
 */
public final class DormExtStayPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final AsyncPagedTable<AccessRecordExtDto> records;

    private final JPanel statsRow = new JPanel(new BorderLayout());

    public DormExtStayPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        statsRow.setOpaque(false);
        renderStats("加载中…", "—", "—", "—");
        this.records = recordTable();
        add(summary());
        add(records);
        loadStatus();
    }

    public void reload() {
        loadStatus();
        records.reload();
    }

    /**
     * 顶部一行基本信息，按设计稿排成并列的统计位。
     *
     * <p>四个值都是一眼扫过去的东西，用两列表单的形式写会把「当前状态」这种一个词
     * 的值配上半屏宽的空白。并列 + 竖分隔线的密度才对得上它们的信息量。</p>
     */
    private JPanel summary() {
        JButton refresh = new SecondaryButton("刷新状态");
        refresh.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { reload(); }
        });
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.add(DormUi.header("我的在宿状态",
                "按最近一次进出记录实时判定，不是库里存的字段；已批准的离校请假期间显示为离校登记中。",
                DormUi.actions(refresh), false));
        column.add(statsRow);
        column.add(javax.swing.Box.createVerticalStrut(22));
        return column;
    }

    /** 把四个值刷进统计条；状态本身用语义色，一眼看出在不在宿。 */
    private void renderStats(String statusText, String roomText, String exitText, String entryText) {
        statsRow.removeAll();
        java.awt.Color tone = "离宿".equals(statusText) ? DesignTokens.WARNING
                : "在宿".equals(statusText) ? DesignTokens.SUCCESS
                : "离校登记中".equals(statusText) ? DesignTokens.INFO : null;
        JPanel stats = DormUi.stats(new java.awt.Color[]{tone, null, null, null},
                "当前状态", statusText, "",
                "宿舍", roomText, "",
                "最近离宿", exitText, "",
                "最近归宿", entryText, "");
        statsRow.add(stats, BorderLayout.CENTER);
        statsRow.revalidate();
        statsRow.repaint();
    }

    private AsyncPagedTable<AccessRecordExtDto> recordTable() {
        return new AsyncPagedTable<AccessRecordExtDto>("我的进出记录",
                "晚归按宿管设定的门禁时段判定，调整门禁时间后历史记录会按新时段重算。",
                "搜索门禁点",
                new String[]{"全部记录", "仅入宿", "仅离宿"},
                new String[]{"编号", "类型", "时间", "门禁点", "晚归"},
                new AsyncPagedTable.Loader<AccessRecordExtDto>() {
                    @Override
                    public PageSlice<AccessRecordExtDto> load(int p, String keyword, String filter)
                            throws Exception {
                        DormPage<AccessRecordExtDto> value = service.myAccessRecords(
                                new DormPageQuery(p, 20, keyword, typeCode(filter), null, null));
                        return RealUi.page(value);
                    }
                },
                new AsyncPagedTable.RowMapper<AccessRecordExtDto>() {
                    @Override
                    public Object[] values(AccessRecordExtDto row) {
                        return new Object[]{Long.valueOf(row.getId()), typeLabel(row.getRecordType()),
                                RealUi.dateTime(row.getOccurredAt()), RealUi.text(row.getDoorName()),
                                row.isLateReturn() ? "晚归" : ""};
                    }
                }, null);
    }

    private void loadStatus() {
        AsyncTask.run(new AsyncTask.Work<StayStatusDto>() {
            @Override public StayStatusDto run() throws Exception { return service.myStayStatus(); }
        }, new AsyncTask.Callback<StayStatusDto>() {
            @Override public void onSuccess(StayStatusDto value) { show(value); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }

    private void show(StayStatusDto value) {
        if (value == null) {
            renderStats("无在住记录", "—", "—", "—");
            return;
        }
        renderStats(StayStatusDto.statusName(value.getStatus()),
                RealUi.text(value.getBuildingCode()) + " " + RealUi.text(value.getRoomNo()),
                RealUi.dateTime(value.getLastExitAt()),
                RealUi.dateTime(value.getLastEntryAt()));
    }

    private static String typeCode(String filter) {
        if ("仅入宿".equals(filter)) return "ENTRY";
        if ("仅离宿".equals(filter)) return "EXIT";
        return null;
    }

    private static String typeLabel(String value) {
        return "EXIT".equals(value) ? "离宿" : "入宿";
    }
}
