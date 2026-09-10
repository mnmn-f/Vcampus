package edu.seu.vcampus.server.ai.tool;

import org.junit.Test;

import java.util.Arrays;
import org.threeten.bp.LocalDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 分页业务 DTO 的嵌套内容应能直接在对话中阅读。 */
public final class ToolResultFormatterTest {
    @Test public void formatsNestedItemsWithFriendlyLabels() {
        String text = new ToolResultFormatter().format(new Page(
                Arrays.asList(new Course("CS101", "程序设计")), 1));
        assertTrue(text.contains("课程编号：CS101"));
        assertTrue(text.contains("课程：程序设计"));
        assertFalse(text.contains("Course"));
    }

    @Test public void formatsStudyRoomTimesWithoutReflectingChronology() {
        String text = new ToolResultFormatter().format(new Reservation(2L, 1L,
                "图书馆 研习室A", LocalDateTime.of(2026, 11, 11, 9, 0),
                LocalDateTime.of(2026, 11, 11, 12, 0), "RESERVED"));
        assertTrue(text.contains("开始时间：2026-11-11 09:00"));
        assertTrue(text.contains("结束时间：2026-11-11 12:00"));
        assertTrue(text.contains("自习室：图书馆 研习室A"));
        assertFalse(text.contains("chronology"));
        assertFalse(text.contains("availableChronologies"));
        assertFalse(text.contains("- "));
    }

    @Test public void fieldQuestionReturnsOnlyRequestedBookFieldAndIdentity() {
        String text = new ToolResultFormatter().format(new Book("软件工程实践导论",
                "张老师", "东大出版社", "TP311"), "这本书的作者是谁");
        assertTrue(text.contains("标题：软件工程实践导论"));
        assertTrue(text.contains("作者：张老师"));
        assertFalse(text.contains("出版社"));
        assertFalse(text.contains("ISBN"));
    }

    @Test public void completeInformationQuestionKeepsAllBookFields() {
        String text = new ToolResultFormatter().format(new Book("软件工程实践导论",
                "张老师", "东大出版社", "TP311"), "返回这本书的完整信息");
        assertTrue(text.contains("作者：张老师"));
        assertTrue(text.contains("出版社：东大出版社"));
        assertTrue(text.contains("ISBN：TP311"));
    }

    public static final class Page {
        private final Iterable<Course> items; private final int total;
        Page(Iterable<Course> items, int total) { this.items = items; this.total = total; }
        public Iterable<Course> getItems() { return items; }
        public int getTotal() { return total; }
    }

    public static final class Course {
        private final String courseCode; private final String courseName;
        Course(String courseCode, String courseName) {
            this.courseCode = courseCode; this.courseName = courseName;
        }
        public String getCourseCode() { return courseCode; }
        public String getCourseName() { return courseName; }
    }

    public static final class Reservation {
        private final long id; private final long roomId; private final String roomName;
        private final LocalDateTime startAt; private final LocalDateTime endAt;
        private final String status;
        Reservation(long id, long roomId, String roomName, LocalDateTime startAt,
                LocalDateTime endAt, String status) {
            this.id = id; this.roomId = roomId; this.roomName = roomName;
            this.startAt = startAt; this.endAt = endAt; this.status = status;
        }
        public long getId() { return id; }
        public long getRoomId() { return roomId; }
        public String getRoomName() { return roomName; }
        public LocalDateTime getStartAt() { return startAt; }
        public LocalDateTime getEndAt() { return endAt; }
        public String getStatus() { return status; }
    }

    public static final class Book {
        private final String title; private final String author;
        private final String publisher; private final String isbn;
        Book(String title, String author, String publisher, String isbn) {
            this.title = title; this.author = author; this.publisher = publisher; this.isbn = isbn;
        }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getPublisher() { return publisher; }
        public String getIsbn() { return isbn; }
    }
}
