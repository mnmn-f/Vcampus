package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;

import java.util.List;
import java.util.Locale;

/** 内存测试仓储共享分页和文本规范化逻辑。 */
final class InMemoryLibrarySupport {
    private InMemoryLibrarySupport() {
    }

    static <T> PageResult<T> page(List<T> all, int page, int size) {
        int from = Math.min(all.size(), (page - 1) * size);
        int to = Math.min(all.size(), from + size);
        return new PageResult<T>(all.subList(from, to), page, size, all.size());
    }

    static String lower(String value) {
        return value == null || value.trim().isEmpty() ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }

    static boolean same(String filter, String value) {
        return filter == null || filter.trim().isEmpty()
                || filter.trim().equalsIgnoreCase(value);
    }

    static String safe(String value) { return value == null ? "" : value; }
}
