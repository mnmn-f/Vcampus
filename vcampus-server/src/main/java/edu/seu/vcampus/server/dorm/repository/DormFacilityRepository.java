package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;

import java.sql.Connection;
import java.sql.SQLException;

/** 楼栋、房间、床位只读边界。 */
public interface DormFacilityRepository {
    DormPage<DormBuildingDto> listBuildings(Connection connection, DormPageQuery query)
            throws SQLException;

    DormPage<DormRoomDto> listRooms(Connection connection, DormPageQuery query)
            throws SQLException;

    DormPage<DormBedDto> listBeds(Connection connection, DormPageQuery query,
                                  boolean includeOccupant) throws SQLException;

    DormBedDto findBed(Connection connection, long bedId, boolean forUpdate)
            throws SQLException;

    boolean roomExists(Connection connection, long roomId) throws SQLException;
}
