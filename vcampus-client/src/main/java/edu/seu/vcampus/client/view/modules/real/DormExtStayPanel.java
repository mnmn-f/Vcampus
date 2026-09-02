package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.AccessRecordExtDto;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
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

    private final JLabel status = UiFactory.body("—");
    private final JLabel room = UiFactory.body("—");
    private final JLabel lastExit = UiFactory.body("—");
    private final JLabel lastEntry = UiFactory.body("—");

    public DormExtStayPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.page = page;
        this.service = service;
        this.records = recordTable();
        add(summary());
        add(records);
        loadStatus();
    }

    public void reload() {
        loadStatus();
        records.reload();
    }

    private JPanel summary() {
        SectionCard card = new SectionCard("我的在宿状态",
                "按最近一次进出记录实时判定；已批准的离校请假期间显示为离校登记中。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8));
        fields.setOpaque(false);
        fields.add(UiFactory.labelledField("当前状态", status));
        fields.add(UiFactory.labelledField("宿舍", room));
        fields.add(UiFactory.labelledField("最近离宿", lastExit));
        fields.add(UiFactory.labelledField("最近归宿", lastEntry));

        JPanel line = UiFactory.horizontal(8);
        JButton refresh = new SecondaryButton("刷新状态");
        refresh.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { reload(); }
        });
        line.add(refresh);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(fields, BorderLayout.CENTER);
        content.add(line, BorderLayout.SOUTH);
        card.setContent(content);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
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
            status.setText("无在住记录");
            room.setText("—");
            lastExit.setText("—");
            lastEntry.setText("—");
            return;
        }
        status.setText(StayStatusDto.statusName(value.getStatus()));
        room.setText(RealUi.text(value.getBuildingCode()) + " " + RealUi.text(value.getRoomNo()));
        lastExit.setText(RealUi.dateTime(value.getLastExitAt()));
        lastEntry.setText(RealUi.dateTime(value.getLastEntryAt()));
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
