package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccessRecordDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionDto;
import edu.seu.vcampus.common.dto.dorm.HygieneInspectionRequest;
import edu.seu.vcampus.common.dto.dorm.LateReturnAlertDto;
import edu.seu.vcampus.common.dto.dorm.LateReturnHandleRequest;
import edu.seu.vcampus.common.dto.dorm.RepairCreateRequest;
import edu.seu.vcampus.common.dto.dorm.RepairEvaluationRequest;
import edu.seu.vcampus.common.dto.dorm.RepairOrderDto;
import edu.seu.vcampus.common.dto.dorm.RepairStatusRequest;

import java.sql.Connection;
import java.sql.SQLException;

/** 门禁、未归、卫生和报修治理边界。 */
public interface DormGovernanceRepository {
    AccessRecordDto addAccess(Connection connection, long studentUserId,
                              AccessRecordDto record) throws SQLException;

    DormPage<AccessRecordDto> listAccess(Connection connection, Long studentUserId,
                                         DormPageQuery query) throws SQLException;

    DormPage<LateReturnAlertDto> listAlerts(Connection connection, Long studentUserId,
                                            DormPageQuery query) throws SQLException;

    LateReturnAlertDto lockAlert(Connection connection, long alertId) throws SQLException;

    LateReturnAlertDto handleAlert(Connection connection, long alertId, long handlerId,
                                   LateReturnHandleRequest request) throws SQLException;

    DormPage<HygieneInspectionDto> listHygiene(Connection connection, DormPageQuery query)
            throws SQLException;

    HygieneInspectionDto saveHygiene(Connection connection, HygieneInspectionRequest request,
                                     long inspectorId) throws SQLException;

    DormPage<RepairOrderDto> listRepairs(Connection connection, Long reporterId,
                                         DormPageQuery query) throws SQLException;

    RepairOrderDto createRepair(Connection connection, RepairCreateRequest request,
                                long reporterId) throws SQLException;

    RepairOrderDto lockRepair(Connection connection, long orderId) throws SQLException;

    RepairOrderDto updateRepair(Connection connection, RepairStatusRequest request,
                                long actorId) throws SQLException;

    RepairOrderDto evaluateRepair(Connection connection, RepairEvaluationRequest request,
                                  long studentId) throws SQLException;
}
