package edu.seu.vcampus.server.dorm.repository.mysql;

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
import edu.seu.vcampus.server.dorm.repository.DormGovernanceRepository;

import java.sql.Connection;
import java.sql.SQLException;

/** MySQL 治理仓储组合；门禁、未归、卫生、报修各自独立。 */
public final class MySqlDormGovernanceRepository implements DormGovernanceRepository {
    private final MySqlDormAccessRepository access = new MySqlDormAccessRepository();
    private final MySqlDormAlertRepository alerts = new MySqlDormAlertRepository();
    private final MySqlDormHygieneRepository hygiene = new MySqlDormHygieneRepository();
    private final MySqlDormRepairRepository repairs = new MySqlDormRepairRepository();

    @Override public AccessRecordDto addAccess(Connection c, long student, AccessRecordDto r) throws SQLException {
        return access.add(c, student, r);
    }
    @Override public DormPage<AccessRecordDto> listAccess(Connection c, Long student, DormPageQuery q) throws SQLException {
        return access.list(c, student, q);
    }
    @Override public DormPage<LateReturnAlertDto> listAlerts(Connection c, Long student, DormPageQuery q) throws SQLException {
        return alerts.list(c, student, q);
    }
    @Override public LateReturnAlertDto lockAlert(Connection c, long id) throws SQLException { return alerts.lock(c, id); }
    @Override public LateReturnAlertDto handleAlert(Connection c, long id, long actor, LateReturnHandleRequest r)
            throws SQLException { return alerts.handle(c, id, actor, r); }
    @Override public DormPage<HygieneInspectionDto> listHygiene(Connection c, DormPageQuery q) throws SQLException {
        return hygiene.list(c, q);
    }
    @Override public HygieneInspectionDto saveHygiene(Connection c, HygieneInspectionRequest r, long actor)
            throws SQLException { return hygiene.save(c, r, actor); }
    @Override public DormPage<RepairOrderDto> listRepairs(Connection c, Long reporter, DormPageQuery q) throws SQLException {
        return repairs.list(c, reporter, q);
    }
    @Override public RepairOrderDto createRepair(Connection c, RepairCreateRequest r, long reporter) throws SQLException {
        return repairs.create(c, r, reporter);
    }
    @Override public RepairOrderDto lockRepair(Connection c, long id) throws SQLException { return repairs.lock(c, id); }
    @Override public RepairOrderDto updateRepair(Connection c, RepairStatusRequest r, long actor) throws SQLException {
        return repairs.update(c, r, actor);
    }
    @Override public RepairOrderDto evaluateRepair(Connection c, RepairEvaluationRequest r, long student) throws SQLException {
        return repairs.evaluate(c, r, student);
    }
}
