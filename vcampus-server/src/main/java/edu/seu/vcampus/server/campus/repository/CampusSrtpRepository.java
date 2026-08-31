package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;

import java.sql.Connection;
import java.sql.SQLException;

/** V1/V2 srtp_records 表的持久化边界。 */
public interface CampusSrtpRepository {
    CampusPage<SrtpRecordDto> list(Connection connection, CampusPageQuery query,
                                   Long studentId) throws SQLException;
    SrtpRecordDto findSrtp(Connection connection, long id) throws SQLException;
    SrtpRecordDto lockSrtp(Connection connection, long id) throws SQLException;
    SrtpRecordDto save(Connection connection, SrtpSaveRequest request,
                      long defaultStudentId, long actorId, boolean administrator)
            throws SQLException;
    SrtpRecordDto review(Connection connection, SrtpStatusRequest request,
                         long reviewerId) throws SQLException;
}
