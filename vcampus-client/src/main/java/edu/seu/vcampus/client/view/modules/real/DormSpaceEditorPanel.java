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

/**
 * 宿管员维护楼栋、房间和床位；关联对象从已存在的空间中选择。
 *
 * <p>三个子页各是一张表单，同一张表单既做新建也做修改，靠表单顶上的模式提示区分：
 * 没有选中任何行时是「新建」，在上面的台账里点了一行就切成「编辑那一行」。
 * 主按钮的字随模式变（「新建楼栋」/「保存修改」），免得像原来那样两个按钮都叫
 * 「新建」「保存」，分不清哪个才是真的往数据库里写。</p>
 */
public final class DormSpaceEditorPanel extends SectionCard {
    /** 保存成功后通知外面的页面：哪一类记录、保存后的样子、是不是新建的。 */
    public interface Listener {
        void buildingSaved(DormBuildingDto value, boolean created);
        void roomSaved(DormRoomDto value, boolean created);
        void bedSaved(DormBedDto value, boolean created);
    }

    private final BasePage page;
    private final DormClientService service;
    private final Listener listener;
    private final JLabel buildingMode = UiFactory.muted(" ");
    private final JLabel roomMode = UiFactory.muted(" ");
    private final JLabel bedMode = UiFactory.muted(" ");
    private JButton buildingSubmit;
    private JButton roomSubmit;
    private JButton bedSubmit;
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

    public DormSpaceEditorPanel(BasePage page, DormClientService service, Listener listener) {
        super("空间维护", "新建：直接填表单点「新建」；修改：先在上面的台账里选中一行，改完点「保存修改」。");
        if (page == null || service == null || listener == null) throw new IllegalArgumentException("空间维护依赖不能为空");
        this.page = page; this.service = service; this.listener = listener;
        setContent(tabs()); startBuilding(); startRoom(); startBed(); loadReferences();
    }

    /** 上面的台账重新加载后可能没有选中行了，让表单回到新建模式。 */
    public void clearSelection() { startBuilding(); startRoom(); startBed(); }

    public void showBuilding(DormBuildingDto value) {
        if (value == null) { startBuilding(); return; }
        buildingId = value.getId(); buildingCode.setText(RealUi.input(value.getBuildingCode()));
        buildingName.setText(RealUi.input(value.getBuildingName())); buildingAddress.setText(RealUi.input(value.getAddress()));
        buildingGender.setSelectedItem(RealUi.option(value.getGenderPolicy())); buildingStatus.setSelectedItem(RealUi.option(value.getStatus()));
        buildingError.setText(" ");
        mode(buildingMode, buildingSubmit, "正在编辑楼栋：" + RealUi.text(value.getBuildingCode()) + " " + RealUi.text(value.getBuildingName()), "保存修改");
    }

    public void showRoom(DormRoomDto value) {
        if (value == null) { startRoom(); return; }
        roomId = value.getId(); roomBuildingSelection = value.getBuildingId(); select(roomBuilding, roomBuildingSelection, "当前楼栋");
        roomNo.setText(RealUi.input(value.getRoomNo())); roomFloor.setText(String.valueOf(value.getFloorNo()));
        roomCapacity.setText(String.valueOf(value.getCapacity())); roomType.setSelectedItem(RealUi.option(value.getRoomType()));
        roomStatus.setSelectedItem(RealUi.option(value.getStatus())); roomDescription.setText(RealUi.input(value.getDescription())); roomError.setText(" ");
        mode(roomMode, roomSubmit, "正在编辑房间：" + RealUi.text(value.getBuildingName()) + " " + RealUi.text(value.getRoomNo()), "保存修改");
    }

    public void showBed(DormBedDto value) {
        if (value == null) { startBed(); return; }
        bedId = value.getId(); bedRoomSelection = value.getRoomId(); select(bedRoom, bedRoomSelection, "当前房间");
        bedNo.setText(RealUi.input(value.getBedNo())); bedStatus.setSelectedItem(RealUi.option(value.getStatus())); bedError.setText(" ");
        mode(bedMode, bedSubmit, "正在编辑床位：" + RealUi.text(value.getRoomNo()) + " 房 " + RealUi.text(value.getBedNo()) + " 号床", "保存修改");
    }

    private static void mode(JLabel label, JButton submit, String text, String submitText) {
        label.setText(text);
        if (submit != null) submit.setText(submitText);
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
        JPanel actions = actions("清空，改为新建", "新建楼栋", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startBuilding(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveBuilding(); }
        }, buildingError);
        buildingSubmit = submitOf(actions);
        return tab(buildingMode, fields, actions);
    }

    private JPanel roomTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("所属楼栋", roomBuilding)); fields.add(UiFactory.labelledField("房间号", roomNo));
        fields.add(UiFactory.labelledField("楼层", roomFloor)); fields.add(UiFactory.labelledField("容量", roomCapacity));
        fields.add(UiFactory.labelledField("房间类型", roomType)); fields.add(UiFactory.labelledField("状态", roomStatus));
        fields.add(UiFactory.labelledField("说明", roomDescription));
        JPanel actions = actions("清空，改为新建", "新建房间", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startRoom(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveRoom(); }
        }, roomError);
        roomSubmit = submitOf(actions);
        return tab(roomMode, fields, actions);
    }

    private JPanel bedTab() {
        JPanel fields = new JPanel(new GridLayout(0, 2, 12, 8)); fields.setOpaque(false);
        fields.add(UiFactory.labelledField("所属房间", bedRoom)); fields.add(UiFactory.labelledField("床位号", bedNo));
        fields.add(UiFactory.labelledField("状态", bedStatus));
        JPanel actions = actions("清空，改为新建", "新建床位", new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { startBed(); }
        }, new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveBed(); }
        }, bedError);
        bedSubmit = submitOf(actions);
        return tab(bedMode, fields, actions);
    }

    private void loadReferences() {
        AsyncTask.run(new AsyncTask.Work<SpaceReferences>() {
            @Override public SpaceReferences run() throws Exception {
                return new SpaceReferences(allReferences(service::buildings), allReferences(service::rooms));
            }
        }, new AsyncTask.Callback<SpaceReferences>() {
            @Override public void onSuccess(SpaceReferences value) { fillReferences(value); }
            @Override public void onFailure(Throwable error) { roomError.setText(AsyncTask.message(error)); bedError.setText(AsyncTask.message(error)); }
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

    interface ReferenceLoader<T> { DormPage<T> load(DormPageQuery query) throws Exception; }
    static <T> List<T> allReferences(ReferenceLoader<T> loader) throws Exception {
        List<T> result = new java.util.ArrayList<>();
        for (int p = 1; ; p++) {
            DormPage<T> batch = loader.load(new DormPageQuery(p, 100, null, null, null, null));
            if (batch == null || batch.getItems().isEmpty()) return result;
            result.addAll(batch.getItems());
            if (result.size() >= batch.getTotal()) return result;
        }
    }

    private void startBuilding() { buildingId = 0L; buildingCode.setText(""); buildingName.setText(""); buildingAddress.setText(""); buildingGender.setSelectedItem(RealUi.option("MALE")); buildingStatus.setSelectedItem(RealUi.option("OPEN")); buildingError.setText(" "); mode(buildingMode, buildingSubmit, "新建楼栋：填好编码和名称后点「新建楼栋」，会立刻出现在上面的楼栋表里。", "新建楼栋"); }
    private void startRoom() { roomId = 0L; roomBuildingSelection = 0L; select(roomBuilding, 0L, ""); roomNo.setText(""); roomFloor.setText("1"); roomCapacity.setText("4"); roomType.setSelectedItem(RealUi.option("STANDARD")); roomStatus.setSelectedItem(RealUi.option("AVAILABLE")); roomDescription.setText(""); roomError.setText(" "); mode(roomMode, roomSubmit, "新建房间：先选所属楼栋，填房间号、楼层、容量后点「新建房间」。", "新建房间"); }
    private void startBed() { bedId = 0L; bedRoomSelection = 0L; select(bedRoom, 0L, ""); bedNo.setText(""); bedStatus.setSelectedItem(RealUi.option("AVAILABLE")); bedError.setText(" "); mode(bedMode, bedSubmit, "新建床位：先选所属房间，填床位号（如 1、2、3、4）后点「新建床位」。", "新建床位"); }

    private void saveBuilding() {
        try { final long id = buildingId; final DormBuildingWriteRequest request = new DormBuildingWriteRequest(id, RealUi.required(buildingCode.getText(), "楼栋编码"), RealUi.required(buildingName.getText(), "楼栋名称"), RealUi.optional(buildingAddress.getText()), RealUi.code(buildingGender.getSelectedItem()), RealUi.code(buildingStatus.getSelectedItem()));
            AsyncTask.run(new AsyncTask.Work<DormBuildingDto>() { @Override public DormBuildingDto run() throws Exception { return id == 0L ? service.createBuilding(request) : service.updateBuilding(request); } }, new AsyncTask.Callback<DormBuildingDto>() { @Override public void onSuccess(DormBuildingDto value) { page.showSuccess(id == 0L ? "楼栋已新建，楼栋表已定位到它。" : "楼栋修改已保存。"); showBuilding(value); listener.buildingSaved(value, id == 0L); loadReferences(); } @Override public void onFailure(Throwable cause) { buildingError.setText(AsyncTask.message(cause)); } });
        } catch (IllegalArgumentException ex) { buildingError.setText(ex.getMessage()); }
    }

    private void saveRoom() {
        try { final long id = roomId; final SpaceOption parent = selected(roomBuilding, "所属楼栋", roomError); if (parent == null) return; final DormRoomWriteRequest request = new DormRoomWriteRequest(id, parent.id, RealUi.required(roomNo.getText(), "房间号"), requiredInt(roomFloor.getText(), "楼层"), requiredPositiveInt(roomCapacity.getText(), "容量"), RealUi.code(roomType.getSelectedItem()), RealUi.code(roomStatus.getSelectedItem()), RealUi.optional(roomDescription.getText()));
            AsyncTask.run(new AsyncTask.Work<DormRoomDto>() { @Override public DormRoomDto run() throws Exception { return id == 0L ? service.createRoom(request) : service.updateRoom(request); } }, new AsyncTask.Callback<DormRoomDto>() { @Override public void onSuccess(DormRoomDto value) { page.showSuccess(id == 0L ? "房间已新建，房间表已定位到它。" : "房间修改已保存。"); showRoom(value); listener.roomSaved(value, id == 0L); loadReferences(); } @Override public void onFailure(Throwable cause) { roomError.setText(AsyncTask.message(cause)); } });
        } catch (IllegalArgumentException ex) { roomError.setText(ex.getMessage()); }
    }

    private void saveBed() {
        try { final long id = bedId; final SpaceOption parent = selected(bedRoom, "所属房间", bedError); if (parent == null) return; final String status = RealUi.code(bedStatus.getSelectedItem()); if (id == 0L && "OCCUPIED".equals(status)) throw new IllegalArgumentException("新增床位不能直接设为占用"); final DormBedWriteRequest request = new DormBedWriteRequest(id, parent.id, RealUi.required(bedNo.getText(), "床位号"), status);
            AsyncTask.run(new AsyncTask.Work<DormBedDto>() { @Override public DormBedDto run() throws Exception { return id == 0L ? service.createBed(request) : service.updateBed(request); } }, new AsyncTask.Callback<DormBedDto>() { @Override public void onSuccess(DormBedDto value) { page.showSuccess(id == 0L ? "床位已新建，床位表已定位到它。" : "床位修改已保存。"); showBed(value); listener.bedSaved(value, id == 0L); loadReferences(); } @Override public void onFailure(Throwable cause) { bedError.setText(AsyncTask.message(cause)); } });
        } catch (IllegalArgumentException ex) { bedError.setText(ex.getMessage()); }
    }

    private static SpaceOption selected(JComboBox<SpaceOption> box, String label, JLabel error) { SpaceOption value = (SpaceOption) box.getSelectedItem(); if (value == null || value.id <= 0L) { error.setText("请选择" + label); return null; } return value; }
    private static void select(JComboBox<SpaceOption> box, long id, String fallback) { if (id <= 0L) { if (box.getItemCount() > 0) box.setSelectedIndex(0); return; } for (int i = 0; i < box.getItemCount(); i++) if (((SpaceOption) box.getItemAt(i)).id == id) { box.setSelectedIndex(i); return; } box.addItem(new SpaceOption(id, fallback)); box.setSelectedIndex(box.getItemCount() - 1); }
    private static int requiredPositiveInt(String value, String label) { int n = requiredInt(value, label); if (n <= 0) throw new IllegalArgumentException(label + "必须大于 0"); return n; }
    private static int requiredInt(String value, String label) { try { int n = Integer.parseInt(RealUi.required(value, label)); if (n < 0) throw new IllegalArgumentException(label + "不能为负数"); return n; } catch (NumberFormatException ex) { throw new IllegalArgumentException(label + "必须是整数"); } }
    private static JPanel tab(JLabel mode, JPanel fields, JPanel actions) { JPanel stack = new JPanel(); stack.setOpaque(false); stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS)); mode.setAlignmentX(LEFT_ALIGNMENT); fields.setAlignmentX(LEFT_ALIGNMENT); actions.setAlignmentX(LEFT_ALIGNMENT); stack.add(mode); stack.add(Box.createVerticalStrut(10)); stack.add(fields); stack.add(Box.createVerticalStrut(14)); stack.add(actions); JPanel content = new JPanel(new BorderLayout()); content.setOpaque(false); content.setBorder(javax.swing.BorderFactory.createEmptyBorder(14, 2, 4, 2)); content.add(stack, BorderLayout.NORTH); return content; }
    private JPanel actions(String fresh, String save, java.awt.event.ActionListener newAction, java.awt.event.ActionListener saveAction, JLabel error) { JPanel actions = UiFactory.horizontal(8); JButton clear = new SecondaryButton(fresh); clear.addActionListener(newAction); JButton submit = new PrimaryButton(save); submit.addActionListener(saveAction); actions.add(clear); actions.add(submit); actions.add(error); return actions; }
    /** {@link #actions} 里第二个组件就是主按钮；拿出来是为了按模式改它的文字。 */
    private static JButton submitOf(JPanel actions) { return (JButton) actions.getComponent(1); }
    private static JComboBox<RealUi.CodeOption> choices(String... codes) { JComboBox<RealUi.CodeOption> box = new JComboBox<RealUi.CodeOption>(RealUi.options(codes)); box.setFont(DesignTokens.regular(13)); return box; }
    private static JTextField field() { return UiFactory.textField(10); }

    private static final class SpaceOption { private final long id; private final String label; SpaceOption(long id, String label) { this.id = id; this.label = label; } static SpaceOption empty(String label) { return new SpaceOption(0L, label); } @Override public String toString() { return label; } }
    private static final class SpaceReferences { private final List<DormBuildingDto> buildings; private final List<DormRoomDto> rooms; SpaceReferences(List<DormBuildingDto> b, List<DormRoomDto> r) { buildings = b; rooms = r; } }
}
