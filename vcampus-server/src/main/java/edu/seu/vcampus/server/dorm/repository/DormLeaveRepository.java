package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.LeaveQuery;
import edu.seu.vcampus.common.dto.dorm.LeaveRequestDto;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;

import java.sql.Connection;
import java.sql.SQLException;
import org.threeten.bp.LocalDateTime;

/** 请假申请的分页、状态机和重叠检查边界。 */
public interface DormLeaveRepository {
    void lockStudent(Connection connection, long studentUserId) throws SQLException;

    boolean hasOverlap(Connection connection, long studentUserId, LocalDateTime startAt,
                       LocalDateTime endAt, Long excludedId) throws SQLException;

    LeaveRequestDto submit(Connection connection, long studentUserId,
                           LeaveSubmitRequest request) throws SQLException;

    edu.seu.vcampus.common.dto.dorm.DormPage<LeaveRequestDto> list(Connection connection,
                                                                     Long studentUserId,
                                                                     LeaveQuery query) throws SQLException;

    LeaveRequestDto lock(Connection connection, long leaveId) throws SQLException;

    LeaveRequestDto cancel(Connection connection, long leaveId) throws SQLException;

    LeaveRequestDto review(Connection connection, long leaveId, long reviewerId,
                           boolean approved, String remark) throws SQLException;
}
