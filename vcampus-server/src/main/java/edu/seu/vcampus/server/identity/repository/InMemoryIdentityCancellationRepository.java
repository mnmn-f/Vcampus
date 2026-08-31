package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 账号注销申请内存仓储；通过聚合状态快照模拟事务回滚。 */
final class InMemoryIdentityCancellationRepository implements IdentityCancellationRepository {
    private final InMemoryIdentityState state;

    InMemoryIdentityCancellationRepository(InMemoryIdentityState state) {
        this.state = state;
    }

    @Override public AccountCancellationPage searchAccountCancellations(Connection c,
                                                                          AccountCancellationQuery query,
                                                                          Long userId) {
        AccountCancellationQuery q = query == null ? new AccountCancellationQuery() : query;
        synchronized (state) {
            List<AccountCancellationDto> found = new ArrayList<AccountCancellationDto>();
            for (AccountCancellationRecord record : state.cancellationRequests.values()) {
                if (userId != null && record.getUserId() != userId.longValue()) continue;
                if (q.getStatus() != null && !q.getStatus().equalsIgnoreCase(record.getStatus())) continue;
                found.add(record.toDto());
            }
            Collections.sort(found, new Comparator<AccountCancellationDto>() {
                @Override public int compare(AccountCancellationDto a, AccountCancellationDto b) {
                    return Long.compare(b.getId(), a.getId());
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new AccountCancellationPage(new ArrayList<AccountCancellationDto>(
                    found.subList(from, to)), q.getPage(), q.getPageSize(), found.size());
        }
    }

    @Override public AccountCancellationRecord findAccountCancellation(Connection c,
                                                                                    long requestId,
                                                                                    boolean forUpdate) {
        synchronized (state) {
            return state.cancellationRequests.get(Long.valueOf(requestId));
        }
    }

    @Override public AccountCancellationRecord findPendingAccountCancellation(Connection c,
                                                                                         long userId,
                                                                                         boolean forUpdate) {
        synchronized (state) {
            for (AccountCancellationRecord record : state.cancellationRequests.values()) {
                if (record.getUserId() == userId && "PENDING".equals(record.getStatus())) {
                    return record;
                }
            }
            return null;
        }
    }

    @Override public long insertAccountCancellation(Connection c, long userId, String reason) {
        synchronized (state) {
            if (findPending(userId) != null) {
                throw new IdentityRepositoryException(ResultCodes.CONFLICT, "已有待处理注销申请");
            }
            IdentityUserRecord user = state.users.get(Long.valueOf(userId));
            if (user == null) throw new IdentityRepositoryException(ResultCodes.NOT_FOUND, "用户不存在");
            long id = state.cancellationSequence++;
            LocalDateTime now = LocalDateTime.now();
            state.cancellationRequests.put(Long.valueOf(id), new AccountCancellationRecord(id, userId,
                    user.getAccount(), user.getDisplayName(), reason, "PENDING", null, null, null,
                    now, now));
            return id;
        }
    }

    @Override public boolean withdrawAccountCancellation(Connection c, long requestId, long userId) {
        synchronized (state) {
            AccountCancellationRecord old = state.cancellationRequests.get(Long.valueOf(requestId));
            if (old == null || old.getUserId() != userId || !"PENDING".equals(old.getStatus())) {
                return false;
            }
            replace(old, "CANCELLED", null, null, null);
            return true;
        }
    }

    @Override public boolean reviewAccountCancellation(Connection c, long requestId, String status,
                                                       long reviewerId, String remark) {
        synchronized (state) {
            AccountCancellationRecord old = state.cancellationRequests.get(Long.valueOf(requestId));
            if (old == null || !"PENDING".equals(old.getStatus())) return false;
            replace(old, status, Long.valueOf(reviewerId), LocalDateTime.now(), remark);
            return true;
        }
    }

    private AccountCancellationRecord findPending(long userId) {
        for (AccountCancellationRecord record : state.cancellationRequests.values()) {
            if (record.getUserId() == userId && "PENDING".equals(record.getStatus())) return record;
        }
        return null;
    }

    private void replace(AccountCancellationRecord old, String status, Long reviewer,
                         LocalDateTime reviewedAt, String remark) {
        state.cancellationRequests.put(Long.valueOf(old.getId()), new AccountCancellationRecord(
                old.getId(), old.getUserId(), old.getAccount(), old.getDisplayName(), old.getReason(),
                status, reviewer, reviewedAt, remark, old.getCreatedAt(), LocalDateTime.now()));
    }
}
