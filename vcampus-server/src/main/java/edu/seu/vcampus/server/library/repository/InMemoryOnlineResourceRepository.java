package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.OnlineResourceSearchRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceUpsertRequest;
import edu.seu.vcampus.common.dto.library.OnlineResourceView;
import edu.seu.vcampus.common.dto.library.PageResult;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 无磁盘线上资源仓储。 */
public final class InMemoryOnlineResourceRepository implements OnlineResourceRepository {
    private final Map<Long, OnlineResourceView> resources = new LinkedHashMap<Long, OnlineResourceView>();
    private long nextId = 1L;

    @Override
    public synchronized OnlineResourceView findById(Connection c, long resourceId) {
        return resources.get(resourceId);
    }

    @Override
    public synchronized PageResult<OnlineResourceView> search(Connection c,
                                                               OnlineResourceSearchRequest r) {
        List<OnlineResourceView> found = new ArrayList<OnlineResourceView>();
        String key = InMemoryLibrarySupport.lower(r.getKeyword());
        for (OnlineResourceView row : resources.values()) {
            String text = InMemoryLibrarySupport.lower(row.getTitle() + " "
                    + InMemoryLibrarySupport.safe(row.getResourceType()) + " "
                    + InMemoryLibrarySupport.safe(row.getDescription()));
            if (key != null && !text.contains(key)) continue;
            if (!InMemoryLibrarySupport.same(r.getResourceType(), row.getResourceType())) continue;
            if (!InMemoryLibrarySupport.same(r.getStatus(), row.getStatus())) continue;
            found.add(row);
        }
        return InMemoryLibrarySupport.page(found, r.getPage(), r.getPageSize());
    }

    @Override
    public synchronized OnlineResourceView save(Connection c, OnlineResourceUpsertRequest r,
                                                 long publisherId) {
        long id = r.getId() <= 0 ? nextId++ : r.getId();
        OnlineResourceView old = resources.get(id);
        OnlineResourceView value = new OnlineResourceView(id, r.getTitle(), r.getResourceType(),
                r.getUrl(), r.getDescription(), old == null ? publisherId : old.getPublisherId(), r.getStatus(),
                "ACTIVE".equals(r.getStatus())
                        ? (old == null || old.getPublishedAt() == null
                        ? LocalDateTime.now() : old.getPublishedAt())
                        : old == null ? null : old.getPublishedAt());
        resources.put(id, value);
        return value;
    }

    public synchronized void seed(OnlineResourceView resource) {
        resources.put(resource.getId(), resource);
        nextId = Math.max(nextId, resource.getId() + 1L);
    }

}
