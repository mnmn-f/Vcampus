package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * 宿管员的住宿与空间：楼栋、房间、床位三张台账，外加它们的编辑表单。
 *
 * <p>这一页只回答「有多少楼、多少房、多少床，哪些还空着」，以及维护这些基础数据。
 * 「把某个学生放到哪张床上」不在这里——那是一条申请的结局，属于「申请与审批」，
 * 房间平面图也跟着长在那一页。同一件事有两个入口，只会让人不知道该在哪儿做，
 * 也让这一页的主线被一个跟台账无关的表单挤到下面。</p>
 *
 * <p>三张表自上而下从粗到细：楼栋、房间、床位。编辑表单排在最后，它是维护动作，
 * 不是每天都要看的东西。</p>
 */
public final class DormManagerSpacePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final DormClientService service;
    private final AsyncPagedTable<DormBuildingDto> buildings;
    private final AsyncPagedTable<DormRoomDto> rooms;
    private final AsyncPagedTable<DormBedDto> beds;
    private final DormSpaceEditorPanel editor;
    private final JPanel statsRow = new JPanel(new BorderLayout());

    public DormManagerSpacePanel(BasePage page, DormClientService service) {
        super();
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.service = service;
        statsRow.setOpaque(false);
        editor = new DormSpaceEditorPanel(page, service, new Runnable() {
            @Override public void run() { reload(); }
        });
        buildings = buildingTable();
        rooms = roomTable();
        beds = bedTable();

        renderStats(-1, -1, -1, -1);
        add(DormUi.header("住宿与空间",
                "楼栋、房间与床位的全量台账；选中一行可在下方编辑。分配床位在「申请与审批」里做。",
                null, false));
        add(statsRow);
        add(Box.createVerticalStrut(22));
        add(buildings);
        add(Box.createVerticalStrut(24));
        add(rooms);
        add(Box.createVerticalStrut(24));
        add(beds);
        add(Box.createVerticalStrut(24));
        add(editor);
        loadStats();
    }

    public void reload() {
        buildings.reload();
        rooms.reload();
        beds.reload();
        loadStats();
    }

    // ---------- 顶部统计 ----------

    private void loadStats() {
        AsyncTask.run(new AsyncTask.Work<long[]>() {
            @Override public long[] run() throws Exception {
                // 只要总数，所以每条查询取一行；四次小查询比在客户端拉全量再数便宜得多。
                return new long[]{
                        total(service.buildings(new DormPageQuery(1, 1, null, null, null, null))),
                        total(service.rooms(new DormPageQuery(1, 1, null, null, null, null))),
                        total(service.beds(new DormPageQuery(1, 1, null, null, null, null))),
                        total(service.beds(new DormPageQuery(1, 1, null, "AVAILABLE", null, null)))};
            }
        }, new AsyncTask.Callback<long[]>() {
            @Override public void onSuccess(long[] value) { renderStats(value[0], value[1], value[2], value[3]); }
            @Override public void onFailure(Throwable error) { renderStats(-1, -1, -1, -1); }
        });
    }

    private static long total(DormPage<?> value) {
        return value == null ? 0L : value.getTotal();
    }

    private void renderStats(long buildingCount, long roomCount, long bedCount, long freeBeds) {
        statsRow.removeAll();
        statsRow.add(DormUi.stats(new java.awt.Color[]{null, null, null,
                        freeBeds == 0 ? DesignTokens.WARNING : DesignTokens.SUCCESS},
                "楼栋", number(buildingCount), "栋",
                "房间", number(roomCount), "间",
                "床位", number(bedCount), "张",
                "空闲床位", number(freeBeds), "张"), BorderLayout.CENTER);
        statsRow.revalidate();
        statsRow.repaint();
    }

    private static String number(long value) {
        return value < 0 ? "—" : String.valueOf(value);
    }

    // ---------- 表格 ----------

    private AsyncPagedTable<DormBuildingDto> buildingTable() {
        return new AsyncPagedTable<DormBuildingDto>("楼栋", "楼栋编码、地址、性别政策和状态。", "搜索楼栋或地址",
                new String[]{"全部状态", "启用", "停用"}, new String[]{"编码", "名称", "地址", "性别政策", "状态"},
                new AsyncPagedTable.Loader<DormBuildingDto>() {
                    @Override public PageSlice<DormBuildingDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.buildings(new DormPageQuery(p, 20, k, spaceStatus(f), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<DormBuildingDto>() {
                    @Override public Object[] values(DormBuildingDto row) {
                        return new Object[]{row.getBuildingCode(), row.getBuildingName(), row.getAddress(),
                                RealUi.status(row.getGenderPolicy()), RealUi.status(row.getStatus())};
                    }
                }, new AsyncPagedTable.SelectionListener<DormBuildingDto>() {
                    @Override public void onSelected(DormBuildingDto row) { editor.showBuilding(row); }
                });
    }

    private AsyncPagedTable<DormRoomDto> roomTable() {
        return new AsyncPagedTable<DormRoomDto>("房间", "选中一间房可在下方编辑它的容量、类型和状态。", "搜索房间号或楼栋",
                new String[]{"全部状态", "可用", "停用"},
                new String[]{"楼栋", "房间号", "楼层", "容量", "已住", "类型", "状态"},
                new AsyncPagedTable.Loader<DormRoomDto>() {
                    @Override public PageSlice<DormRoomDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.rooms(new DormPageQuery(p, 20, k, spaceStatus(f), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<DormRoomDto>() {
                    @Override public Object[] values(DormRoomDto row) {
                        return new Object[]{row.getBuildingName(), row.getRoomNo(), Integer.valueOf(row.getFloorNo()),
                                Integer.valueOf(row.getCapacity()), Integer.valueOf(row.getOccupiedBeds()),
                                RealUi.status(row.getRoomType()), RealUi.status(row.getStatus())};
                    }
                }, new AsyncPagedTable.SelectionListener<DormRoomDto>() {
                    @Override public void onSelected(DormRoomDto row) { editor.showRoom(row); }
                });
    }

    private AsyncPagedTable<DormBedDto> bedTable() {
        return new AsyncPagedTable<DormBedDto>("床位台账", "占用人信息仅管理员可见。", "搜索房间或床位号",
                new String[]{"全部状态", "空闲", "已占用", "维护中"},
                new String[]{"楼栋", "房间", "床位", "状态", "占用人"},
                new AsyncPagedTable.Loader<DormBedDto>() {
                    @Override public PageSlice<DormBedDto> load(int p, String k, String f) throws Exception {
                        return RealUi.page(service.beds(new DormPageQuery(p, 20, k, bedStatus(f), null, null)));
                    }
                }, new AsyncPagedTable.RowMapper<DormBedDto>() {
                    @Override public Object[] values(DormBedDto row) {
                        return new Object[]{row.getBuildingCode(), row.getRoomNo(), row.getBedNo(),
                                RealUi.status(row.getStatus()), RealUi.text(row.getOccupantUserId())};
                    }
                }, new AsyncPagedTable.SelectionListener<DormBedDto>() {
                    @Override public void onSelected(DormBedDto value) {
                        if (value == null) return;
                        editor.showBed(value);
                    }
                });
    }

    private static String spaceStatus(String filter) {
        if ("启用".equals(filter)) return "OPEN";
        if ("可用".equals(filter)) return "AVAILABLE";
        if ("停用".equals(filter)) return "CLOSED";
        return null;
    }

    private static String bedStatus(String filter) {
        if ("空闲".equals(filter)) return "AVAILABLE";
        if ("已占用".equals(filter)) return "OCCUPIED";
        if ("维护中".equals(filter)) return "MAINTENANCE";
        return null;
    }
}
