package edu.seu.vcampus.common.dto.identity;

import java.util.List;

/** 会话分页结果。 */
public final class SessionPage extends IdentityPage<SessionDto> {
    private static final long serialVersionUID = 1L;

    public SessionPage(List<SessionDto> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public SessionPage(int page, int pageSize, long total, List<SessionDto> items) {
        this(items, page, pageSize, total);
    }

    public List<SessionDto> getSessions() { return getItems(); }
}
