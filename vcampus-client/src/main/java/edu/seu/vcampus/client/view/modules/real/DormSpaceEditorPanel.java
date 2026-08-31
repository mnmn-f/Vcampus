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
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;

import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/** 宿管员楼栋、房间和床位的页面内新增/编辑入口，不提供物理删除。 */
public final class DormSpaceEditorPanel extends SectionCard {
    private final BasePage page;
    private final DormClientService service;
    private final Runnable refresh;
    private final JTextField buildingId = idField();
    private final JTextField buildingCode = field();
    private final JTextField buildingName = field();
    private final JTextField buildingAddress = field();
    private final JComboBox<RealUi.CodeOption> buildingGender = choices("MIXED", "MALE", "FEMALE");
    private final JComboBox<RealUi.CodeOption> buildingStatus = choices("OPEN", "MAINTENANCE", "CLOSED");
    private final JLabel buildingError = UiFactory.muted(" ");
    private final JTextField roomId = idField();
    private final JTextField roomBuildingId = field();
    private final JTextField roomNo = field();
    private final JTextField roomFloor = field();
    private final JTextField roomCapacity = field();
    private final JComboBox<RealUi.CodeOption> roomType = choices("STANDARD", "SUITE", "SPECIAL");
    private final JComboBox<RealUi.CodeOption> roomStatus = choices("AVAILABLE", "FULL", "MAINTENANCE", "CLOSED");
    private final JTextField roomDescription = field();
    private final JLabel roomError = UiFactory.muted(" ");
    private final JTextField bedId = idField();
    private final JTextField bedRoomId = field();
    private final JTextField bedNo = field();
    private final JComboBox<RealUi.CodeOption> bedStatus = choices("AVAILABLE", "OCCUPIED", "MAINTENANCE");
    private final JLabel bedError = UiFactory.muted(" ");

    public DormSpaceEditorPanel(BasePage page, DormClientService service, Runnable refresh) {
        super("空间维护", "维护楼栋、房间和床位；仅支持新增和编辑。");
        if (page == null || service == null || refresh == null) throw new IllegalArgumentException("空间维护依赖不能为空");
        this.page = page; this.service = service; this.refresh = refresh;
        setContent(tabs()); startBuilding(); startRoom(); startBed();
    }

    public void showBuilding(DormBuildingDto value) {
        if (value == null) { startBuilding(); return; }
        buildingId.setText(String.valueOf(value.getId())); buildingCode.setText(RealUi.input(value.getBuildingCode()));
        buildingName.setText(RealUi.input(value.getBuildingName())); buildingAddress.setText(RealUi.input(value.getAddress()));
        buildingGender.setSelectedItem(RealUi.option(value.getGenderPolicy())); buildingStatus.setSelectedItem(RealUi.option(value.getStatus()));
        buildingError.setText(" ");
    }

    public void showRoom(DormRoomDto value) {
        if (value == null) { startRoom(); return; }
        roomId.setText(String.valueOf(value.getId())); roomBuildingId.setText(String.valueOf(value.getBuildingId()));
        roomNo.setText(RealUi.input(value.getRoomNo())); roomFloor.setText(String.valueOf(value.getFloorNo()));
        roomCapacity.setText(String.valueOf(value.getCapacity())); roomType.setSelectedItem(RealUi.option(value.getRoomType()));
        roomStatus.setSelectedItem(RealUi.option(value.getStatus())); roomDescription.setText(RealUi.input(value.getDescription())); roomError.setText(" ");
    }

    public void showBed(DormBedDto value) {
        if (value == null) { startBed(); return; }
        bedId.setText(String.valueOf(value.getId())); bedRoomId.setText(String.valueOf(value.getRoomId()));
        bedNo.setText(RealUi.input(value.getBedNo())); bedStatus.setSelectedItem(RealUi.option(value.getStatus())); bedError.setText(" ");
    }

    private JTabbedPane tabs() {
        JTabbedPane tabs = new JTabbedPane(); tabs.setFont(DesignTokens.regular(13));
        tabs.addTab("楼栋维护", buildingTab()); tabs.addTab("房间维护", roomTab()); tabs.addTab("床位维护", bedTab()); return tabs;
    }

    private JPanel buildingTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("楼栋 ID（自动）", buildingId)); fields.add(UiFactory.labelledField("楼栋编码", buildingCode));
        fields.add(UiFactory.labelledField("楼栋名称", buildingName)); fields.add(UiFactory.labelledField("地址", buildingAddress));
        fields.add(UiFactory.labelledField("性别政策", buildingGender)); fields.add(UiFactory.labelledField("状态", buildingStatus));
        JPanel actions = actions("新建楼栋", "保存楼栋", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startBuilding(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveBuilding(); }
        }, buildingError);
        return tab(fields, actions);
    }

    private JPanel roomTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("房间 ID（自动）", roomId)); fields.add(UiFactory.labelledField("楼栋 ID", roomBuildingId));
        fields.add(UiFactory.labelledField("房间号", roomNo)); fields.add(UiFactory.labelledField("楼层", roomFloor));
        fields.add(UiFactory.labelledField("容量", roomCapacity)); fields.add(UiFactory.labelledField("房间类型", roomType));
        fields.add(UiFactory.labelledField("状态", roomStatus)); fields.add(UiFactory.labelledField("说明", roomDescription));
        JPanel actions = actions("新建房间", "保存房间", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startRoom(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveRoom(); }
        }, roomError);
        return tab(fields, actions);
    }

    private JPanel bedTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("床位 ID（自动）", bedId)); fields.add(UiFactory.labelledField("房间 ID", bedRoomId));
        fields.add(UiFactory.labelledField("床位号", bedNo)); fields.add(UiFactory.labelledField("状态", bedStatus));
        JPanel actions = actions("新建床位", "保存床位", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startBed(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveBed(); }
        }, bedError);
        return tab(fields, actions);
    }

    private static JPanel tab(JPanel fields, JPanel actions) {
        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(fields, BorderLayout.NORTH); content.add(actions, BorderLayout.SOUTH); return content;
    }

    private JPanel actions(String fresh, String save, java.awt.event.ActionListener newAction,
                           java.awt.event.ActionListener saveAction, JLabel error) {
        JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton(fresh); clear.addActionListener(newAction);
        JButton submit = new PrimaryButton(save); submit.addActionListener(saveAction); actions.add(clear); actions.add(submit); actions.add(error); return actions;
    }

    private void startBuilding() { buildingId.setText(""); buildingCode.setText(""); buildingName.setText(""); buildingAddress.setText(""); buildingGender.setSelectedItem(RealUi.option("MIXED")); buildingStatus.setSelectedItem(RealUi.option("OPEN")); buildingError.setText(" "); }
    private void startRoom() { roomId.setText(""); roomBuildingId.setText(""); roomNo.setText(""); roomFloor.setText("1"); roomCapacity.setText("4"); roomType.setSelectedItem(RealUi.option("STANDARD")); roomStatus.setSelectedItem(RealUi.option("AVAILABLE")); roomDescription.setText(""); roomError.setText(" "); }
    private void startBed() { bedId.setText(""); bedRoomId.setText(""); bedNo.setText(""); bedStatus.setSelectedItem(RealUi.option("AVAILABLE")); bedError.setText(" "); }

    private void saveBuilding() {
        try {
            final long id = optionalId(buildingId.getText()); final DormBuildingWriteRequest request = new DormBuildingWriteRequest(id,
                    RealUi.required(buildingCode.getText(), "楼栋编码"), RealUi.required(buildingName.getText(), "楼栋名称"),
                    RealUi.optional(buildingAddress.getText()), RealUi.code(buildingGender.getSelectedItem()), RealUi.code(buildingStatus.getSelectedItem()));
            AsyncTask.run(new AsyncTask.Work<DormBuildingDto>() {
                @Override public DormBuildingDto run() throws Exception { return id == 0L ? service.createBuilding(request) : service.updateBuilding(request); }
            }, new AsyncTask.Callback<DormBuildingDto>() {
                @Override public void onSuccess(DormBuildingDto value) { page.showSuccess("楼栋已保存。"); showBuilding(value); refresh.run(); }
                @Override public void onFailure(Throwable cause) { buildingError.setText(AsyncTask.message(cause)); }
            });
        } catch (IllegalArgumentException ex) { buildingError.setText(ex.getMessage()); }
    }

    private void saveRoom() {
        try {
            final long id = optionalId(roomId.getText()); final int capacity = requiredPositiveInt(roomCapacity.getText(), "容量");
            final DormRoomWriteRequest request = new DormRoomWriteRequest(id,
                    requiredLong(roomBuildingId.getText(), "楼栋 ID"), RealUi.required(roomNo.getText(), "房间号"),
                    requiredInt(roomFloor.getText(), "楼层"), capacity, RealUi.code(roomType.getSelectedItem()),
                    RealUi.code(roomStatus.getSelectedItem()), RealUi.optional(roomDescription.getText()));
            AsyncTask.run(new AsyncTask.Work<DormRoomDto>() {
                @Override public DormRoomDto run() throws Exception { return id == 0L ? service.createRoom(request) : service.updateRoom(request); }
            }, new AsyncTask.Callback<DormRoomDto>() {
                @Override public void onSuccess(DormRoomDto value) { page.showSuccess("房间已保存。"); showRoom(value); refresh.run(); }
                @Override public void onFailure(Throwable cause) { roomError.setText(AsyncTask.message(cause)); }
            });
        } catch (IllegalArgumentException ex) { roomError.setText(ex.getMessage()); }
    }

    private void saveBed() {
        try {
            final long id = optionalId(bedId.getText()); final String status = RealUi.code(bedStatus.getSelectedItem());
            if (id == 0L && "OCCUPIED".equals(status)) throw new IllegalArgumentException("新增床位不能直接设为占用");
            final DormBedWriteRequest request = new DormBedWriteRequest(id,
                    requiredLong(bedRoomId.getText(), "房间 ID"), RealUi.required(bedNo.getText(), "床位号"), status);
            AsyncTask.run(new AsyncTask.Work<DormBedDto>() {
                @Override public DormBedDto run() throws Exception { return id == 0L ? service.createBed(request) : service.updateBed(request); }
            }, new AsyncTask.Callback<DormBedDto>() {
                @Override public void onSuccess(DormBedDto value) { page.showSuccess("床位已保存。"); showBed(value); refresh.run(); }
                @Override public void onFailure(Throwable cause) { bedError.setText(AsyncTask.message(cause)); }
            });
        } catch (IllegalArgumentException ex) { bedError.setText(ex.getMessage()); }
    }

    private static long optionalId(String value) { String text = RealUi.optional(value); return text == null ? 0L : requiredLong(text, "编号"); }
    private static long requiredLong(String value, String label) { Long number = RealUi.number(RealUi.required(value, label)); if (number == null || number.longValue() <= 0L) throw new IllegalArgumentException(label + "必须是正整数"); return number.longValue(); }
    private static int requiredPositiveInt(String value, String label) { int number = requiredInt(value, label); if (number <= 0) throw new IllegalArgumentException(label + "必须大于 0"); return number; }
    private static int requiredInt(String value, String label) { try { int number = Integer.parseInt(RealUi.required(value, label)); if (number < 0) throw new IllegalArgumentException(label + "不能为负数"); return number; } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + "必须是整数"); } }
    private static JComboBox<RealUi.CodeOption> choices(String... codes) { JComboBox<RealUi.CodeOption> box = new JComboBox<RealUi.CodeOption>(RealUi.options(codes)); box.setFont(DesignTokens.regular(13)); return box; }
    private static JTextField field() { return UiFactory.textField(10); }
    private static JTextField idField() { JTextField value = field(); value.setEditable(false); return value; }
}
