package edu.seu.vcampus.server.library.service;

import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogDto;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogPage;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessLogQuery;
import edu.seu.vcampus.common.dto.library.OnlineResourceAccessRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.library.repository.InMemoryOnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.library.repository.OnlineResourceAccessLogRepository;
import edu.seu.vcampus.server.library.repository.OnlineResourceRepository;
import edu.seu.vcampus.server.security.SessionContext;

import org.threeten.bp.LocalDateTime;

/** 线上资源检索和图书管理员启停维护服务。 */
public final class OnlineResourceService extends LibraryServiceSupport {
    private final OnlineResourceRepository resources;
    private final OnlineResourceAccessLogRepository accessLogs;

    public OnlineResourceService(OnlineResourceRepository resources,
                                 TransactionManager transactions) {
        this(resources, new InMemoryOnlineResourceAccessLogRepository(), transactions);
    }

    public OnlineResourceService(OnlineResourceRepository resources,
                                 OnlineResourceAccessLogRepository accessLogs,
                                 TransactionManager transactions) {
        super(transactions);
        if (resources == null) throw new IllegalArgumentException("resources is required");
        if (accessLogs == null) throw new IllegalArgumentException("accessLogs is required");
        this.resources = resources; this.accessLogs = accessLogs;
    }

    public PageResult<OnlineResourceView> search(final SessionContext session,
                                                 final OnlineResourceSearchRequest request) {
        require(session, Permission.LIBRARY_READ);
        final OnlineResourceSearchRequest original = request == null
                ? new OnlineResourceSearchRequest() : request;
        page(original.getPage(), original.getPageSize());
        final OnlineResourceSearchRequest query = session.allows(Permission.LIBRARY_MANAGE)
                ? original : new OnlineResourceSearchRequest(original.getKeyword(),
                original.getResourceType(), "ACTIVE", original.getPage(), original.getPageSize());
        return execute(new Work<PageResult<OnlineResourceView>>() { public PageResult<OnlineResourceView> run(java.sql.Connection c) throws Exception { return resources.search(c, query); } });
    }

    public OnlineResourceView save(final SessionContext session,
                                   final OnlineResourceUpsertRequest request) {
        require(session, Permission.LIBRARY_MANAGE);
        validate(request);
        return execute(new Work<OnlineResourceView>() {
            @Override public OnlineResourceView run(java.sql.Connection c) throws Exception {
                if (request.getId() > 0 && resources.findById(c, request.getId()) == null) throw new LibraryServiceException(ResultCodes.NOT_FOUND, "线上资源不存在");
                return resources.save(c, request, session.getUserId());
            }
        });
    }

    public OnlineResourceView access(final SessionContext session,
                                     final OnlineResourceAccessRequest request) {
        require(session, Permission.LIBRARY_READ);
        if (request == null) throw invalid("线上资源访问请求不能为空");
        id(request.getResourceId(), "资源编号不正确");
        return execute(new Work<OnlineResourceView>() {
            @Override public OnlineResourceView run(java.sql.Connection c) throws Exception {
                OnlineResourceView found = resources.findById(c, request.getResourceId());
                if (found == null) throw new LibraryServiceException(ResultCodes.NOT_FOUND, "线上资源不存在");
                OnlineResourceView resource = found;
                if (!session.allows(Permission.LIBRARY_MANAGE) && !"ACTIVE".equalsIgnoreCase(resource.getStatus())) throw new LibraryServiceException(ResultCodes.FORBIDDEN, "线上资源暂未启用");
                accessLogs.append(c, new OnlineResourceAccessLogDto(0L, resource.getId(), session.getUserId(), resource.getTitle(), session.getAccount(), session.getDisplayName(), LocalDateTime.now()));
                return resource;
            }
        });
    }

    public OnlineResourceAccessLogPage searchAccessLogs(final SessionContext session,
                                                        final OnlineResourceAccessLogQuery request) {
        require(session, Permission.LIBRARY_MANAGE);
        final OnlineResourceAccessLogQuery query = request == null
                ? new OnlineResourceAccessLogQuery() : request;
        page(query.getPage(), query.getPageSize());
        return execute(new Work<OnlineResourceAccessLogPage>() { public OnlineResourceAccessLogPage run(java.sql.Connection c) throws Exception { return accessLogs.search(c, query); } });
    }

    private static void validate(OnlineResourceUpsertRequest r) {
        if (r == null) throw invalid("线上资源请求不能为空");
        text(r.getTitle(), "资源名称"); text(r.getResourceType(), "资源类型");
        text(r.getUrl(), "资源地址");
        if (!"ACTIVE".equals(r.getStatus()) && !"INACTIVE".equals(r.getStatus())) {
            throw invalid("资源状态不正确");
        }
    }

    private static LibraryServiceException invalid(String message) {
        return new LibraryServiceException(ResultCodes.INVALID_INPUT, message);
    }
}
