package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.BorrowRecordView;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import java.util.ArrayList;
import java.util.List;

/** 图书馆界面预览所用的本地样例数据；不会访问数据库或网络。 */
final class DemoLibraryData {
    private DemoLibraryData() { }

    static List<BookDetail> books() {
        LocalDateTime now = LocalDateTime.now();
        List<BookDetail> values = new ArrayList<BookDetail>();
        values.add(new BookDetail(101L, "9787302519620", "Java 核心技术", "Cay S. Horstmann",
                "机械工业出版社", "计算机", 8, 5, "李文正图书馆三楼 A-16",
                "Java 程序设计与工程实践参考书。", "ON_SHELF", now.minusMonths(6), now));
        values.add(new BookDetail(102L, "9787111544937", "计算机网络", "谢希仁",
                "电子工业出版社", "计算机", 10, 3, "李文正图书馆三楼 B-08",
                "计算机网络基础与体系结构。", "ON_SHELF", now.minusMonths(5), now));
        values.add(new BookDetail(103L, "9787040406641", "高等数学", "同济大学数学系",
                "高等教育出版社", "数学", 12, 7, "李文正图书馆二楼 C-12",
                "工科数学基础课程参考教材。", "ON_SHELF", now.minusMonths(4), now));
        values.add(new BookDetail(104L, "9787020002207", "围城", "钱钟书",
                "人民文学出版社", "文学", 6, 2, "李文正图书馆四楼 D-05",
                "中国现代文学经典作品。", "ON_SHELF", now.minusMonths(3), now));
        return values;
    }

    static List<BorrowRecordView> borrowings() {
        LocalDateTime now = LocalDateTime.now();
        List<BorrowRecordView> values = new ArrayList<BorrowRecordView>();
        values.add(new BorrowRecordView(201L, 102L, "计算机网络", 1L, "演示学生",
                now.minusDays(7), now.plusDays(23), null, "BORROWED", 0, "演示借阅记录"));
        values.add(new BorrowRecordView(202L, 104L, "围城", 1L, "演示学生",
                now.minusDays(45), now.minusDays(15), now.minusDays(18), "RETURNED", 0, null));
        return values;
    }

    static List<StudyRoomView> rooms() {
        List<StudyRoomView> values = new ArrayList<StudyRoomView>();
        values.add(room(301L, "李文正图书馆一楼", "A101", 24, "安静学习区"));
        values.add(room(302L, "李文正图书馆二楼", "B203", 12, "小组讨论室"));
        values.add(room(303L, "李文正图书馆三楼", "C305", 36, "综合自习室"));
        return values;
    }

    static List<StudyRoomReservationView> reservations() {
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        List<StudyRoomReservationView> values = new ArrayList<StudyRoomReservationView>();
        values.add(new StudyRoomReservationView(401L, 302L, "李文正图书馆二楼 B203",
                1L, start, start.plusHours(2), "RESERVED", null));
        return values;
    }

    static List<OnlineResourceView> resources() {
        LocalDateTime now = LocalDateTime.now();
        List<OnlineResourceView> values = new ArrayList<OnlineResourceView>();
        values.add(new OnlineResourceView(501L, "中国知网", "学术数据库",
                "https://www.cnki.net", "论文、期刊与学位文献检索。", 5L, "ACTIVE", now.minusMonths(3)));
        values.add(new OnlineResourceView(502L, "万方数据", "学术数据库",
                "https://www.wanfangdata.com.cn", "中文学术资源与科技报告。", 5L, "ACTIVE", now.minusMonths(2)));
        return values;
    }

    private static StudyRoomView room(long id, String building, String number,
                                      int capacity, String description) {
        return new StudyRoomView(id, building, number, capacity, LocalTime.of(8, 0),
                LocalTime.of(22, 0), "OPEN", description);
    }
}
