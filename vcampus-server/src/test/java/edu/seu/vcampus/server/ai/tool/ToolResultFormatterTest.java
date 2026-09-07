package edu.seu.vcampus.server.ai.tool;

import org.junit.Test;

import java.util.Arrays;

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
}
