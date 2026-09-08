package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

final class DemoOnlineResourceService {
    private final List<OnlineResourceView> resources = DemoLibraryData.resources();
    private final List<OnlineResourceAccessLogDto> accessLogs =
            new ArrayList<OnlineResourceAccessLogDto>();
    private long nextAccessLogId = 1L;

    PageResult<OnlineResourceView> search(OnlineResourceSearchRequest request) {
        OnlineResourceSearchRequest query = request == null
                ? new OnlineResourceSearchRequest() : request;
        List<OnlineResourceView> result = new ArrayList<OnlineResourceView>();
        for (OnlineResourceView value : resources) {
            if (DemoLibrarySupport.matches(value.getTitle() + " " + value.getResourceType()
                    + " " + value.getDescription(), query.getKeyword())
                    && DemoLibrarySupport.same(query.getResourceType(), value.getResourceType())
                    && DemoLibrarySupport.same(query.getStatus(), value.getStatus())) {
                result.add(value);
            }
        }
        return DemoLibrarySupport.page(result, query.getPage(), query.getPageSize());
    }

    OnlineResourceView save(OnlineResourceUpsertRequest request)
            throws NetworkClientException {
        if (request == null) throw DemoLibrarySupport.error("资源信息不能为空");
        DemoLibrarySupport.required(request.getTitle(), "资源名称不能为空");
        DemoLibrarySupport.required(request.getResourceType(), "资源类型不能为空");
        DemoLibrarySupport.required(request.getUrl(), "资源地址不能为空");
        long id = request.getId() > 0L ? request.getId() : nextResourceId();
        OnlineResourceView old = findResource(id);
        LocalDateTime published = "ACTIVE".equals(request.getStatus())
                ? old == null || old.getPublishedAt() == null
                ? LocalDateTime.now() : old.getPublishedAt()
                : old == null ? null : old.getPublishedAt();
        OnlineResourceView value = new OnlineResourceView(id, request.getTitle(),
                request.getResourceType(), request.getUrl(), request.getDescription(), 5L,
                DemoLibrarySupport.text(request.getStatus(), "ACTIVE"), published);
        replaceResource(value);
        return value;
    }

    OnlineResourceView access(long resourceId) throws NetworkClientException {
        OnlineResourceView value = findResource(resourceId);
        if (value == null) throw DemoLibrarySupport.error("未找到线上资源");
        if (!"ACTIVE".equals(value.getStatus())) {
            throw DemoLibrarySupport.error("该线上资源当前已停用");
        }
        accessLogs.add(0, new OnlineResourceAccessLogDto(nextAccessLogId++, value.getId(),
                1L, value.getTitle(), "demo_student", "演示学生", LocalDateTime.now()));
        return value;
    }

    OnlineResourceAccessLogPage accessLogs(OnlineResourceAccessLogQuery request) {
        OnlineResourceAccessLogQuery query = request == null
                ? new OnlineResourceAccessLogQuery() : request;
        List<OnlineResourceAccessLogDto> result = new ArrayList<OnlineResourceAccessLogDto>();
        for (OnlineResourceAccessLogDto value : accessLogs) {
            if (query.getResourceId() != null
                    && query.getResourceId().longValue() != value.getResourceId()) continue;
            if (query.getUserId() != null
                    && query.getUserId().longValue() != value.getUserId()) continue;
            if (query.getFrom() != null && value.getAccessedAt().isBefore(query.getFrom())) continue;
            if (query.getTo() != null && value.getAccessedAt().isAfter(query.getTo())) continue;
            result.add(value);
        }
        int from = Math.min((query.getPage() - 1) * query.getPageSize(), result.size());
        int to = Math.min(from + query.getPageSize(), result.size());
        return new OnlineResourceAccessLogPage(
                new ArrayList<OnlineResourceAccessLogDto>(result.subList(from, to)),
                query.getPage(), query.getPageSize(), result.size());
    }

    private OnlineResourceView findResource(long id) {
        for (OnlineResourceView value : resources) if (value.getId() == id) return value;
        return null;
    }

    private void replaceResource(OnlineResourceView value) {
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(i).getId() == value.getId()) {
                resources.set(i, value);
                return;
            }
        }
        resources.add(value);
    }

    private long nextResourceId() {
        long id = 1L;
        for (OnlineResourceView value : resources) id = Math.max(id, value.getId() + 1L);
        return id;
    }
}
