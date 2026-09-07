package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;
import edu.seu.vcampus.common.dto.store.AccountTransactionDto;

import java.math.BigDecimal;
import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 账户与流水内存仓储；支持幂等键和余额链测试。 */
final class InMemoryStoreAccountRepository implements StoreAccountRepository {
    private final InMemoryStoreState state;

    InMemoryStoreAccountRepository(InMemoryStoreState state) { this.state = state; }

    @Override
    public AccountDto findAccount(Connection c, long userId, boolean forUpdate) {
        synchronized (state) {
            InMemoryStoreState.MemoryAccount account = state.accounts.get(userId);
            return account == null ? null : toDto(account);
        }
    }

    @Override
    public AccountLedgerPage findLedger(Connection c, long userId, AccountLedgerQuery query) {
        AccountLedgerQuery q = query == null ? new AccountLedgerQuery() : query;
        synchronized (state) {
            InMemoryStoreState.MemoryAccount account = state.accounts.get(userId);
            if (account == null) return new AccountLedgerPage(Collections.<AccountTransactionDto>emptyList(),
                    q.getPage(), q.getPageSize(), 0L);
            List<AccountTransactionDto> found = new ArrayList<AccountTransactionDto>();
            List<AccountTransactionDto> all = state.ledgers.get(account.id);
            if (all != null) {
                for (AccountTransactionDto value : all) {
                    if (q.getTransactionType() == null || q.getTransactionType().equalsIgnoreCase(value.getTransactionType())) {
                        found.add(value);
                    }
                }
            }
            Collections.sort(found, new Comparator<AccountTransactionDto>() {
                @Override public int compare(AccountTransactionDto a, AccountTransactionDto b) {
                    LocalDateTime at = a.getCreatedAt();
                    LocalDateTime bt = b.getCreatedAt();
                    int byTime = bt.compareTo(at);
                    return byTime != 0 ? byTime : Long.compare(b.getId(), a.getId());
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new AccountLedgerPage(new ArrayList<AccountTransactionDto>(found.subList(from, to)),
                    q.getPage(), q.getPageSize(), found.size());
        }
    }

    @Override
    public LedgerRecord findTransactionByKey(Connection c, String key) {
        synchronized (state) { return state.keys.get(key); }
    }

    @Override
    public AccountDto findOrderPaymentAccount(Connection c, long orderId, boolean forUpdate) {
        synchronized (state) {
            for (List<AccountTransactionDto> rows : state.ledgers.values()) {
                for (AccountTransactionDto value : rows) {
                    if ("PURCHASE".equals(value.getTransactionType())
                            && "STORE_ORDER".equals(value.getReferenceType())
                            && value.getReferenceId() != null
                            && value.getReferenceId().longValue() == orderId) {
                        for (InMemoryStoreState.MemoryAccount account : state.accounts.values()) {
                            if (account.id == value.getAccountId()) return toDto(account);
                        }
                    }
                }
            }
            return null;
        }
    }

    @Override
    public boolean updateAccountBalance(Connection c, long accountId, BigDecimal expected,
                                       BigDecimal updated) {
        synchronized (state) {
            for (InMemoryStoreState.MemoryAccount account : state.accounts.values()) {
                if (account.id == accountId && "ACTIVE".equals(account.status)
                        && account.balance.compareTo(expected) == 0) {
                    account.balance = updated;
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public long insertTransaction(Connection c, long accountId, String type, BigDecimal amount,
                                  BigDecimal before, BigDecimal after, String referenceType,
                                  Long referenceId, String key, long operatorId, String remark) {
        synchronized (state) {
            if (state.keys.containsKey(key)) throw new StoreRepositoryException("幂等键已存在");
            long id = state.transactionSequence++;
            AccountTransactionDto value = new AccountTransactionDto(id, accountId, type, amount,
                    before, after, referenceType, referenceId, key, Long.valueOf(operatorId),
                    remark, LocalDateTime.now());
            List<AccountTransactionDto> ledger = state.ledgers.get(accountId);
            if (ledger == null) {
                ledger = new ArrayList<AccountTransactionDto>();
                state.ledgers.put(accountId, ledger);
            }
            ledger.add(value);
            state.keys.put(key, new LedgerRecord(accountId, amount, referenceType, referenceId, type));
            return id;
        }
    }

    void add(AccountDto value) {
        synchronized (state) {
            long id = value.getId() > 0 ? value.getId() : state.accountSequence++;
            state.accounts.put(value.getUserId(), new InMemoryStoreState.MemoryAccount(id,
                    value.getUserId(), value.getBalance(), value.getStatus()));
            state.accountSequence = Math.max(state.accountSequence, id + 1L);
        }
    }

    void addTransaction(AccountTransactionDto value) {
        synchronized (state) {
            List<AccountTransactionDto> ledger = state.ledgers.get(value.getAccountId());
            if (ledger == null) {
                ledger = new ArrayList<AccountTransactionDto>();
                state.ledgers.put(value.getAccountId(), ledger);
            }
            ledger.add(value);
            state.keys.put(value.getIdempotencyKey(), new LedgerRecord(value.getAccountId(),
                    value.getAmount(), value.getReferenceType(), value.getReferenceId(),
                    value.getTransactionType()));
            state.transactionSequence = Math.max(state.transactionSequence, value.getId() + 1L);
        }
    }

    AccountDto account(long userId) {
        synchronized (state) {
            InMemoryStoreState.MemoryAccount value = state.accounts.get(userId);
            return value == null ? null : toDto(value);
        }
    }

    private static AccountDto toDto(InMemoryStoreState.MemoryAccount account) {
        return new AccountDto(account.id, account.userId, account.balance, account.status);
    }
}
