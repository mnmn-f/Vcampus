package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.library.*;
import javax.swing.*;
import java.awt.*;

/** 房间式自习室预约界面，按日期和时段选择房间并确认。 */
final class LibraryRoomBookingPanel extends JPanel {
    private final BasePage host; private final LibraryClientService service;
    private final CalendarDateField date = new CalendarDateField();
    private final JComboBox<String> start = LibraryUi.combo(hours()), end = LibraryUi.combo(hours()), floor = LibraryUi.combo(new String[]{"全部楼层", "一层", "二层", "三层", "四层"});
    private final JPanel rooms = LibraryUi.stack(8), summary = LibraryUi.stack(10), reservations = LibraryUi.stack(8);
    private StudyRoomView selected; private int pageNumber = 1; private long roomSerial, recordSerial;

    LibraryRoomBookingPanel(BasePage host, LibraryClientService service) {
        super(new LibraryUi.StackLayout(18)); setOpaque(false); this.host = host; this.service = service;
        add(LibraryUi.heading("自习室预约")); add(LibraryUi.label("预约自习室     我的预约记录", 14, false));
        start.setSelectedItem("14:00"); end.setSelectedItem("16:00");
        JPanel filters = new JPanel(new edu.seu.vcampus.client.ui.ResponsiveGridLayout(175, 5, 12)); filters.setOpaque(false);
        filters.add(LibraryUi.field("预约日期", date)); filters.add(LibraryUi.field("开始时间", start)); filters.add(LibraryUi.field("结束时间", end)); filters.add(LibraryUi.field("楼层", floor));
        filters.add(LibraryUi.field(" ", LibraryUi.button("查询", true, () -> { pageNumber = 1; loadRooms(); }))); LibraryUi.Surface filterCard = LibraryUi.card(); filterCard.add(filters); add(filterCard);
        JPanel roomCard = LibraryUi.card(); roomCard.add(LibraryUi.between(LibraryUi.label("可预约自习室", 18, true), LibraryUi.muted("李文正图书馆"))); roomCard.add(rooms);
        JPanel confirm = LibraryUi.card(); confirm.add(LibraryUi.label("确认预约", 18, true)); confirm.add(summary);
        add(LibraryUi.columns(roomCard, confirm));
        JPanel recordCard = LibraryUi.card(); recordCard.add(LibraryUi.between(LibraryUi.label("我的预约记录", 18, true), LibraryUi.link("刷新", this::loadReservations))); recordCard.add(reservations); add(recordCard);
        loadRooms(); loadReservations(); showSummary();
        start.addActionListener(e -> loadRooms()); end.addActionListener(e -> loadRooms());
        floor.addActionListener(e -> { pageNumber = 1; loadRooms(); });
        date.addPropertyChangeListener("date", e -> loadRooms());
        edu.seu.vcampus.client.ui.VisibleRefresh.attach(this, () -> selected == null,
                () -> { loadRooms(); loadReservations(); });
    }
    private static String[] hours() { String[] values = new String[29]; for (int i = 0; i < values.length; i++) { int minutes = 8 * 60 + i * 30; values[i] = String.format("%02d:%02d", minutes / 60, minutes % 60); } return values; }
    private org.threeten.bp.LocalDateTime time(JComboBox<String> value) { return date.getDate().atTime(org.threeten.bp.LocalTime.parse((String) value.getSelectedItem())); }
    private void loadRooms() {
        if (!time(end).isAfter(time(start))) {
            ++roomSerial; selected = null; showSummary();
            LibraryUi.replace(rooms, LibraryUi.state("结束时间须晚于开始时间", null)); return;
        }
        final long request = ++roomSerial; final int p = pageNumber; selected = null; showSummary(); LibraryUi.replace(rooms, LibraryUi.state("正在查询自习室…", null));
        String floorKeyword = floor.getSelectedIndex() == 0 ? null
                : ((String) floor.getSelectedItem()).replace("层", "楼");
        StudyRoomSearchRequest query = new StudyRoomSearchRequest(floorKeyword, null, null, time(start), time(end), p, 8);
        AsyncTask.run(() -> service.searchStudyRooms(query), new AsyncTask.Callback<PageResult<StudyRoomView>>() {
            public void onSuccess(PageResult<StudyRoomView> data) {
                if (request != roomSerial) return; rooms.removeAll();
                JPanel head = tableRow("自习室", "楼层", "状态", "操作", true); rooms.add(head);
                for (StudyRoomView room : data.getItems()) rooms.add(roomRow(room)); if (data.getItems().isEmpty()) rooms.add(LibraryUi.state("该条件下暂无自习室", null));
                rooms.add(LibraryUi.pager(data.getPage(), data.getPageSize(), data.getTotal(), n -> { pageNumber = n; loadRooms(); })); rooms.revalidate(); rooms.repaint();
            }
            public void onFailure(Throwable error) { if (request == roomSerial) LibraryUi.replace(rooms, LibraryUi.state("自习室暂未加载", LibraryRoomBookingPanel.this::loadRooms)); }
        });
    }
    private JPanel roomRow(StudyRoomView room) {
        boolean open = "OPEN".equals(room.getStatus()); JButton choose = LibraryUi.button(open ? "选择" : "不可预约", false, () -> { selected = room; showSummary(); loadRoomsSelection(); }); choose.setEnabled(open);
        return tableRow(LibraryUi.plain(room.getRoomNo()) + " 自习室", room.getBuildingName(), open ? "空闲" : "占用", choose, false, open);
    }
    private void loadRoomsSelection() { for (Component component : rooms.getComponents()) if (component instanceof JPanel) component.repaint(); }
    private JPanel tableRow(String first, String second, String third, String fourth, boolean header) { return tableRow(first, second, third, LibraryUi.label(fourth, 12, header), header, true); }
    private JPanel tableRow(String first, String second, String third, Component action, boolean header, boolean green) {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0)); row.setOpaque(true); row.setBackground(header ? DesignTokens.PRIMARY_LIGHT : Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        row.add(LibraryUi.label(first, header ? 12 : 13, header)); row.add(LibraryUi.label(second, 12, header)); row.add(header ? LibraryUi.label(third, 12, true) : LibraryUi.badge(third, green)); row.add(action); return row;
    }
    private void showSummary() {
        summary.removeAll(); if (selected == null) summary.add(LibraryUi.state("请从左侧选择自习室后提交预约", null)); else {
            LibraryUi.Surface room = new LibraryUi.Surface(DesignTokens.PRIMARY_LIGHT, 15); room.add(LibraryUi.label(selected.getRoomNo() + " 自习室", 19, true)); room.add(LibraryUi.muted(selected.getBuildingName())); summary.add(room);
            summary.add(pair("日期", date.getDate().toString())); summary.add(pair("时段", start.getSelectedItem() + "–" + end.getSelectedItem()));
            JButton confirm = LibraryUi.button("提交预约", true, () -> { }); clearActions(confirm); confirm.addActionListener(e -> reserve(confirm)); summary.add(confirm);
        }
        summary.revalidate(); summary.repaint();
    }
    private static void clearActions(JButton button) { for (java.awt.event.ActionListener listener : button.getActionListeners()) button.removeActionListener(listener); }
    private JPanel pair(String key, String value) { return LibraryUi.between(LibraryUi.muted(key), LibraryUi.label(value, 13, false)); }
    private void reserve(JButton button) {
        if (selected == null) return; org.threeten.bp.LocalDateTime from = time(start), to = time(end);
        if (!to.isAfter(from) || from.isBefore(org.threeten.bp.LocalDateTime.now())) { host.showWarning("请选择正确的预约日期和时段"); return; }
        button.setEnabled(false); StudyRoomReservationRequest request = new StudyRoomReservationRequest(selected.getId(), from, to);
        AsyncTask.run(() -> service.reserveStudyRoom(request), new AsyncTask.Callback<StudyRoomReservationView>() {
            public void onSuccess(StudyRoomReservationView value) { host.showSuccess("自习室预约成功"); selected = null; showSummary(); loadReservations(); loadRooms(); }
            public void onFailure(Throwable error) { button.setEnabled(true); host.showError(AsyncTask.message(error)); }
        });
    }
    private void loadReservations() {
        final long request = ++recordSerial; LibraryUi.replace(reservations, LibraryUi.state("正在加载预约记录…", null));
        AsyncTask.run(() -> service.reservations(new StudyRoomReservationSearchRequest(null, 1, 20)), new AsyncTask.Callback<PageResult<StudyRoomReservationView>>() {
            public void onSuccess(PageResult<StudyRoomReservationView> data) {
                if (request != recordSerial) return; reservations.removeAll(); reservations.add(reservationHeader());
                int shown = 0; for (StudyRoomReservationView row : data.getItems()) { reservations.add(reservationRow(row)); if (++shown == 6) break; }
                if (shown == 0) reservations.add(LibraryUi.state("暂无预约记录", null)); reservations.revalidate(); reservations.repaint();
            }
            public void onFailure(Throwable error) { if (request == recordSerial) LibraryUi.replace(reservations, LibraryUi.state("预约记录暂未加载", LibraryRoomBookingPanel.this::loadReservations)); }
        });
    }
    private JPanel reservationHeader() { return reservationCells("自习室", "日期", "时段", "状态", LibraryUi.label("操作", 12, true), true); }
    private JPanel reservationRow(StudyRoomReservationView row) {
        Component action = new JLabel(); if ("RESERVED".equals(row.getStatus())) { JButton cancel = LibraryUi.button("取消预约", false, () -> { }); clearActions(cancel); cancel.addActionListener(e -> cancel(row, cancel)); action = cancel; }
        return reservationCells(row.getRoomName(), row.getStartAt().toLocalDate().toString(), RealUi.time(row.getStartAt().toLocalTime()) + "–" + RealUi.time(row.getEndAt().toLocalTime()), RealUi.status(row.getStatus()), action, false);
    }
    private JPanel reservationCells(String a, String b, String c, String d, Component action, boolean header) {
        JPanel row = new JPanel(new GridLayout(1, 5, 8, 0)); row.setOpaque(true); row.setBackground(header ? DesignTokens.PRIMARY_LIGHT : Color.WHITE); row.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        row.add(LibraryUi.label(a, 12, header)); row.add(LibraryUi.label(b, 12, header)); row.add(LibraryUi.label(c, 12, header)); row.add(header ? LibraryUi.label(d, 12, true) : LibraryUi.badge(d, "RESERVED".equals(d) || "已预约".equals(d))); row.add(action); return row;
    }
    private void cancel(StudyRoomReservationView row, JButton button) {
        if (!RealUi.confirm(this, "确认取消该自习室预约？")) return; button.setEnabled(false);
        AsyncTask.run(() -> service.cancelReservation(row.getId()), new AsyncTask.Callback<StudyRoomReservationView>() {
            public void onSuccess(StudyRoomReservationView value) { host.showSuccess("预约已取消"); loadReservations(); loadRooms(); }
            public void onFailure(Throwable error) { button.setEnabled(true); host.showError(AsyncTask.message(error)); }
        });
    }
}
