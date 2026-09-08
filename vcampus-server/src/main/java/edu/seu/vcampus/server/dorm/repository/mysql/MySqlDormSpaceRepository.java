package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryException;
import edu.seu.vcampus.server.dorm.repository.DormSpaceRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** 楼栋、房间、床位维护 DAO；不提供物理删除。 */
public final class MySqlDormSpaceRepository implements DormSpaceRepository {
    private static final String BED_COLUMNS = "b.id,b.room_id,db.building_code,dr.room_no,b.bed_no,b.status,"
            + "ar.student_user_id AS occupant_user_id";
    @Override public DormBuildingDto createBuilding(Connection c, DormBuildingWriteRequest r, long actor)
            throws SQLException {
        ensureBuildingCode(c, r.getBuildingCode(), 0L);
        long id;
        String sql = "INSERT INTO dorm_buildings(building_code,building_name,address,gender_policy,status)"
                + " VALUES(?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, r.getBuildingCode()); s.setString(2, r.getBuildingName());
            s.setString(3, r.getAddress()); s.setString(4, r.getGenderPolicy()); s.setString(5, r.getStatus());
            s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("building id was not generated");
                id = keys.getLong(1);
            }
        }
        return building(c, id, false);
    }
    @Override public DormBuildingDto updateBuilding(Connection c, DormBuildingWriteRequest r, long actor)
            throws SQLException {
        ensureBuildingCode(c, r.getBuildingCode(), r.getId());
        String sql = "UPDATE dorm_buildings SET building_code=?,building_name=?,address=?,gender_policy=?,status=?"
                + " WHERE id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, r.getBuildingCode()); s.setString(2, r.getBuildingName());
            s.setString(3, r.getAddress()); s.setString(4, r.getGenderPolicy()); s.setString(5, r.getStatus());
            s.setLong(6, r.getId());
            if (s.executeUpdate() != 1) throw missing("楼栋");
        }
        return building(c, r.getId(), false);
    }
    @Override public DormRoomDto createRoom(Connection c, DormRoomWriteRequest r, long actor)
            throws SQLException {
        ensureBuilding(c, r.getBuildingId());
        ensureRoomNo(c, r.getBuildingId(), r.getRoomNo(), 0L);
        long id;
        String sql = "INSERT INTO dorm_rooms(building_id,room_no,floor_no,capacity,room_type,status,description)"
                + " VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindRoom(s, r); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("room id was not generated");
                id = keys.getLong(1);
            }
        }
        return room(c, id, false);
    }

    @Override public DormRoomDto updateRoom(Connection c, DormRoomWriteRequest r, long actor)
            throws SQLException {
        ensureBuilding(c, r.getBuildingId());
        ensureRoomNo(c, r.getBuildingId(), r.getRoomNo(), r.getId());
        if (occupied(c, r.getId()) > r.getCapacity()) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "房间容量不能小于当前住宿人数");
        }
        String sql = "UPDATE dorm_rooms SET building_id=?,room_no=?,floor_no=?,capacity=?,room_type=?,status=?,description=?"
                + " WHERE id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bindRoom(s, r); s.setLong(8, r.getId());
            if (s.executeUpdate() != 1) throw missing("房间");
        }
        return room(c, r.getId(), false);
    }

    @Override public DormBedDto createBed(Connection c, DormBedWriteRequest r, long actor)
            throws SQLException {
        ensureRoom(c, r.getRoomId());
        ensureBedNo(c, r.getRoomId(), r.getBedNo(), 0L);
        long id;
        try (PreparedStatement s = c.prepareStatement(
                "INSERT INTO dorm_beds(room_id,bed_no,status) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            s.setLong(1, r.getRoomId()); s.setString(2, r.getBedNo()); s.setString(3, r.getStatus()); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("bed id was not generated");
                id = keys.getLong(1);
            }
        }
        return bed(c, id, false);
    }

    @Override public DormBedDto updateBed(Connection c, DormBedWriteRequest r, long actor)
            throws SQLException {
        DormBedDto old = bed(c, r.getId(), true);
        if (old == null) throw missing("床位");
        ensureRoom(c, r.getRoomId());
        ensureBedNo(c, r.getRoomId(), r.getBedNo(), r.getId());
        if (old.getOccupantUserId() != null && old.getRoomId() != r.getRoomId()) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "有活动住宿的床位不能更换房间");
        }
        if (old.getOccupantUserId() != null && !"OCCUPIED".equals(r.getStatus())) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "有活动住宿的床位不能设为非占用");
        }
        if (old.getOccupantUserId() == null && "OCCUPIED".equals(r.getStatus())) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "没有住宿记录的床位不能设为占用");
        }
        try (PreparedStatement s = c.prepareStatement(
                "UPDATE dorm_beds SET room_id=?,bed_no=?,status=? WHERE id=?")) {
            s.setLong(1, r.getRoomId()); s.setString(2, r.getBedNo()); s.setString(3, r.getStatus()); s.setLong(4, r.getId());
            if (s.executeUpdate() != 1) throw missing("床位");
        }
        return bed(c, r.getId(), false);
    }

    private static void bindRoom(PreparedStatement s, DormRoomWriteRequest r) throws SQLException {
        s.setLong(1, r.getBuildingId()); s.setString(2, r.getRoomNo()); s.setInt(3, r.getFloorNo());
        s.setInt(4, r.getCapacity()); s.setString(5, r.getRoomType()); s.setString(6, r.getStatus());
        s.setString(7, r.getDescription());
    }

    private static DormBuildingDto building(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT id,building_code,building_name,address,gender_policy,status FROM dorm_buildings WHERE id=?"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.building(r) : null; }
        }
    }

    private static DormRoomDto room(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT dr.id,dr.building_id,db.building_code,db.building_name,dr.room_no,dr.floor_no,dr.capacity,"
                + "dr.room_type,dr.status,dr.description,(SELECT COUNT(*) FROM dorm_beds b WHERE b.room_id=dr.id AND b.status='OCCUPIED') occupied_beds"
                + " FROM dorm_rooms dr JOIN dorm_buildings db ON db.id=dr.building_id WHERE dr.id=?"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.room(r) : null; }
        }
    }

    private static DormBedDto bed(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + BED_COLUMNS + " FROM dorm_beds b JOIN dorm_rooms dr ON dr.id=b.room_id"
                + " JOIN dorm_buildings db ON db.id=dr.building_id LEFT JOIN accommodation_records ar"
                + " ON ar.bed_id=b.id AND ar.status='ACTIVE' WHERE b.id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.bed(r) : null; }
        }
    }

    private static int occupied(Connection c, long roomId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM accommodation_records ar"
                + " JOIN dorm_beds b ON b.id=ar.bed_id WHERE ar.status='ACTIVE' AND b.room_id=?")) {
            s.setLong(1, roomId); try (ResultSet r = s.executeQuery()) { r.next(); return r.getInt(1); }
        }
    }

    private static void ensureBuilding(Connection c, long id) throws SQLException {
        if (!exists(c, "SELECT 1 FROM dorm_buildings WHERE id=?", id)) throw missing("楼栋");
    }
    private static void ensureRoom(Connection c, long id) throws SQLException {
        if (!exists(c, "SELECT 1 FROM dorm_rooms WHERE id=?", id)) throw missing("房间");
    }
    private static void ensureBuildingCode(Connection c, String code, long excluded) throws SQLException {
        if (existsPair(c, "SELECT 1 FROM dorm_buildings WHERE building_code=? AND id<>?", code, excluded)) {
            throw new DormRepositoryException(DormCommands.SPACE_DUPLICATE, "楼栋编码已存在");
        }
    }
    private static void ensureRoomNo(Connection c, long building, String no, long excluded) throws SQLException {
        if (existsTriple(c, "SELECT 1 FROM dorm_rooms WHERE building_id=? AND room_no=? AND id<>?", building, no, excluded)) {
            throw new DormRepositoryException(DormCommands.SPACE_DUPLICATE, "房间号已存在");
        }
    }
    private static void ensureBedNo(Connection c, long room, String no, long excluded) throws SQLException {
        if (existsTriple(c, "SELECT 1 FROM dorm_beds WHERE room_id=? AND bed_no=? AND id<>?", room, no, excluded)) {
            throw new DormRepositoryException(DormCommands.SPACE_DUPLICATE, "床位号已存在");
        }
    }
    private static boolean exists(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next(); } }
    }
    private static boolean existsPair(Connection c, String sql, String value, long excluded) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setString(1, value); s.setLong(2, excluded); try (ResultSet r = s.executeQuery()) { return r.next(); } }
    }
    private static boolean existsTriple(Connection c, String sql, long first, String second, long excluded) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, first); s.setString(2, second); s.setLong(3, excluded); try (ResultSet r = s.executeQuery()) { return r.next(); } }
    }

    private static DormRepositoryException missing(String name) {
        return new DormRepositoryException(DormCommands.SPACE_NOT_FOUND, name + "不存在");
    }
}
