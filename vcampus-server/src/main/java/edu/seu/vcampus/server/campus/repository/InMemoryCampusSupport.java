package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;

import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 内存分页、关键字和时间判断工具。 */
final class InMemoryCampusSupport {
    private InMemoryCampusSupport() { }

    static boolean matches(CampusPageQuery q, String status, String... values) {
        if (q != null && q.getStatus() != null && !q.getStatus().equals(status)) return false;
        if (q == null || q.getKeyword() == null) return true;
        String key = q.getKeyword().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(key)) return true;
        }
        return false;
    }

    static <T> CampusPage<T> page(List<T> source, CampusPageQuery query) {
        CampusPageQuery q = query == null ? CampusPageQuery.all() : query;
        int from = Math.min((q.getPage() - 1) * q.getPageSize(), source.size());
        int to = Math.min(from + q.getPageSize(), source.size());
        return new CampusPage<T>(q.getPage(), q.getPageSize(), source.size(),
                new ArrayList<T>(source.subList(from, to)));
    }

    static boolean effective(LocalDateTime publish, LocalDateTime expire, LocalDateTime now) {
        return (publish == null || !publish.isAfter(now))
                && (expire == null || expire.isAfter(now));
    }

    static boolean overlaps(LocalDateTime start, LocalDateTime end,
                            LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && end.isAfter(otherStart);
    }
}
