package edu.seu.vcampus.server.db;

import org.junit.Test;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import java.sql.Time;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** JDBC/ThreeTen 时间转换必须接受整分时间并保留 Timestamp 纳秒。 */
public final class JdbcTemporalTest {
    @Test
    public void timestampUsesJdbcFormatAndRoundTripsNanoseconds() {
        LocalDateTime value = LocalDateTime.of(2026, 9, 2, 10, 30, 15, 123456789);

        Timestamp timestamp = JdbcTemporal.timestamp(value);

        assertEquals("2026-09-02 10:30:15.123456789", timestamp.toString());
        assertEquals(value, JdbcTemporal.localDateTime(timestamp));
    }

    @Test
    public void timestampAcceptsWholeMinute() {
        LocalDateTime value = LocalDateTime.of(2026, 9, 2, 10, 30);

        assertEquals(value, JdbcTemporal.localDateTime(JdbcTemporal.timestamp(value)));
    }

    @Test
    public void timeAlwaysIncludesSeconds() {
        LocalTime value = LocalTime.of(8, 0);

        Time time = JdbcTemporal.time(value);

        assertEquals("08:00:00", time.toString());
        assertEquals(value, JdbcTemporal.localTime(time));
    }

    @Test
    public void nullValuesRemainNull() {
        assertNull(JdbcTemporal.timestamp(null));
        assertNull(JdbcTemporal.localDateTime(null));
        assertNull(JdbcTemporal.time(null));
        assertNull(JdbcTemporal.localTime(null));
    }
}
