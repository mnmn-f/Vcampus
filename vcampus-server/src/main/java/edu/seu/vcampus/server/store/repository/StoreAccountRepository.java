package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.AccountDto;
import edu.seu.vcampus.common.dto.store.AccountLedgerPage;
import edu.seu.vcampus.common.dto.store.AccountLedgerQuery;

import java.math.BigDecimal;
import java.sql.Connection;

/** accounts/account_transactions 表的事务读写边界。 */
public interface StoreAccountRepository {
    AccountDto findAccount(Connection connection, long userId, boolean forUpdate);
    AccountLedgerPage findLedger(Connection connection, long userId,
                                 AccountLedgerQuery query);
    LedgerRecord findTransactionByKey(Connection connection,
                                                String idempotencyKey);
    AccountDto findOrderPaymentAccount(Connection connection, long orderId,
                                       boolean forUpdate);
    boolean updateAccountBalance(Connection connection, long accountId,
                                 BigDecimal expectedBalance, BigDecimal newBalance);
    long insertTransaction(Connection connection, long accountId, String type,
                           BigDecimal amount, BigDecimal before, BigDecimal after,
                           String referenceType, Long referenceId,
                           String idempotencyKey, long operatorId, String remark);
}
