package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SecondaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

/** 宿管员维护楼栋、房间和床位；关联对象从已存在的空间中选择。 */
public final class DormSpaceEditorPanel extends SectionCard {
    private final BasePage page;
    private final DormClientService service;
    private final Runnable refresh;
    private long buildingId;
    private long roomId;
    private long bedId;
    private long roomBuildingSelection;
    private long bedRoomSelection;
    private final JTextField buildingCode = field();
    private final JTextField buildingName = field();
    private final JTextField buildingAddress = field();
    private final JComboBox<RealUi.CodeOption> buildingGender = choices("MALE", "FEMALE");
    private final JComboBox<RealUi.CodeOption> buildingStatus = choices("OPEN", "MAINTENANCE", "CLOSED");
    private final JLabel buildingError = UiFactory.muted(" ");
    private final JComboBox<SpaceOption> roomBuilding = new JComboBox<SpaceOption>();
    private final JTextField roomNo = field();
    private final JTextField roomFloor = field();
    private final JTextField roomCapacity = field();
    private final JComboBox<RealUi.CodeOption> roomType = choices("STANDARD", "SUITE", "SPECIAL");
    private final JComboBox<RealUi.CodeOption> roomStatus = choices("AVAILABLE", "FULL", "MAINTENANCE", "CLOSED");
    private final JTextField roomDescription = field();
    private final JLabel roomError = UiFactory.muted(" ");
    private final JComboBox<SpaceOption> bedRoom = new JComboBox<SpaceOption>();
    private final JTextField bedNo = field();
    private final JComboBox<RealUi.CodeOption> bedStatus = choices("AVAILABLE", "OCCUPIED", "MAINTENANCE");
    private final JLabel bedError = UiFactory.muted(" ");

    public DormSpaceEditorPanel(BasePage page, DormClientService service, Runnable refresh) {
        super("空间维护", "维护楼栋、房间与床位");
        if (page == null || service == null || refresh == null) throw new IllegalArgumentException("空间维护依赖不能为空");
        this.page = page; this.service = service; this.refresh = refresh;
        setContent(tabs()); startBuilding(); startRoom(); startBed(); loadReferences();
    }

    public void showBuilding(DormBuildingDto value) {
        if (value == null) { startBuilding(); return; }
        buildingId = value.getId(); buildingCode.setText(RealUi.input(value.getBuildingCode()));
        buildingName.setText(RealUi.input(value.getBuildingName())); buildingAddress.setText(RealUi.input(value.getAddress()));
        buildingGender.setSelectedItem(RealUi.option(value.getGenderPolicy())); buildingStatus.setSelectedItem(RealUi.option(value.getStatus()));
        buildingError.setText(" ");
    }

    public void showRoom(DormRoomDto value) {
        if (value == null) { startRoom(); return; }
        roomId = value.getId(); roomBuildingSelection = value.getBuildingId(); select(roomBuilding, roomBuildingSelection, "当前楼栋");
        roomNo.setText(RealUi.input(value.getRoomNo())); roomFloor.setText(String.valueOf(value.getFloorNo()));
        roomCapacity.setText(String.valueOf(value.getCapacity())); roomType.setSelectedItem(RealUi.option(value.getRoomType()));
        roomStatus.setSelectedItem(RealUi.option(value.getStatus())); roomDescription.setText(RealUi.input(value.getDescription())); roomError.setText(" ");
    }

    public void showBed(DormBedDto value) {
        if (value == null) { startBed(); return; }
        bedId = value.getId(); bedRoomSelection = value.getRoomId(); select(bedRoom, bedRoomSelection, "当前房间");
        bedNo.setText(RealUi.input(value.getBedNo())); bedStatus.setSelectedItem(RealUi.option(value.getStatus())); bedError.setText(" ");
    }

    private JTabbedPane tabs() {
        JTabbedPane tabs = new JTabbedPane(); tabs.setFont(DesignTokens.regular(13));
        tabs.addTab("楼栋维护", buildingTab()); tabs.addTab("房间维护", roomTab()); tabs.addTab("床位维护", bedTab()); return tabs;
    }

    private JPanel buildingTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("楼栋编码", buildingCode)); fields.add(UiFactory.labelledField("楼栋名称", buildingName));
        fields.add(UiFactory.labelledField("地址", buildingAddress)); fields.add(UiFactory.labelledField("性别政策", buildingGender));
        fields.add(UiFactory.labelledField("状态", buildingStatus));
        return tab(fields, actions("新建楼栋", "保存楼栋", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startBuilding(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveBuilding(); }
        }, buildingError));
    }

    private JPanel roomTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("所属楼栋", roomBuilding)); fields.add(UiFactory.labelledField("房间号", roomNo));
        fields.add(UiFactory.labelledField("楼层", roomFloor)); fields.add(UiFactory.labelledField("容量", roomCapacity));
        fields.add(UiFactory.labelledField("房间类型", roomType)); fields.add(UiFactory.labelledField("状态", roomStatus));
        fields.add(UiFactory.labelledField("说明", roomDescription));
        return tab(fields, actions("新建房间", "保存房间", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startRoom(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveRoom(); }
        }, roomError));
    }

    private JPanel bedTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("所属房间", bedRoom)); fields.add(UiFactory.labelledField("床位号", bedNo));
        fields.add(UiFactory.labelledField("状态", bedStatus));
        return tab(fields, actions("新建床位", "保存床位", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startBed(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveBed(); }
        }, bedError));
    }

    private void loadReferences() {
        AsyncTask.run(new AsyncTask.Work<SpaceReferences>() {
            @Override public SpaceReferences run() throws Exception {
                DormPage<DormBuildingDto> b = service.buildings(new DormPageQuery(1, 200, null, null, null, null));
                DormPage<DormRoomDto> r = service.rooms(new DormPageQuery(1, 500, null, null, null, null));
                return new SpaceReferences(b == null ? null : b.getItems(), r == null ? null : r.getItems());
            }
        }, new AsyncTask.Callback<SpaceReferences>() {
            @Override public void onSuccess(SpaceReferences value) { fillReferences(value); }
            @Override public void onFailure(Throwable error) { fillReferences(null); }
        });
    }

    private void fillReferences(SpaceReferences value) {
        roomBuilding.removeAllItems(); roomBuilding.addItem(SpaceOption.empty("请选择楼栋"));
        bedRoom.removeAllItems(); bedRoom.addItem(SpaceOption.empty("请选择房间"));
        if (value != null) {
            if (value.buildings != null) for (DormBuildingDto b : value.buildings) roomBuilding.addItem(
                    new SpaceOption(b.getId(), RealUi.text(b.getBuildingName()) + "（" + RealUi.text(b.getBuildingCode()) + "）"));
            if (value.rooms != null) for (DormRoomDto r : value.rooms) bedRoom.addItem(
                    new SpaceOption(r.getId(), RealUi.text(r.getBuildingName()) + " " + RealUi.text(r.getRoomNo())));
        }
        select(roomBuilding, roomBuildingSelection, "当前楼栋"); select(bedRoom, bedRoomSelection, "当前房间");
    }

    private void startBuilding() { buildingId = 0L; buildingCode.setText(""); buildingName.setText(""); buildingAddress.setText(""); buildingGender.setSelectedItem(RealUi.option("MALE")); buildingStatus.setSelectedItem(RealUi.option("OPEN")); buildingError.setText(" "); }
    private void startRoom() { roomId = 0L; roomBuildingSelection = 0L; select(roomBuilding, 0L, ""); roomNo.setText(""); roomFloor.setText("1"); roomCapacity.setText("4"); roomType.setSelectedItem(RealUi.option("STANDARD")); roomStatus.setSelectedItem(RealUi.option("AVAILABLE")); roomDescription.setText(""); roomError.setText(" "); }
    private void startBed() { bedId = 0L; bedRoomSelection = 0L; select(bedRoom, 0L, ""); bedNo.setText(""); bedStatus.setSelectedItem(RealUi.option("AVAILABLE")); bedError.setText(" "); }

    private void saveBuilding() {
        try { final long id = buildingId; final DormBuildingWriteRequest request = new DormBuildingWriteRequest(id, RealUi.required(buildingCode.getText(), "楼栋编码"), RealUi.required(buildingName.getText(), "楼栋名称"), RealUi.optional(buildingAddress.getText()), RealUi.code(buildingGender.getSelectedItem()), RealUi.code(buildingStatus.getSelectedItem()));
            AsyncTask.run(new AsyncTask.Work<DormBuildingDto>() { @Override public DormBuildingDto run() throws Exception { return id == 0L ? service.createBuilding(request) : service.updateBuilding(request); } }, new AsyncTask.Callback<DormBuildingDto>() { @Override public void onSuccess(DormBuildingDto value) { page.showSuccess("楼栋已保存。"); showBuilding(value); refresh.run(); loadReferences(); } @Override public void onFailure(Throwable cause) { buildingError.setText(AsyncTask.message(cause)); } });
        } catch (IllegalArgumentException ex) { buildingError.setText(ex.getMessage()); }
    }

    private void saveRoom() {
        try { final long id = roomId; final SpaceOption parent = selected(roomBuilding, "所属楼栋", roomError); if (parent == null) return; final DormRoomWriteRequest request = new DormRoomWriteRequest(id, parent.id, RealUi.required(roomNo.getText(), "房间号"), requiredInt(roomFloor.getText(), "楼层"), requiredPositiveInt(roomCapacity.getText(), "容量"), RealUi.code(roomType.getSelectedItem()), RealUi.code(roomStatus.getSelectedItem()), RealUi.optional(roomDescription.getText()));
            AsyncTask.run(new AsyncTask.Work<DormRoomDto>() { @Override public DormRoomDto run() throws Exception { return id == 0L ? service.createRoom(request) : service.updateRoom(request); } }, new AsyncTask.Callback<DormRoomDto>() { @Override public void onSuccess(DormRoomDto value) { page.showSuccess("房间已保存。"); showRoom(value); refresh.run(); loadReferences(); } @Override public void onFailure(Throwable cause) { roomError.setText(AsyncTask.message(cause)); } });
        } catch (IllegalArgumentException ex) { roomError.setText(ex.getMessage()); }
    }

    private void saveBed() {
        try { final long id = bedId; final SpaceOption parent = selected(bedRoom, "所属房间", bedError); if (parent == null) return; final String status = RealUi.code(bedStatus.getSelectedItem()); if (id == 0L && "OCCUPIED".equals(status)) throw new IllegalArgumentException("新增床位不能直接设为占用"); final DormBedWriteRequest request = new DormBedWriteRequest(id, parent.id, RealUi.required(bedNo.getText(), "床位号"), status);
            AsyncTask.run(new AsyncTask.Work<DormBedDto>() { @Override public DormBedDto run() throws Exception { return id == 0L ? service.createBed(request) : service.updateBed(request); } }, new AsyncTask.Callback<DormBedDto>() { @Override public void onSuccess(DormBedDto value) { page.showSuccess("床位已保存。"); showBed(value); refresh.run(); loadReferences(); } @Override public void onFailure(Throwable cause) { bedError.setText(AsyncTask.message(cause)); } });
        } catch (IllegalArgumentException ex) { bedError.setText(ex.getMessage()); }
    }

    private static SpaceOption selected(JComboBox<SpaceOption> box, String label, JLabel error) { SpaceOption value = (SpaceOption) box.getSelectedItem(); if (value == null || value.id <= 0L) { error.setText("请选择" + label); return null; } return value; }
    private static void select(JComboBox<SpaceOption> box, long id, String fallback) { if (id <= 0L) { if (box.getItemCount() > 0) box.setSelectedIndex(0); return; } for (int i = 0; i < box.getItemCount(); i++) if (((SpaceOption) box.getItemAt(i)).id == id) { box.setSelectedIndex(i); return; } box.addItem(new SpaceOption(id, fallback)); box.setSelectedIndex(box.getItemCount() - 1); }
    private static int requiredPositiveInt(String value, String label) { int n = requiredInt(value, label); if (n <= 0) throw new IllegalArgumentException(label + "必须大于 0"); return n; }
    private static int requiredInt(String value, String label) { try { int n = Integer.parseInt(RealUi.required(value, label)); if (n < 0) throw new IllegalArgumentException(label + "不能为负数"); return n; } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + "必须是整数"); } }
    private static JPanel tab(JPanel fields, JPanel actions) { JPanel stack = new JPanel(); stack.setOpaque(false); stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS)); stack.add(fields); stack.add(Box.createVerticalStrut(14)); stack.add(actions); JPanel content = new JPanel(new BorderLayout()); content.setOpaque(false); content.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 2, 4, 2)); content.add(stack, BorderLayout.NORTH); return content; }
    private JPanel actions(String fresh, String save, java.awt.event.ActionListener newAction, java.awt.event.ActionListener saveAction, JLabel error) { JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton(fresh); clear.addActionListener(newAction); JButton submit = new PrimaryButton(save); submit.addActionListener(saveAction); actions.add(clear); actions.add(submit); actions.add(error); return actions; }
    private static JComboBox<RealUi.CodeOption> choices(String... codes) { JComboBox<RealUi.CodeOption> box = new JComboBox<RealUi.CodeOption>(RealUi.options(codes)); box.setFont(DesignTokens.regular(13)); return box; }
    private static JTextField field() { return UiFactory.textField(10); }

    private static final class SpaceOption { private final long id; private final String label; SpaceOption(long id, String label) { this.id = id; this.label = label; } static SpaceOption empty(String label) { return new SpaceOption(0L, label); } @Override public String toString() { return label; } }
    private static final class SpaceReferences { private final List<DormBuildingDto> buildings; private final List<DormRoomDto> rooms; SpaceReferences(List<DormBuildingDto> b, List<DormRoomDto> r) { buildings = b; rooms = r; } }
}
