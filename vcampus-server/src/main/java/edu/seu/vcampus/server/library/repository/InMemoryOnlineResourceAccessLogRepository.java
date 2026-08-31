package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 无磁盘访问日志仓储，供服务单元测试和内存集成测试使用。 */
public final class InMemoryOnlineResourceAccessLogRepository
        implements OnlineResourceAccessLogRepository {
    private final List<OnlineResourceAccessLogDto> logs =
            new ArrayList<OnlineResourceAccessLogDto>();
    private long nextId = 1L;

    @Override
    public synchronized void append(Connection connection, OnlineResourceAccessLogDto log) {
        if (log == null) throw new IllegalArgumentException("log is required");
        logs.add(new OnlineResourceAccessLogDto(nextId++, log.getResourceId(), log.getUserId(),
                log.getResourceTitle(), log.getAccount(), log.getDisplayName(),
                log.getAccessedAt() == null ? LocalDateTime.now() : log.getAccessedAt()));
    }

    @Override
    public synchronized OnlineResourceAccessLogPage search(Connection connection,
                                                            OnlineResourceAccessLogQuery query) {
        OnlineResourceAccessLogQuery q = query == null ? new OnlineResourceAccessLogQuery() : query;
        List<OnlineResourceAccessLogDto> found = new ArrayList<OnlineResourceAccessLogDto>();
        for (OnlineResourceAccessLogDto log : logs) if (matches(log, q)) found.add(log);
        Collections.sort(found, new Comparator<OnlineResourceAccessLogDto>() {
            @Override public int compare(OnlineResourceAccessLogDto left,
                                          OnlineResourceAccessLogDto right) {
                int time = right.getAccessedAt().compareTo(left.getAccessedAt());
                return time == 0 ? Long.compare(right.getId(), left.getId()) : time;
            }
        });
        return page(found, q);
    }

    public synchronized int size() { return logs.size(); }

    private static boolean matches(OnlineResourceAccessLogDto log,
                                   OnlineResourceAccessLogQuery q) {
        if (q.getResourceId() != null && q.getResourceId().longValue() != log.getResourceId()) return false;
        if (q.getUserId() != null && q.getUserId().longValue() != log.getUserId()) return false;
        if (q.getFrom() != null && log.getAccessedAt().isBefore(q.getFrom())) return false;
        return q.getTo() == null || !log.getAccessedAt().isAfter(q.getTo());
    }

    private static OnlineResourceAccessLogPage page(List<OnlineResourceAccessLogDto> rows,
                                                     OnlineResourceAccessLogQuery q) {
        int from = Math.min(rows.size(), q.getOffset());
        int to = Math.min(rows.size(), from + q.getPageSize());
        return new OnlineResourceAccessLogPage(rows.subList(from, to), q.getPage(),
                q.getPageSize(), rows.size());
    }
}
