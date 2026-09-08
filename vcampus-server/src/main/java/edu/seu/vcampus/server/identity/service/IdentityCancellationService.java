package edu.seu.vcampus.server.identity.service;
import edu.seu.vcampus.common.dto.identity.AccountCancellationDto;
import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;
import edu.seu.vcampus.common.dto.identity.AccountCancellationRequest;
import edu.seu.vcampus.common.dto.identity.AccountCancellationReviewRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.AccountCancellationRecord;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/** 账号注销申请用例；批准仅在事务提交后清理运行时会话。 */
final class IdentityCancellationService {
    private static final Set<String> STATUSES = new HashSet<String>(Arrays.asList(
            "PENDING", "APPROVED", "REJECTED", "CANCELLED"));
    private final IdentityRecordRepository repository;
    private final IdentityTransactionRunner transactions;
    private final SessionManager sessionManager;
    IdentityCancellationService(IdentityRecordRepository repository,
                                 IdentityTransactionRunner transactions,
                                 SessionManager sessionManager) {
        this.repository = repository;
        this.transactions = transactions;
        this.sessionManager = sessionManager;
    }
    AccountCancellationDto submit(final SessionContext session,
                                  final AccountCancellationRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.PROFILE_UPDATE);
        final String reason = reason(request);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountCancellationDto>() {
                    @Override public AccountCancellationDto execute(Connection c)
                            throws IdentityServiceException {
                        IdentityUserRecord user = user(c, session.getUserId(), true);
                        active(user);
                        if (repository.findPendingAccountCancellation(c, user.getUserId(), true) != null) {
                            throw conflict("已有待处理注销申请");
                        }
                        long id = repository.insertAccountCancellation(c, user.getUserId(), reason);
                        IdentityServiceSupport.audit(repository, c, session,
                                "ACCOUNT_CANCELLATION_SUBMIT", "ACCOUNT_CANCELLATION",
                                Long.valueOf(id), null);
                        return request(c, id, false).toDto();
                    }
                });
    }
    AccountCancellationPage own(final SessionContext session, AccountCancellationQuery query)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.PROFILE_READ);
        final AccountCancellationQuery safe = validateQuery(query);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountCancellationPage>() {
                    @Override public AccountCancellationPage execute(Connection c) {
                        return repository.searchAccountCancellations(c, safe,
                                Long.valueOf(session.getUserId()));
                    }
                });
    }
    AccountCancellationDto withdraw(final SessionContext session, final long requestId)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.PROFILE_UPDATE);
        IdentityServiceSupport.positive(requestId, "申请编号");
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountCancellationDto>() {
                    @Override public AccountCancellationDto execute(Connection c)
                            throws IdentityServiceException {
                        user(c, session.getUserId(), true);
                        AccountCancellationRecord old = request(c, requestId, true);
                        if (old.getUserId() != session.getUserId()) {
                            throw IdentityServiceSupport.error(ResultCodes.FORBIDDEN, "只能撤回本人的申请");
                        }
                        pending(old);
                        if (!repository.withdrawAccountCancellation(c, requestId, session.getUserId())) {
                            throw conflict("申请状态已变化");
                        }
                        IdentityServiceSupport.audit(repository, c, session,
                                "ACCOUNT_CANCELLATION_WITHDRAW", "ACCOUNT_CANCELLATION",
                                Long.valueOf(requestId), null);
                        return request(c, requestId, false).toDto();
                    }
                });
    }
    AccountCancellationPage search(SessionContext session, AccountCancellationQuery query)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        final AccountCancellationQuery safe = validateQuery(query);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountCancellationPage>() {
                    @Override public AccountCancellationPage execute(Connection c) {
                        return repository.searchAccountCancellations(c, safe, null);
                    }
                });
    }

    AccountCancellationDto approve(SessionContext session, AccountCancellationReviewRequest request)
            throws IdentityServiceException {
        return review(session, request, true);
    }

    AccountCancellationDto reject(SessionContext session, AccountCancellationReviewRequest request)
            throws IdentityServiceException {
        return review(session, request, false);
    }

    private AccountCancellationDto review(final SessionContext session,
                                          final AccountCancellationReviewRequest review,
                                          final boolean approve) throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.USER_MANAGE);
        if (review == null) throw invalid("审核请求不能为空");
        IdentityServiceSupport.positive(review.getRequestId(), "申请编号");
        final String remark = IdentityServiceSupport.text(review.getRemark(), "审核备注");
        IdentityServiceSupport.length(remark, 500, "审核备注");
        final String status = approve ? "APPROVED" : "REJECTED";
        AccountCancellationDto result = IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountCancellationDto>() {
                    @Override public AccountCancellationDto execute(Connection c)
                            throws IdentityServiceException {
                        AccountCancellationRecord probe = request(c, review.getRequestId(), false);
                        IdentityUserRecord target = user(c, probe.getUserId(), true);
                        AccountCancellationRecord old = request(c, review.getRequestId(), true);
                        pending(old);
                        if (approve) active(target);
                        if (!repository.reviewAccountCancellation(c, review.getRequestId(), status,
                                session.getUserId(), remark)) {
                            throw conflict("申请状态已变化");
                        }
                        if (approve) {
                            if (!repository.updateStatus(c, target.getUserId(), "DISABLED")) {
                                throw conflict("用户状态未更新");
                            }
                            repository.revokePersistedUserSessions(c, target.getUserId());
                        }
                        IdentityServiceSupport.audit(repository, c, session,
                                "ACCOUNT_CANCELLATION_" + status, "ACCOUNT_CANCELLATION",
                                Long.valueOf(review.getRequestId()), null);
                        return request(c, review.getRequestId(), false).toDto();
                    }
                });
        if (approve) sessionManager.invalidateUserSessions(result.getUserId());
        return result;
    }

    private AccountCancellationRecord request(Connection c, long id, boolean lock)
            throws IdentityServiceException {
        AccountCancellationRecord found = repository.findAccountCancellation(c, id, lock);
        if (found == null) throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "注销申请不存在");
        return found;
    }

    private IdentityUserRecord user(Connection c, long id, boolean lock)
            throws IdentityServiceException {
        IdentityUserRecord found = repository.findById(c, id, lock);
        if (found == null) throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "用户不存在");
        return found;
    }

    private static void active(IdentityUserRecord user) throws IdentityServiceException {
        if (!"ACTIVE".equals(user.getStatus())) {
            throw IdentityServiceSupport.error(ResultCodes.ACCOUNT_DISABLED, "账号当前不可办理注销申请");
        }
    }

    private static void pending(AccountCancellationRecord request) throws IdentityServiceException {
        if (!"PENDING".equals(request.getStatus())) throw conflict("申请已处理，不能重复操作");
    }

    private static String reason(AccountCancellationRequest request) throws IdentityServiceException {
        if (request == null) throw invalid("注销申请不能为空");
        String value = IdentityServiceSupport.text(request.getReason(), "申请原因");
        IdentityServiceSupport.length(value, 500, "申请原因");
        return value;
    }

    private static AccountCancellationQuery validateQuery(AccountCancellationQuery query)
            throws IdentityServiceException {
        AccountCancellationQuery safe = query == null ? new AccountCancellationQuery() : query;
        if (safe.getStatus() != null && !STATUSES.contains(safe.getStatus().toUpperCase())) {
            throw invalid("申请状态不正确");
        }
        return safe;
    }

    private static IdentityServiceException invalid(String message) {
        return IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, message);
    }

    private static IdentityServiceException conflict(String message) {
        return IdentityServiceSupport.error(ResultCodes.CONFLICT, message);
    }
}
