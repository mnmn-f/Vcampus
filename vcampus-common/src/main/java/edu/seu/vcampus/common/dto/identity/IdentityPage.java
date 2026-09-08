package edu.seu.vcampus.common.dto.identity;

import java.util.List;
import edu.seu.vcampus.common.dto.store.StorePage;

/** 身份与系统列表统一分页结果。 */
public class IdentityPage<T> extends StorePage<T> {
    private static final long serialVersionUID = 1L;

    public IdentityPage(List<T> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }
}
