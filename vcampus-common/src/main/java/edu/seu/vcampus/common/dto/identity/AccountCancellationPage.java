package edu.seu.vcampus.common.dto.identity;

import java.util.List;

/** 账号注销申请分页结果。 */
public final class AccountCancellationPage extends IdentityPage<AccountCancellationDto> {
    private static final long serialVersionUID = 1L;

    public AccountCancellationPage(List<AccountCancellationDto> items, int page,
                                   int pageSize, long total) {
        super(items, page, pageSize, total);
    }

    public AccountCancellationPage(int page, int pageSize, long total,
                                   List<AccountCancellationDto> items) {
        this(items, page, pageSize, total);
    }

    public List<AccountCancellationDto> getRequests() { return getItems(); }
}
