package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DemoLibrarySupport {
    private DemoLibrarySupport() { }

    static int number(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    static String required(String value, String message) throws NetworkClientException {
        if (value == null || value.trim().length() == 0) throw error(message);
        return value.trim();
    }

    static String text(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }

    static boolean same(String expected, String actual) {
        return expected == null || expected.trim().length() == 0
                || expected.equalsIgnoreCase(actual);
    }

    static boolean matches(String source, String keyword) {
        return keyword == null || keyword.trim().length() == 0 || source != null
                && source.toLowerCase(Locale.ROOT).contains(
                keyword.trim().toLowerCase(Locale.ROOT));
    }

    static NetworkClientException error(String message) {
        return new NetworkClientException("DEMO.LIBRARY", message);
    }

    static <T> PageResult<T> page(List<T> values, int page, int pageSize) {
        int current = page < 1 ? 1 : page;
        int size = pageSize < 1 ? 20 : pageSize;
        int from = Math.min((current - 1) * size, values.size());
        int to = Math.min(from + size, values.size());
        return new PageResult<T>(new ArrayList<T>(values.subList(from, to)), current, size,
                values.size());
    }
}
