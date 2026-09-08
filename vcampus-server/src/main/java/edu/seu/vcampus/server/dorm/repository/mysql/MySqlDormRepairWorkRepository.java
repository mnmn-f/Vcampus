package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkOrderDto;
import edu.seu.vcampus.common.dto.dorm.ext.RepairWorkerDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 维修员视角的报修工单查询与状态流转。
 *
 * <p>三个写操作都写成「带条件的 UPDATE」而不是「先查后改」：接单是两个维修员会同时
 * 点的按钮，先查再改中间隔着一次网络往返，两个人都会读到「还没人接」然后双双写进
 * 去，后写的把前一个人的名字覆盖掉。把前置条件放进 WHERE，数据库自己保证只有一次
 * 更新会命中，返回的受影响行数就是「抢到没有」。</p>
 */
final class MySqlDormRepairWorkRepository {
    private static final String COLUMNS =
            "o.id, o.room_id, b.building_name, r.room_no, o.category, o.description, o.priority,"
            + " o.status, o.reporter_id, u.display_name AS reporter_name, o.submitted_at,"
            + " o.accepted_at, o.completed_at, o.handler_id, o.evaluation_score,"
            + " COALESCE(p.allow_enter, 0) AS allow_enter, p.note AS entry_note, u.phone AS contact_phone";
    private static final String FROM =
            " FROM repair_orders o"
            + " JOIN dorm_rooms r ON r.id = o.room_id"
            + " JOIN dorm_buildings b ON b.id = r.building_id"
            + " LEFT JOIN users u ON u.id = o.reporter_id"
            + " LEFT JOIN dorm_repair_entry_permits p ON p.repair_order_id = o.id";

    /** 待接队列：已提交且还没有处理人。急件排前面，同级按提交时间先来先接。 */
    DormPage<RepairWorkOrderDto> queue(Connection c, DormPageQuery query) throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE o.status = 'SUBMITTED' AND o.handler_id IS NULL");
        keyword(where, params, q);
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where,
                "SELECT " + COLUMNS + FROM + where + priorityOrder() + " LIMIT ? OFFSET ?",
                params, q.getPage(), q.getPageSize(), reader(false));
    }

    /**
     * 派给本人的工单。
     *
     * @param active {@code true} 取在手的（已接单/处理中），{@code false} 取已了结的
     */
    DormPage<RepairWorkOrderDto> assigned(Connection c, long handlerId, DormPageQuery query, boolean active)
            throws SQLException {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<Object> params = new ArrayList<Object>();
        StringBuilder where = new StringBuilder(" WHERE o.handler_id = ? AND o.status IN ");
        // 报完工之后这单就不在维修员手上了，等宿管审核，所以归到「处理记录」那一侧。
        where.append(active ? "('ACCEPTED', 'IN_PROGRESS')" : "('PENDING_REVIEW', 'COMPLETED', 'CANCELLED')");
        params.add(Long.valueOf(handlerId));
        keyword(where, params, q);
        String order = active ? priorityOrder() : " ORDER BY o.completed_at DESC, o.id DESC";
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + FROM + where,
                "SELECT " + COLUMNS + FROM + where + order + " LIMIT ? OFFSET ?",
                params, q.getPage(), q.getPageSize(), reader(true));
    }

    RepairWorkOrderDto find(Connection c, long orderId, long handlerId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE o.id = ?")) {
            s.setLong(1, orderId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? new HandlerAwareReader(handlerId).read(r) : null;
            }
        }
    }

    /** @return 1 表示抢到了，0 表示单子不存在或已经被别人接走 */
    int claim(Connection c, long orderId, long handlerId) throws SQLException {
        String sql = "UPDATE repair_orders SET handler_id = ?, status = 'ACCEPTED',"
                + " accepted_at = CURRENT_TIMESTAMP(3)"
                + " WHERE id = ? AND status = 'SUBMITTED' AND handler_id IS NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, handlerId);
            s.setLong(2, orderId);
            return s.executeUpdate();
        }
    }

    /** @return 1 表示流转成功，0 表示不是本人的单或当前状态不允许开工 */
    int start(Connection c, long orderId, long handlerId) throws SQLException {
        String sql = "UPDATE repair_orders SET status = 'IN_PROGRESS'"
                + " WHERE id = ? AND handler_id = ? AND status = 'ACCEPTED'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, orderId);
            s.setLong(2, handlerId);
            return s.executeUpdate();
        }
    }

    /**
     * 维修员报完工：转「待宿管审核」，不直接终结。
     *
     * <p>completed_at 留到宿管审核通过时再写——它记的是这单真正了结的时间，维修员
     * 说完工只是他这一侧做完了。</p>
     *
     * @return 1 表示成功，0 表示不是本人的单或当前状态不允许报完工
     */
    int finish(Connection c, long orderId, long handlerId) throws SQLException {
        String sql = "UPDATE repair_orders SET status = 'PENDING_REVIEW'"
                + " WHERE id = ? AND handler_id = ? AND status IN ('ACCEPTED', 'IN_PROGRESS')";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, orderId);
            s.setLong(2, handlerId);
            return s.executeUpdate();
        }
    }

    /**
     * 宿管审核维修员报上来的完工。
     *
     * @param approved 通过则终结并写 completed_at；打回则退回处理中，交给同一个维修员
     * @return 1 表示成功，0 表示这单不在待审核状态
     */
    int review(Connection c, long orderId, boolean approved) throws SQLException {
        String sql = approved
                ? "UPDATE repair_orders SET status = 'COMPLETED', completed_at = CURRENT_TIMESTAMP(3)"
                        + " WHERE id = ? AND status = 'PENDING_REVIEW'"
                : "UPDATE repair_orders SET status = 'IN_PROGRESS'"
                        + " WHERE id = ? AND status = 'PENDING_REVIEW'";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, orderId);
            return s.executeUpdate();
        }
    }

    /**
     * 可派单的维修员：启用中的 REPAIR_WORKER 账号，附带各自在手的工单数。
     *
     * <p>在手工单数用左连接算，而不是先查人再逐个查单——维修员不多，但那种写法每加
     * 一个人就多一次往返，而且中间状态会变，算出来的数字彼此不同步。</p>
     */
    List<RepairWorkerDto> workers(Connection c) throws SQLException {
        String sql = "SELECT u.id, u.display_name,"
                + " COUNT(o.id) AS active_orders"
                + " FROM users u"
                + " JOIN user_roles ur ON ur.user_id = u.id"
                + " JOIN roles r ON r.id = ur.role_id AND r.code = 'REPAIR_WORKER'"
                + " LEFT JOIN repair_orders o ON o.handler_id = u.id"
                + "   AND o.status IN ('ACCEPTED', 'IN_PROGRESS')"
                + " WHERE u.status = 'ACTIVE'"
                + " GROUP BY u.id, u.display_name"
                + " ORDER BY active_orders ASC, u.display_name ASC";
        List<RepairWorkerDto> rows = new ArrayList<RepairWorkerDto>();
        try (PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) {
            while (r.next()) {
                rows.add(new RepairWorkerDto(r.getLong("id"), r.getString("display_name"),
                        r.getInt("active_orders")));
            }
        }
        return rows;
    }

    /**
     * 宿管派单。
     *
     * <p>已完工和已取消的单不允许改派：那时候活已经干完了，改派只会把台账搞乱。
     * 已经派给别人的单可以改派——这正是宿管该有的调度权，和维修员自己抢单不同。</p>
     */
    int assign(Connection c, long orderId, long workerId) throws SQLException {
        String sql = "UPDATE repair_orders SET handler_id = ?,"
                + " status = CASE WHEN status = 'SUBMITTED' THEN 'ACCEPTED' ELSE status END,"
                + " accepted_at = COALESCE(accepted_at, CURRENT_TIMESTAMP(3))"
                + " WHERE id = ? AND status IN ('SUBMITTED', 'ACCEPTED', 'IN_PROGRESS')";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, workerId);
            s.setLong(2, orderId);
            return s.executeUpdate();
        }
    }

    /** 宿管视角：联系电话照常给出，派单本来就要联系人。 */
    RepairWorkOrderDto findForManager(Connection c, long orderId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT " + COLUMNS + FROM + " WHERE o.id = ?")) {
            s.setLong(1, orderId);
            try (ResultSet r = s.executeQuery()) {
                return r.next() ? row(r, true) : null;
            }
        }
    }

    /** 急件先派：URGENT → HIGH → NORMAL → LOW，同级按提交时间先来先接。 */
    private static String priorityOrder() {
        return " ORDER BY FIELD(o.priority, 'URGENT', 'HIGH', 'NORMAL', 'LOW'), o.submitted_at ASC, o.id ASC";
    }

    private static void keyword(StringBuilder where, List<Object> params, DormPageQuery q) {
        String keyword = JdbcDormSupport.clean(q.getKeyword());
        if (keyword != null) {
            where.append(" AND (r.room_no LIKE ? OR b.building_name LIKE ? OR o.category LIKE ? OR o.description LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        String status = JdbcDormSupport.clean(q.getStatus());
        if (status != null) {
            where.append(" AND o.status = ?");
            params.add(status);
        }
    }

    private static JdbcDormSupport.Reader<RepairWorkOrderDto> reader(final boolean withPhone) {
        return new JdbcDormSupport.Reader<RepairWorkOrderDto>() {
            @Override public RepairWorkOrderDto read(ResultSet r) throws SQLException {
                return row(r, withPhone);
            }
        };
    }

    /** 同一份结果里既有队列单也有在手单，联系电话按「是不是我的单」逐行决定。 */
    private static final class HandlerAwareReader implements JdbcDormSupport.Reader<RepairWorkOrderDto> {
        private final long handlerId;
        HandlerAwareReader(long handlerId) { this.handlerId = handlerId; }
        @Override public RepairWorkOrderDto read(ResultSet r) throws SQLException {
            long handler = r.getLong("handler_id");
            return row(r, !r.wasNull() && handler == handlerId);
        }
    }

    private static RepairWorkOrderDto row(ResultSet r, boolean withPhone) throws SQLException {
        long reporter = r.getLong("reporter_id");
        Long reporterId = r.wasNull() ? null : Long.valueOf(reporter);
        long handler = r.getLong("handler_id");
        Long handlerId = r.wasNull() ? null : Long.valueOf(handler);
        int score = r.getInt("evaluation_score");
        Integer evaluation = r.wasNull() ? null : Integer.valueOf(score);
        return new RepairWorkOrderDto(r.getLong("id"), r.getLong("room_id"), r.getString("building_name"),
                r.getString("room_no"), r.getString("category"), r.getString("description"),
                r.getString("priority"), r.getString("status"), reporterId, r.getString("reporter_name"),
                JdbcDormSupport.localTimestamp(r, "submitted_at"),
                JdbcDormSupport.localTimestamp(r, "accepted_at"),
                JdbcDormSupport.localTimestamp(r, "completed_at"),
                handlerId, r.getInt("allow_enter") == 1, r.getString("entry_note"),
                withPhone ? r.getString("contact_phone") : null, evaluation);
    }
}
