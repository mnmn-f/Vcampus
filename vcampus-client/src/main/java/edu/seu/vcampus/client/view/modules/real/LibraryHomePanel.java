package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.client.service.library.LibraryClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.client.ui.DesignTokens;
import edu.seu.vcampus.common.dto.campus.*;
import edu.seu.vcampus.common.dto.library.*;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** 学生首页：用真实馆藏、借阅、预约和公告数据组成概览。 */
final class LibraryHomePanel extends JPanel {
    private final LibraryClientService library; private final CampusClientService campus;
    private final Consumer<String> searchBooks; private final Runnable openBorrowings, openRooms;
    private final JPanel recommended = LibraryUi.stack(12), notices = LibraryUi.stack(10), appointment = LibraryUi.stack(8);
    private final JLabel current = metric("—"), due = metric("—"), today = metric("—");
    private long refreshSerial;

    LibraryHomePanel(ClientSession session, LibraryClientService library, CampusClientService campus,
                     Consumer<String> searchBooks, Runnable openBorrowings, Runnable openRooms) {
        super(new LibraryUi.StackLayout(18)); setOpaque(false); this.library = library; this.campus = campus;
        this.searchBooks = searchBooks; this.openBorrowings = openBorrowings; this.openRooms = openRooms;
        LibraryUi.Surface welcome = new LibraryUi.Surface(DesignTokens.PRIMARY_LIGHT, 24);
        welcome.add(LibraryUi.label(greeting() + "，" + session.getDisplayName(), 25, true));
        welcome.add(LibraryUi.muted("今天，也读一点喜欢的书。"));
        JTextField search = LibraryUi.search("搜索书名、作者或 ISBN");
        JButton submit = LibraryUi.button("搜索图书", true, () -> this.searchBooks.accept(search.getText().trim()));
        search.addActionListener(e -> submit.doClick()); welcome.add(LibraryUi.between(search, submit)); add(welcome);

        JPanel metrics = new JPanel(new edu.seu.vcampus.client.ui.ResponsiveGridLayout(200, 3, 14)); metrics.setOpaque(false);
        metrics.add(metricCard("当前借阅", current, openBorrowings)); metrics.add(metricCard("即将到期", due, openBorrowings)); metrics.add(metricCard("今日预约", today, openRooms)); add(metrics);
        JPanel booksCard = LibraryUi.card(); booksCard.add(LibraryUi.between(LibraryUi.label("馆藏推荐", 18, true), LibraryUi.link("查看全部 →", () -> this.searchBooks.accept("")))); booksCard.add(recommended);
        JPanel right = LibraryUi.stack(14); JPanel announcementCard = LibraryUi.card(); announcementCard.add(LibraryUi.label("图书馆公告", 18, true)); announcementCard.add(notices); right.add(announcementCard);
        JPanel appointmentCard = LibraryUi.card(); appointmentCard.add(LibraryUi.between(LibraryUi.label("我的预约", 18, true), LibraryUi.link("查看详情 →", openRooms))); appointmentCard.add(appointment); right.add(appointmentCard);
        add(LibraryUi.columns(booksCard, right)); refresh();
    }
    void refresh() {
        final long request = ++refreshSerial; LibraryUi.replace(recommended, LibraryUi.state("正在加载馆藏…", null));
        AsyncTask.run(() -> load(), new AsyncTask.Callback<HomeData>() {
            public void onSuccess(HomeData data) { if (request != refreshSerial) return; render(data); }
            public void onFailure(Throwable error) { if (request == refreshSerial) { current.setText("—"); due.setText("—"); today.setText("—"); LibraryUi.replace(recommended, LibraryUi.state("首页内容暂未加载", LibraryHomePanel.this::refresh)); } }
        });
    }
    private HomeData load() throws Exception {
        HomeData data = new HomeData(); data.books = library.searchBooks(new BookSearchRequest(null, null, "ON_SHELF", 1, 4)).getItems();
        List<BorrowRecordView> all = LibraryCatalogPanel.allBorrowings(library); data.borrowed = new ArrayList<>();
        org.threeten.bp.LocalDate todayDate = org.threeten.bp.LocalDate.now();
        for (BorrowRecordView row : all) if (LibraryCatalogPanel.outstanding(row)) { data.borrowed.add(row); if (row.getDueAt() != null && !row.getDueAt().toLocalDate().isAfter(todayDate.plusDays(7))) data.due++; }
        data.reservations = library.reservations(new StudyRoomReservationSearchRequest("RESERVED", 1, 20)).getItems();
        for (StudyRoomReservationView row : data.reservations) if (row.getStartAt() != null && row.getStartAt().toLocalDate().equals(todayDate)) data.today++;
        data.announcements = campus.announcements(new CampusAnnouncementQuery(new CampusPageQuery(1, 3, null, "PUBLISHED"), "LIBRARY")).getItems(); return data;
    }
    private void render(HomeData data) {
        current.setText(String.valueOf(data.borrowed.size())); due.setText(String.valueOf(data.due)); today.setText(String.valueOf(data.today));
        recommended.removeAll(); JPanel grid = new JPanel(new edu.seu.vcampus.client.ui.ResponsiveGridLayout(120, 4, 14)); grid.setOpaque(false);
        for (BookDetail book : data.books) {
            JPanel item = LibraryUi.stack(6); item.add(new LibraryUi.Cover(book, 105, 145)); item.add(LibraryUi.label(book.getTitle(), 13, true));
            item.add(LibraryUi.muted(LibraryUi.plain(book.getAuthor()))); item.add(LibraryUi.badge("可借阅", true));
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); item.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { searchBooks.accept(book.getTitle()); } }); grid.add(item);
        }
        recommended.add(data.books.isEmpty() ? LibraryUi.state("暂无推荐图书", null) : grid); recommended.revalidate(); recommended.repaint();
        notices.removeAll(); for (CampusAnnouncementDto value : data.announcements) notices.add(notice(value));
        if (data.announcements.isEmpty()) notices.add(LibraryUi.state("暂无公告", null)); notices.revalidate(); notices.repaint();
        appointment.removeAll(); StudyRoomReservationView next = null; org.threeten.bp.LocalDateTime now = org.threeten.bp.LocalDateTime.now();
        for (StudyRoomReservationView value : data.reservations) if (value.getEndAt() != null && value.getEndAt().isAfter(now) && (next == null || value.getStartAt().isBefore(next.getStartAt()))) next = value;
        if (next == null) appointment.add(LibraryUi.state("暂无近期预约", null)); else {
            appointment.add(LibraryUi.label(next.getRoomName(), 14, true)); appointment.add(LibraryUi.muted(next.getStartAt().toLocalDate() + "  " + RealUi.time(next.getStartAt().toLocalTime()) + "–" + RealUi.time(next.getEndAt().toLocalTime())));
        }
        appointment.revalidate(); appointment.repaint();
    }
    private JPanel notice(CampusAnnouncementDto value) {
        JPanel row = LibraryUi.stack(5); row.add(LibraryUi.label(value.getTitle(), 13, true));
        String date = value.getPublishAt() == null ? "" : value.getPublishAt().toLocalDate().toString(); row.add(LibraryUi.muted(date));
        row.setToolTipText(value.getContent()); row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DesignTokens.BORDER_LIGHT), BorderFactory.createEmptyBorder(0, 0, 9, 0))); return row;
    }
    private static JLabel metric(String text) { JLabel l = LibraryUi.label(text, 26, true); l.setForeground(LibraryUi.GREEN); return l; }
    private JPanel metricCard(String title, JLabel value, Runnable action) {
        JPanel card = LibraryUi.card(); card.add(LibraryUi.label(title, 13, false)); card.add(value); card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() { public void mouseClicked(java.awt.event.MouseEvent e) { action.run(); } }); return card;
    }
    private static String greeting() { int h = java.time.LocalTime.now().getHour(); return h < 11 ? "早上好" : h < 14 ? "中午好" : h < 18 ? "下午好" : "晚上好"; }
    private static final class HomeData { List<BookDetail> books; List<BorrowRecordView> borrowed; List<StudyRoomReservationView> reservations; List<CampusAnnouncementDto> announcements; int due, today; }
}
