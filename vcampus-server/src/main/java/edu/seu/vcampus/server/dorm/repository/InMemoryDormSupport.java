package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 内存仓储的分页、文本和状态小工具。 */
final class InMemoryDormSupport {
    private InMemoryDormSupport() { }

    static boolean matches(String keyword, String... values) {
        if (keyword == null || keyword.trim().isEmpty()) return true;
        String needle = keyword.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    static boolean status(String expected, String actual) {
        return expected == null || expected.trim().isEmpty() || expected.equals(actual);
    }

    static <T> DormPage<T> page(List<T> source, DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        return page(source, q.getPage(), q.getPageSize());
    }

    static <T> DormPage<T> page(List<T> source, int page, int size) {
        int from = Math.min(source.size(), Math.max(0, (page - 1) * size));
        int to = Math.min(source.size(), from + size);
        List<T> rows = from >= to ? Collections.<T>emptyList()
                : new ArrayList<T>(source.subList(from, to));
        return new DormPage<T>(page, size, source.size(), rows);
    }

    static String[] location(Map<Long, String> rooms, long roomId) {
        String value = rooms.get(Long.valueOf(roomId));
        return value == null ? new String[] { "?", "?" } : value.split("/", 2);
    }

    static String periodKey(long roomId, org.threeten.bp.LocalDate start,
                            org.threeten.bp.LocalDate end) {
        return roomId + "|" + start + "|" + end;
    }
}
