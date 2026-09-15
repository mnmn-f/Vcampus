package edu.seu.vcampus.client.view.modules.real;

import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** 图书管理员的公告时刻和自习室时段均通过下拉组件读写。 */
public final class LibrarianTimeDropdownTest {
    @Test public void timeDropdownUsesDefaultsAndPreservesExistingMinute() {
        TimeDropdown value = new TimeDropdown();
        value.setTime(LocalTime.of(8, 0));
        assertEquals(LocalTime.of(8, 0), value.getTime());
        value.setTime(LocalTime.of(21, 45));
        assertEquals(LocalTime.of(21, 45), value.getTime());
    }

    @Test public void dateTimeDropdownSupportsOptionalAndExistingValues() {
        DateTimeDropdown value = new DateTimeDropdown();
        assertNull(value.getDateTime());
        LocalDateTime selected = LocalDateTime.of(2028, 2, 29, 18, 15);
        value.setDateTime(selected);
        assertEquals(selected, value.getDateTime());
        value.setDateTime(null);
        assertNull(value.getDateTime());
    }
}
