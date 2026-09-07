package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormAssignmentRequest;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import org.threeten.bp.LocalDate;

/** 宿管员楼栋、房间、床位查询及分配/调宿/退宿。 */
public final class DormManagerSpacePanel extends JPanel {
    private final BasePage page; private final DormClientService service;
    private final AsyncPagedTable<DormBuildingDto> buildings; private final AsyncPagedTable<DormRoomDto> rooms; private final AsyncPagedTable<DormBedDto> beds;
    private final DormSpaceEditorPanel editor;
    private final JTextField student = UiFactory.textField(8); private final JTextField record = UiFactory.textField(8); private final JTextField bed = UiFactory.textField(8); private final JTextField date = UiFactory.textField(10); private final JTextField reason = UiFactory.textField(14);

    public DormManagerSpacePanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service;
        editor = new DormSpaceEditorPanel(page, service, new Runnable() {
            @Override public void run() { reload(); }
        });
        buildings = buildingTable(); rooms = roomTable(); beds = bedTable(); add(buildings); add(rooms); add(beds); add(editor); add(assignmentForm());
    }

    public void reload() { buildings.reload(); rooms.reload(); beds.reload(); }

    private AsyncPagedTable<DormBuildingDto> buildingTable() {
        return new AsyncPagedTable<DormBuildingDto>("楼栋", "楼栋编码、地址、性别政策和状态。", "搜索楼栋或地址", new String[]{"全部状态", "启用", "停用"}, new String[]{"编码", "名称", "地址", "性别政策", "状态"}, new AsyncPagedTable.Loader<DormBuildingDto>() {
            @Override public PageSlice<DormBuildingDto> load(int p, String k, String f) throws Exception { return buildingSlice(service.buildings(new DormPageQuery(p, 20, k, spaceStatus(f), null, null))); }
        }, new AsyncPagedTable.RowMapper<DormBuildingDto>() {
            @Override public Object[] values(DormBuildingDto row) { return new Object[]{row.getBuildingCode(), row.getBuildingName(), row.getAddress(), RealUi.status(row.getGenderPolicy()), RealUi.status(row.getStatus())}; }
        }, new AsyncPagedTable.SelectionListener<DormBuildingDto>() {
            @Override public void onSelected(DormBuildingDto row) { editor.showBuilding(row); }
        });
    }
    private AsyncPagedTable<DormRoomDto> roomTable() {
        return new AsyncPagedTable<DormRoomDto>("房间", "房间容量、类型和入住床位数。", "搜索房间号或楼栋", new String[]{"全部状态", "可用", "停用"}, new String[]{"楼栋", "房间号", "楼层", "容量", "已住", "类型", "状态"}, new AsyncPagedTable.Loader<DormRoomDto>() {
            @Override public PageSlice<DormRoomDto> load(int p, String k, String f) throws Exception { return roomSlice(service.rooms(new DormPageQuery(p, 20, k, spaceStatus(f), null, null))); }
        }, new AsyncPagedTable.RowMapper<DormRoomDto>() {
            @Override public Object[] values(DormRoomDto row) { return new Object[]{row.getBuildingName(), row.getRoomNo(), row.getFloorNo(), row.getCapacity(), row.getOccupiedBeds(), RealUi.status(row.getRoomType()), RealUi.status(row.getStatus())}; }
        }, new AsyncPagedTable.SelectionListener<DormRoomDto>() {
            @Override public void onSelected(DormRoomDto row) { editor.showRoom(row); }
        });
    }
    private AsyncPagedTable<DormBedDto> bedTable() {
        return new AsyncPagedTable<DormBedDto>("床位", "占用人信息仅管理员可见。", "搜索房间或床位号", new String[]{"全部状态", "空闲", "已占用", "维护中"}, new String[]{"楼栋", "房间", "床位", "状态", "占用人"}, new AsyncPagedTable.Loader<DormBedDto>() {
            @Override public PageSlice<DormBedDto> load(int p, String k, String f) throws Exception { return bedSlice(service.beds(new DormPageQuery(p, 20, k, bedStatus(f), null, null))); }
        }, new AsyncPagedTable.RowMapper<DormBedDto>() {
            @Override public Object[] values(DormBedDto row) { return new Object[]{row.getBuildingCode(), row.getRoomNo(), row.getBedNo(), RealUi.status(row.getStatus()), RealUi.text(row.getOccupantUserId())}; }
        }, new AsyncPagedTable.SelectionListener<DormBedDto>() {
            @Override public void onSelected(DormBedDto value) { if (value != null) { bed.setText(String.valueOf(value.getId())); editor.showBed(value); } }
        });
    }

    private JPanel assignmentForm() {
        SectionCard card = new SectionCard("住宿分配与变更", "分配、调宿或退宿；填写有效日期和目标床位。");
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("学生编号", student)); fields.add(UiFactory.labelledField("当前记录编号", record)); fields.add(UiFactory.labelledField("目标床位编号", bed)); fields.add(UiFactory.labelledField("生效日期", date)); fields.add(UiFactory.labelledField("操作原因", reason)); fields.add(new JPanel());
        JPanel actions = UiFactory.horizontal(8); JButton assign = new PrimaryButton("直接分配"); assign.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { apply("assign"); } }); JButton transfer = new PrimaryButton("调宿"); transfer.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { apply("transfer"); } }); JButton checkout = new DangerButton("退宿"); checkout.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { if (RealUi.confirm(DormManagerSpacePanel.this, "确认办理该学生退宿？")) apply("checkout"); } }); actions.add(assign); actions.add(transfer); actions.add(checkout);
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false); content.add(fields, BorderLayout.CENTER); content.add(actions, BorderLayout.SOUTH); card.setContent(content); JPanel wrap = new JPanel(new BorderLayout()); wrap.setOpaque(false); wrap.add(card, BorderLayout.CENTER); date.setText(LocalDate.now().toString()); return wrap;
    }

    private void apply(final String operation) {
        try {
            Long studentId = RealUi.number(RealUi.required(student.getText(), "学生编号")); if (studentId == null) throw new IllegalArgumentException("学生编号必须是整数");
            Long recordId = RealUi.number(record.getText()); Long bedId = RealUi.number(bed.getText()); LocalDate effective = LocalDate.parse(RealUi.required(date.getText(), "生效日期"));
            if (!"checkout".equals(operation) && bedId == null) throw new IllegalArgumentException("目标床位编号必须是整数");
            final DormAssignmentRequest request = new DormAssignmentRequest(studentId.longValue(), recordId, bedId, effective, RealUi.optional(reason.getText()));
            AsyncTask.run(new AsyncTask.Work<edu.seu.vcampus.common.dto.dorm.AccommodationDto>() {
                @Override public edu.seu.vcampus.common.dto.dorm.AccommodationDto run() throws Exception { return "assign".equals(operation) ? service.assign(request) : "transfer".equals(operation) ? service.transfer(request) : service.checkout(request); }
            }, new AsyncTask.Callback<edu.seu.vcampus.common.dto.dorm.AccommodationDto>() {
                @Override public void onSuccess(edu.seu.vcampus.common.dto.dorm.AccommodationDto value) { page.showSuccess("住宿关系已更新。"); reload(); }
                @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
            });
        } catch (org.threeten.bp.format.DateTimeParseException ex) { page.showWarning("生效日期格式应为 yyyy-MM-dd。"); }
        catch (IllegalArgumentException ex) { page.showWarning(ex.getMessage()); }
    }

    private static PageSlice<DormBuildingDto> buildingSlice(DormPage<DormBuildingDto> value) { return RealUi.page(value); }
    private static PageSlice<DormRoomDto> roomSlice(DormPage<DormRoomDto> value) { return RealUi.page(value); }
    private static PageSlice<DormBedDto> bedSlice(DormPage<DormBedDto> value) { return RealUi.page(value); }
    private static String spaceStatus(String filter) { if ("启用".equals(filter)) return "OPEN"; if ("可用".equals(filter)) return "AVAILABLE"; if ("停用".equals(filter)) return "CLOSED"; return null; }
    private static String bedStatus(String filter) { if ("空闲".equals(filter)) return "AVAILABLE"; if ("已占用".equals(filter)) return "OCCUPIED"; if ("维护中".equals(filter)) return "MAINTENANCE"; return null; }
}
