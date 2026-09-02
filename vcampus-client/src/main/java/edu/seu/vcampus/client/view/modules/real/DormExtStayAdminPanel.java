package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.ext.StayStatusDto;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

/**
 * 宿管员在宿一览。
 *
 * <p>在宿状态不是库里存的字段，而是按每个学生最近一次进出流水加请假记录实时算出来
 * 的，因此它会随门禁策略的调整而变化——门禁时段在「设置」里调。</p>
 */
public final class DormExtStayAdminPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final AsyncPagedTable<StayStatusDto> stays;

    public DormExtStayAdminPanel(BasePage page, DormExtClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.stays = stayTable(service);
        add(stays);
    }

    public void reload() { stays.reload(); }

    private AsyncPagedTable<StayStatusDto> stayTable(final DormExtClientService service) {
        return new AsyncPagedTable<StayStatusDto>("在宿一览",
                "按最近一次进出记录实时判定，不是库里存的状态字段。",
                "搜索楼栋、房间或学号",
                new String[]{"全部状态", "在宿", "离宿", "离校登记中"},
                new String[]{"学生", "楼栋", "房间", "状态", "最近离宿", "最近归宿"},
                new AsyncPagedTable.Loader<StayStatusDto>() {
                    @Override
                    public PageSlice<StayStatusDto> load(int p, String keyword, String filter)
                            throws Exception {
                        return RealUi.page(narrow(service.stayStatuses(), keyword, filter));
                    }
                },
                new AsyncPagedTable.RowMapper<StayStatusDto>() {
                    @Override
                    public Object[] values(StayStatusDto row) {
                        return new Object[]{Long.valueOf(row.getStudentUserId()),
                                RealUi.text(row.getBuildingCode()), RealUi.text(row.getRoomNo()),
                                StayStatusDto.statusName(row.getStatus()),
                                RealUi.dateTime(row.getLastExitAt()),
                                RealUi.dateTime(row.getLastEntryAt())};
                    }
                }, null);
    }

    /**
     * 检索与分页在客户端做。
     *
     * <p>服务端的 {@code dorm.ext.stay.list} 一次返回全部在住学生（在住人数受床位总数
     * 约束，量级可控），没必要为了一个搜索框再跑一趟网络。</p>
     */
    private static DormPage<StayStatusDto> narrow(DormPage<StayStatusDto> value, String keyword,
                                                  String filter) {
        String needle = keyword == null ? "" : keyword.trim().toLowerCase();
        String status = statusCode(filter);
        List<StayStatusDto> rows = new ArrayList<StayStatusDto>();
        for (StayStatusDto item : value.getItems()) {
            if (status != null && !status.equals(item.getStatus())) continue;
            if (needle.length() > 0 && !matches(item, needle)) continue;
            rows.add(item);
        }
        return new DormPage<StayStatusDto>(1, Math.max(1, rows.size()), rows.size(), rows);
    }

    private static boolean matches(StayStatusDto item, String needle) {
        return contains(item.getBuildingCode(), needle) || contains(item.getRoomNo(), needle)
                || contains(String.valueOf(item.getStudentUserId()), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private static String statusCode(String filter) {
        if ("在宿".equals(filter)) return StayStatusDto.IN_DORM;
        if ("离宿".equals(filter)) return StayStatusDto.OUT;
        if ("离校登记中".equals(filter)) return StayStatusDto.LEAVE_REGISTERED;
        return null;
    }
}
