package edu.seu.vcampus.server.store.service;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.Locale;

/** 面向用户的商店订单编号：VC-年月日-当日四位流水号。 */
final class StoreOrderNumber {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private StoreOrderNumber() { }

    static String create(LocalDate date, int sequence) {
        if (date == null) throw new IllegalArgumentException("订单日期不能为空");
        if (sequence <= 0 || sequence > 9999) {
            throw new IllegalArgumentException("订单流水号必须在1到9999之间");
        }
        return "VC-" + DATE.format(date) + "-"
                + String.format(Locale.ROOT, "%04d", Integer.valueOf(sequence));
    }
}
