package edu.seu.vcampus.server.dorm.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/** 宿舍对账户能力的最小边界，不依赖 store 服务包。 */
public interface DormPaymentPort {
    long charge(Connection connection, long userId, BigDecimal amount,
                String idempotencyKey, long allocationId) throws SQLException;
}
