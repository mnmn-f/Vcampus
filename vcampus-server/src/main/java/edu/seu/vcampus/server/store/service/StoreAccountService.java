package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountRechargeRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.LedgerRecord;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.math.BigDecimal;
import java.sql.Connection;

/** 学生校园账户余额、充值与本人流水。 */
final class StoreAccountService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreAccountService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    AccountDto get(final SessionContext session) throws StoreServiceException {
        requirePurchase(session);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountDto>() {
                    @Override public AccountDto execute(Connection c) throws StoreServiceException {
                        return account(c, session.getUserId(), false);
                    }
                });
    }

    AccountLedgerPage ledger(final SessionContext session, AccountLedgerQuery query)
            throws StoreServiceException {
        requirePurchase(session);
        final AccountLedgerQuery safe = query == null ? new AccountLedgerQuery() : query;
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountLedgerPage>() {
                    @Override public AccountLedgerPage execute(Connection c)
                            throws StoreServiceException {
                        account(c, session.getUserId(), false);
                        return repository.findLedger(c, session.getUserId(), safe);
                    }
                });
    }

    AccountDto recharge(final SessionContext session, final AccountRechargeRequest request)
            throws StoreServiceException {
        requirePurchase(session);
        validateRecharge(request);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<AccountDto>() {
                    @Override public AccountDto execute(Connection c) throws StoreServiceException {
                        AccountDto current = account(c, session.getUserId(), true);
                        LedgerRecord old = repository.findTransactionByKey(c,
                                request.getIdempotencyKey());
                        if (old != null) return idempotent(current, old, request.getAmount());
                        BigDecimal updated = current.getBalance().add(request.getAmount());
                        if (!repository.updateAccountBalance(c, current.getId(), current.getBalance(), updated)) {
                            throw new StoreServiceException(ResultCodes.CONFLICT, "账户余额已变化，请重试");
                        }
                        repository.insertTransaction(c, current.getId(), "RECHARGE", request.getAmount(),
                                current.getBalance(), updated, "ACCOUNT", session.getUserId(),
                                request.getIdempotencyKey(), session.getUserId(), request.getRemark());
                        return account(c, session.getUserId(), false);
                    }
                });
    }

    private AccountDto account(Connection c, long userId, boolean lock)
            throws StoreServiceException {
        AccountDto found = repository.findAccount(c, userId, lock);
        if (found == null) throw new StoreServiceException(ResultCodes.NOT_FOUND, "校园账户不存在");
        return found;
    }

    private static AccountDto idempotent(AccountDto current, LedgerRecord old, BigDecimal amount)
            throws StoreServiceException {
        if (old.getAccountId() != current.getId() || !"RECHARGE".equals(old.getTransactionType())
                || old.getAmount().compareTo(amount) != 0) {
            throw new StoreServiceException(ResultCodes.CONFLICT, "幂等键已用于其他操作");
        }
        return current;
    }

    private static void requirePurchase(SessionContext session) throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_PURCHASE);
    }

    private static void validateRecharge(AccountRechargeRequest request)
            throws StoreServiceException {
        if (request == null) throw new StoreServiceException(ResultCodes.INVALID_INPUT, "充值参数不正确");
        StoreServiceSupport.money(request.getAmount(), "充值", true);
        validateKey(request.getIdempotencyKey());
        StoreServiceSupport.maxLength(request.getRemark(), 500, "备注");
    }

    static void validateKey(String key) throws StoreServiceException {
        StoreServiceSupport.required(key, "幂等键");
        StoreServiceSupport.maxLength(key, 128, "幂等键");
    }
}
