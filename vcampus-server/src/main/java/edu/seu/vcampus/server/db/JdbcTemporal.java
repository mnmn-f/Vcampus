package edu.seu.vcampus.server.db;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/** JDBC 与 ThreeTen 时间类型之间的 Java 7 兼容转换。 */
public final class JdbcTemporal {
    private static final DateTimeFormatter JDBC_TIMESTAMP =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter JDBC_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private JdbcTemporal() {
    }

    public static Timestamp timestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value.format(JDBC_TIMESTAMP));
    }

    public static LocalDateTime localDateTime(Timestamp value) {
        return value == null ? null
                : LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }

    public static Date date(LocalDate value) {
        return value == null ? null : Date.valueOf(value.toString());
    }

    public static LocalDate localDate(Date value) {
        return value == null ? null : LocalDate.parse(value.toString());
    }

    public static Time time(LocalTime value) {
        return value == null ? null : Time.valueOf(value.format(JDBC_TIME));
    }

    public static LocalTime localTime(Time value) {
        return value == null ? null : LocalTime.parse(value.toString());
    }
}
