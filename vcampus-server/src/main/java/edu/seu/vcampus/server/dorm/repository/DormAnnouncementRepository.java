package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.dorm.DormAnnouncementDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;

import java.sql.Connection;
import java.sql.SQLException;

/** 宿舍公告查询与管理边界。 */
public interface DormAnnouncementRepository {
    DormPage<DormAnnouncementDto> list(Connection connection, DormPageQuery query,
                                        boolean includeDrafts) throws SQLException;

    DormAnnouncementDto save(Connection connection, AnnouncementSaveRequest request,
                             long publisherId) throws SQLException;
}
