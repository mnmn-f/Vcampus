package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.AccountCancellationPage;
import edu.seu.vcampus.common.dto.identity.AccountCancellationQuery;

import java.sql.Connection;

/** 账号注销申请的独立读写边界。 */
public interface IdentityCancellationRepository {
    AccountCancellationPage searchAccountCancellations(Connection connection,
                                                        AccountCancellationQuery query,
                                                        Long userId);

    AccountCancellationRecord findAccountCancellation(Connection connection,
                                                                  long requestId,
                                                                  boolean forUpdate);

    AccountCancellationRecord findPendingAccountCancellation(Connection connection,
                                                                         long userId,
                                                                         boolean forUpdate);

    long insertAccountCancellation(Connection connection, long userId, String reason);

    boolean withdrawAccountCancellation(Connection connection, long requestId, long userId);

    boolean reviewAccountCancellation(Connection connection, long requestId, String status,
                                      long reviewerId, String remark);
}
