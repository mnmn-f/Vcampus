package edu.seu.vcampus.common.dto.identity;

import java.util.List;

/** 用户分页结果。 */
public final class UserPage extends IdentityPage<ProfileDto> {
    private static final long serialVersionUID = 1L;

    public UserPage(List<ProfileDto> items, int page, int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public UserPage(int page, int pageSize, long total, List<ProfileDto> items) {
        this(items, page, pageSize, total);
    }

    public List<ProfileDto> getUsers() { return getItems(); }
}
