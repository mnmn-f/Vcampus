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

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;

import java.sql.Connection;
import java.sql.SQLException;

/** 门禁、未归、卫生和报修治理边界。 */
public interface DormGovernanceRepository {
    AccessRecordDto addAccess(Connection connection, long studentUserId,
                              AccessRecordDto record) throws SQLException;

    /**
     * 当前门禁时段：{@code [0]} 门禁时间、{@code [1]} 凌晨界限。策略表还没建或没有那一行时
     * 退回默认的 23:00 / 05:00，登记进出不能因为策略缺失而失败。
     */
    LocalTime[] accessPolicy(Connection connection) throws SQLException;

    /**
     * 为一次晚归归宿开一条待处理预警。同一学生同一天已经有预警（不论状态）就不再开，
     * 返回是否真的新建了一条。
     */
    boolean openLateAlert(Connection connection, long studentUserId, LocalDate alertDate,
                          LocalDateTime detectedAt) throws SQLException;

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
