package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;

import java.sql.Connection;
import java.sql.SQLException;

/** 楼栋、房间和床位的非删除维护边界。 */
public interface DormSpaceRepository {
    DormBuildingDto createBuilding(Connection connection, DormBuildingWriteRequest request,
                                   long actorId) throws SQLException;
    DormBuildingDto updateBuilding(Connection connection, DormBuildingWriteRequest request,
                                   long actorId) throws SQLException;
    DormRoomDto createRoom(Connection connection, DormRoomWriteRequest request,
                           long actorId) throws SQLException;
    DormRoomDto updateRoom(Connection connection, DormRoomWriteRequest request,
                           long actorId) throws SQLException;
    DormBedDto createBed(Connection connection, DormBedWriteRequest request,
                         long actorId) throws SQLException;
    DormBedDto updateBed(Connection connection, DormBedWriteRequest request,
                         long actorId) throws SQLException;
}
