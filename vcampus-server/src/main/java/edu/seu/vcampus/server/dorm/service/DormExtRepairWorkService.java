package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairAssignRequest;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkOrderDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkerDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkRequest;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormExtRepository;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;

/**
 * 维修员的报修工作流：看队列、接单、开工、完工。
 *
 * <p>处理人一律取自会话，不接受客户端传入——否则一个维修员可以把单子接到同事名下，
 * 或者替别人报完工。三个写操作都靠仓储层「带条件的 UPDATE」判定成败，这里只负责把
 * 受影响行数翻译成人能看懂的话。</p>
 *
 * <p>没有「退回」动作：工单退回涉及重新派单和对学生的解释，属于宿管的职责，维修员
 * 单方面把单子扔回队列会让它在两边之间反复横跳。</p>
 */
final class DormExtRepairWorkService extends DormServiceSupport {
    private final DormExtRepository repository;

    DormExtRepairWorkService(DormExtRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    DormPage<RepairWorkOrderDto> queue(SessionContext session, DormPageQuery query) {
        require(session, Permission.DORM_REPAIR_WORK);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        DormExtValidation.page(q);
        return execute(new Work<DormPage<RepairWorkOrderDto>>() {
            @Override public DormPage<RepairWorkOrderDto> run(Connection c) throws Exception {
                return repository.repairQueue(c, q);
            }
        });
    }

    DormPage<RepairWorkOrderDto> assigned(final SessionContext session, DormPageQuery query, final boolean active) {
        require(session, Permission.DORM_REPAIR_WORK);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        DormExtValidation.page(q);
        return execute(new Work<DormPage<RepairWorkOrderDto>>() {
            @Override public DormPage<RepairWorkOrderDto> run(Connection c) throws Exception {
                return repository.repairAssigned(c, session.getUserId(), q, active);
            }
        });
    }

    // ---------- 宿管侧：派单 ----------

    java.util.List<RepairWorkerDto> workers(SessionContext session) {
        require(session, Permission.DORM_GOVERN);
        return execute(new Work<java.util.List<RepairWorkerDto>>() {
            @Override public java.util.List<RepairWorkerDto> run(Connection c) throws Exception {
                return repository.repairWorkers(c);
            }
        });
    }

    RepairWorkOrderDto detail(SessionContext session, final long orderId) {
        require(session, Permission.DORM_GOVERN);
        DormExtValidation.id(orderId, "报修工单");
        return execute(new Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run(Connection c) throws Exception {
                RepairWorkOrderDto value = repository.findRepairForManager(c, orderId);
                if (value == null) throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修工单不存在");
                return value;
            }
        });
    }

    RepairWorkOrderDto assign(SessionContext session, RepairAssignRequest request) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "派单参数不能为空");
        DormExtValidation.id(request.getOrderId(), "报修工单");
        DormExtValidation.id(request.getWorkerUserId(), "维修员");
        final long orderId = request.getOrderId();
        final long workerId = request.getWorkerUserId();
        return execute(new Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run(Connection c) throws Exception {
                if (repository.assignRepair(c, orderId, workerId) == 0) {
                    RepairWorkOrderDto current = repository.findRepairForManager(c, orderId);
                    if (current == null) throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修工单不存在");
                    throw new DormException(DormExtCommands.REPAIR_INVALID_STATE,
                            "工单已经" + ("COMPLETED".equals(current.getStatus()) ? "完工" : "取消") + "，不能再派单");
                }
                return repository.findRepairForManager(c, orderId);
            }
        });
    }

    /**
     * 宿管审核维修员报上来的完工。
     *
     * @param approved 通过则终结工单，打回则退回处理中由同一个维修员继续
     */
    RepairWorkOrderDto review(SessionContext session, RepairWorkRequest request, final boolean approved) {
        require(session, Permission.DORM_GOVERN);
        if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "审核参数不能为空");
        DormExtValidation.id(request.getOrderId(), "报修工单");
        final long orderId = request.getOrderId();
        return execute(new Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run(Connection c) throws Exception {
                if (repository.reviewRepair(c, orderId, approved) == 0) {
                    RepairWorkOrderDto current = repository.findRepairForManager(c, orderId);
                    if (current == null) throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修工单不存在");
                    throw new DormException(DormExtCommands.REPAIR_INVALID_STATE,
                            "只有维修员报完工、状态为「待宿管审核」的工单才能审核；当前是"
                                    + current.getStatus());
                }
                return repository.findRepairForManager(c, orderId);
            }
        });
    }

    RepairWorkOrderDto accept(final SessionContext session, RepairWorkRequest request) {
        return transition(session, request, Step.ACCEPT);
    }

    RepairWorkOrderDto start(final SessionContext session, RepairWorkRequest request) {
        return transition(session, request, Step.START);
    }

    RepairWorkOrderDto finish(final SessionContext session, RepairWorkRequest request) {
        return transition(session, request, Step.FINISH);
    }

    private enum Step { ACCEPT, START, FINISH }

    private RepairWorkOrderDto transition(final SessionContext session, RepairWorkRequest request, final Step step) {
        require(session, Permission.DORM_REPAIR_WORK);
        if (request == null) throw new DormException(DormExtCommands.INVALID_INPUT, "工单参数不能为空");
        DormExtValidation.id(request.getOrderId(), "报修工单");
        final long orderId = request.getOrderId();
        return execute(new Work<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto run(Connection c) throws Exception {
                long handler = session.getUserId();
                int changed = step == Step.ACCEPT ? repository.claimRepair(c, orderId, handler)
                        : step == Step.START ? repository.startRepair(c, orderId, handler)
                        : repository.finishRepair(c, orderId, handler);
                if (changed == 0) reject(c, orderId, handler, step);
                RepairWorkOrderDto value = repository.findRepairWork(c, orderId, handler);
                if (value == null) throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修工单不存在");
                return value;
            }
        });
    }

    /**
     * 更新没命中时，回查一次把原因说清楚。
     *
     * <p>三种情况对维修员来说完全不同：单子不存在、已经被同事接走、当前状态不该做这
     * 一步。统一回一句「操作失败」会让人反复点同一个按钮。</p>
     */
    private void reject(Connection c, long orderId, long handler, Step step) throws Exception {
        RepairWorkOrderDto current = repository.findRepairWork(c, orderId, handler);
        if (current == null) throw new DormException(DormExtCommands.REPAIR_NOT_FOUND, "报修工单不存在");
        if (step == Step.ACCEPT) {
            throw new DormException(DormExtCommands.REPAIR_ALREADY_TAKEN,
                    current.getHandlerId() == null ? "该工单当前不可接单" : "该工单已被其他维修员接走");
        }
        if (current.getHandlerId() == null || current.getHandlerId().longValue() != handler) {
            throw new DormException(DormExtCommands.REPAIR_ALREADY_TAKEN, "只能操作派给自己的工单");
        }
        throw new DormException(DormExtCommands.REPAIR_INVALID_STATE,
                "当前状态（" + current.getStatus() + "）不能执行该操作");
    }
}
