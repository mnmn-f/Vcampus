package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.DangerButton;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.common.security.Role;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** 自习室查询、学生预约取消和管理员开放状态维护。 */
public final class LibraryRoomsPanel extends JPanel {
    private final BasePage page; private final LibraryClientService service; private final Role role;
    private final AsyncPagedTable<StudyRoomView> rooms; private final javax.swing.JTextArea detail = UiFactory.textArea(2, 20);
    private final LibraryRoomEditorPanel editor; private final ReservationFormPanel reservation; private final AsyncPagedTable<StudyRoomReservationView> reservations;

    public LibraryRoomsPanel(BasePage page, LibraryClientService service, Role role) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)); this.page = page; this.service = service; this.role = role; detail.setEditable(false); detail.setText("选择自习室查看详情。"); rooms = roomTable(); add(rooms);
        JPanel info = new JPanel(new BorderLayout()); info.setOpaque(false); info.add(detail, BorderLayout.CENTER); add(info);
        if (role == Role.STUDENT) { reservation = new ReservationFormPanel(new ReservationFormPanel.Listener() {
            @Override public void onReserve(StudyRoomReservationRequest request) { reserve(request); }
        }); reservations = reservationTable(); add(reservation); add(reservations); editor = null; }
        else if (role == Role.LIBRARIAN) { editor = new LibraryRoomEditorPanel(new LibraryRoomEditorPanel.Listener() {
            @Override public void onSave(StudyRoomUpsertRequest request) { LibraryRoomsPanel.this.saveRoom(request); }
        }); reservation = null; reservations = reservationTable(); add(editor); add(reservations); }
        else { editor = null; reservation = null; reservations = null; }
    }

    private AsyncPagedTable<StudyRoomView> roomTable() {
        AsyncPagedTable<StudyRoomView> table = new AsyncPagedTable<StudyRoomView>(role == Role.LIBRARIAN ? "自习室管理" : "自习室信息", "开放状态、容量和开放时段。", "搜索楼栋或房间号", new String[]{"全部状态", "开放", "维护中", "关闭"}, new String[]{"编号", "楼栋", "房间号", "容量", "开放时段", "状态"},
                new AsyncPagedTable.Loader<StudyRoomView>() {
                    @Override public PageSlice<StudyRoomView> load(int p, String keyword, String filter) throws Exception {
                        PageResult<StudyRoomView> result = service.searchStudyRooms(new StudyRoomSearchRequest(keyword, roomStatus(filter), null, null, null, p, 20)); return new PageSlice<StudyRoomView>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize());
                    }
                }, new AsyncPagedTable.RowMapper<StudyRoomView>() {
                    @Override public Object[] values(StudyRoomView row) { return new Object[]{row.getId(), row.getBuildingName(), row.getRoomNo(), row.getCapacity(), RealUi.time(row.getOpenTime()) + "-" + RealUi.time(row.getCloseTime()), RealUi.status(row.getStatus())}; }
                }, new AsyncPagedTable.SelectionListener<StudyRoomView>() {
                    @Override public void onSelected(StudyRoomView row) { selectRoom(row); }
                });
        if (role == Role.STUDENT) { JButton select = new PrimaryButton("预约自习室"); select.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { chooseRoom(); }
        }); table.addAction(select); }
        if (role == Role.LIBRARIAN) {
            JButton create = new PrimaryButton("新建自习室"); create.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { editor.startNew(); }
            }); table.addAction(create);
            JButton close = new DangerButton("删除（关闭）"); close.addActionListener(new java.awt.event.ActionListener() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { closeRoom(); }
            }); table.addAction(close);
        }
        return table;
    }

    private void selectRoom(StudyRoomView value) { if (value == null) { detail.setText("选择自习室查看详情。"); return; } detail.setText("自习室详情：" + value.getBuildingName() + " " + value.getRoomNo() + "，容量 " + value.getCapacity() + "，说明：" + RealUi.text(value.getDescription())); if (editor != null) editor.showRoom(value); if (reservation != null) reservation.selectRoom(value.getId()); }
    private void chooseRoom() { StudyRoomView value = rooms.selectedItem(); if (value == null) page.showWarning("请先选择自习室。"); else reservation.selectRoom(value.getId()); }
    private void reserve(final StudyRoomReservationRequest request) { AsyncTask.run(new AsyncTask.Work<StudyRoomReservationView>() { @Override public StudyRoomReservationView run() throws Exception { return service.reserveStudyRoom(request); } }, new AsyncTask.Callback<StudyRoomReservationView>() { @Override public void onSuccess(StudyRoomReservationView value) { page.showSuccess("自习室预约成功。"); reservations.reload(); } @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); } }); }
    private AsyncPagedTable<StudyRoomReservationView> reservationTable() { AsyncPagedTable<StudyRoomReservationView> table = new AsyncPagedTable<StudyRoomReservationView>(role == Role.LIBRARIAN ? "全部预约记录" : "我的预约记录", "", "按状态筛选", new String[]{"全部状态", "已预约", "已取消"}, new String[]{"编号", "房间", "开始时间", "结束时间", "状态"}, new AsyncPagedTable.Loader<StudyRoomReservationView>() { @Override public PageSlice<StudyRoomReservationView> load(int p, String keyword, String filter) throws Exception { PageResult<StudyRoomReservationView> result = service.reservations(new StudyRoomReservationSearchRequest(reservationStatus(filter), p, 20)); return new PageSlice<StudyRoomReservationView>(result.getItems(), result.getTotal(), result.getPage(), result.getPageSize()); } }, new AsyncPagedTable.RowMapper<StudyRoomReservationView>() { @Override public Object[] values(StudyRoomReservationView row) { return new Object[]{row.getId(), row.getRoomName(), RealUi.dateTime(row.getStartAt()), RealUi.dateTime(row.getEndAt()), RealUi.status(row.getStatus())}; } }, null); JButton cancel = new DangerButton("取消预约"); cancel.addActionListener(new java.awt.event.ActionListener() { @Override public void actionPerformed(java.awt.event.ActionEvent e) { cancel(); } }); table.addAction(cancel); return table; }
    private void cancel() { final StudyRoomReservationView value = reservations.selectedItem(); if (value == null) { page.showWarning("请先选择预约记录。"); return; } if (!RealUi.confirm(this, "确认取消该自习室预约？")) return; AsyncTask.run(new AsyncTask.Work<StudyRoomReservationView>() { @Override public StudyRoomReservationView run() throws Exception { return service.cancelReservation(value.getId()); } }, new AsyncTask.Callback<StudyRoomReservationView>() { @Override public void onSuccess(StudyRoomReservationView result) { page.showSuccess("预约已取消。"); reservations.reload(); } @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); } }); }
    private void saveRoom(final StudyRoomUpsertRequest request) { AsyncTask.run(new AsyncTask.Work<StudyRoomView>() { @Override public StudyRoomView run() throws Exception { return service.saveStudyRoom(request); } }, new AsyncTask.Callback<StudyRoomView>() { @Override public void onSuccess(StudyRoomView value) { page.showSuccess("自习室已保存。"); rooms.reload(); } @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); } }); }
    private void closeRoom() { StudyRoomView value = rooms.selectedItem(); if (value == null) { page.showWarning("请先选择要关闭的自习室。"); return; } if (!RealUi.confirm(this, "确认关闭“" + value.getBuildingName() + " " + value.getRoomNo() + "”？关闭后不能继续预约。")) return; saveRoom(new StudyRoomUpsertRequest(value.getId(), value.getBuildingName(), value.getRoomNo(), value.getCapacity(), value.getOpenTime(), value.getCloseTime(), "CLOSED", value.getDescription())); }
    private static String roomStatus(String filter) { return "开放".equals(filter) ? "OPEN" : "维护中".equals(filter) ? "MAINTENANCE" : "关闭".equals(filter) ? "CLOSED" : null; }
    private static String reservationStatus(String filter) { return "已预约".equals(filter) ? "RESERVED" : "已取消".equals(filter) ? "CANCELLED" : null; }
}
