package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.dorm.UtilityPaymentRequest;

import java.sql.Connection;
import java.sql.SQLException;

/** 水电账单和本人分摊缴费边界。 */
public interface DormBillingRepository {
    DormPage<UtilityBillDto> listBills(Connection connection, long studentUserId,
                                       DormPageQuery query) throws SQLException;

    DormPage<UtilityBillDto> listAllBills(Connection connection, UtilityBillQuery query)
            throws SQLException;

    UtilityBillDto lockAllocation(Connection connection, long studentUserId,
                                  long allocationId) throws SQLException;

    UtilityBillDto pay(Connection connection, long studentUserId,
                       UtilityPaymentRequest request) throws SQLException;
}
