package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;

import java.sql.Connection;
import java.sql.SQLException;

/** 公告读取与发布持久化边界。 */
public interface CampusAnnouncementRepository {
    CampusPage<CampusAnnouncementDto> list(Connection connection, CampusPageQuery query,
                                            String moduleCode, String activeRoleCode,
                                            boolean includeDrafts) throws SQLException;
    CampusAnnouncementDto findAnnouncement(Connection connection, long id) throws SQLException;
    CampusAnnouncementDto lockAnnouncement(Connection connection, long id) throws SQLException;
    CampusAnnouncementDto save(Connection connection, CampusAnnouncementSaveRequest request,
                               long publisherId) throws SQLException;
}
