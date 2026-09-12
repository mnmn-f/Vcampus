package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.AccessRecordRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.threeten.bp.LocalDateTime;

/**
 * 学生端在宿状态与进出记录。
 *
 * <p>在宿状态不是库里存的字段，而是服务端按最近一次进出流水加请假记录实时算出来
 * 的；晚归标记同理，按宿管当前设定的门禁时段判定。因此这两项都会随门禁策略调整
 * 而变化，页面每次刷新看到的都是按当前策略重算的结果。</p>
 *
 * <p>进出流水本来该由门禁闸机自动写入，学生不需要也不应该手填。这里保留一个「自助
 * 登记」入口是为了没有闸机的演示环境：学生点一下「登记出门 / 登记归宿」，服务端照
 * 闸机的方式记一条流水，归宿时刻晚于门禁时段会当场生成晚归预警，宿管端「未归管理」
 * 里立刻能看到——与真实闸机走的是同一条链路。登记人永远是当前登录账号，服务端不接受
 * 客户端指定别人。</p>
 */
public final class DormExtStayPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BasePage page;
    private final DormExtClientService service;
    private final DormClientService dorm;
    private final AsyncPagedTable<AccessRecordExtDto> records;

    private final JPanel statsRow = new JPanel(new BorderLayout());
    private final JTextField doorName = UiFactory.textField(10);
    private final JTextField note = UiFactory.textField(14);

    public DormExtStayPanel(BasePage page, DormClientService dorm, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.dorm = dorm;
        this.service = service;
        statsRow.setOpaque(false);
        renderStats("加载中…", "—", "—", "—");
        this.records = recordTable();
        add(summary());
        if (dorm != null) add(checkInForm());
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

    /** 自助登记使用服务器当前时刻，学生不能修改门禁流水时间。 */
    private JPanel checkInForm() {
        JPanel fields = UiFactory.horizontal(16);
        fields.setOpaque(false);
        fields.add(DormFormUi.field("门禁点（可空）", doorName, 150));
        fields.add(DormFormUi.field("备注（可空）", note, 200));

        JButton exit = new SecondaryButton("登记出门");
        exit.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { record("EXIT"); }
        });
        JButton entry = new PrimaryButton("登记归宿");
        entry.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { record("ENTRY"); }
        });
        JPanel buttons = UiFactory.horizontal(9);
        buttons.add(entry);
        buttons.add(exit);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        fields.setAlignmentX(LEFT_ALIGNMENT);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        rows.add(fields);
        rows.add(javax.swing.Box.createVerticalStrut(14));
        rows.add(buttons);

        JPanel box = DormUi.panel();
        box.add(rows, BorderLayout.CENTER);
        box.setAlignmentX(LEFT_ALIGNMENT);

        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.add(DormUi.header("进出登记",
                "",
                null, false));
        section.add(box);
        section.add(javax.swing.Box.createVerticalStrut(22));
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }

    private void record(final String type) {
        final String door = doorName.getText().trim().isEmpty() ? null : doorName.getText().trim();
        final String remark = note.getText().trim().isEmpty() ? null : note.getText().trim();
        AsyncTask.run(new AsyncTask.Work<AccessRecordDto>() {
            @Override public AccessRecordDto run() throws Exception {
                return dorm.recordAccess(new AccessRecordRequest(type, null, door, "SELF", remark));
            }
        }, new AsyncTask.Callback<AccessRecordDto>() {
            @Override public void onSuccess(AccessRecordDto value) {
                page.showSuccess(("EXIT".equals(type) ? "已登记出门 " : "已登记归宿 ") + RealUi.dateTime(value.getOccurredAt()));
                note.setText("");
                reload();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
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
