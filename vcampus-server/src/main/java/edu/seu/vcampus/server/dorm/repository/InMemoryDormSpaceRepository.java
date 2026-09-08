package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.sql.Connection;

/** 内存楼栋、房间和床位维护，使用共享状态保证并发写入原子性。 */
final class InMemoryDormSpaceRepository implements DormSpaceRepository {
    private final InMemoryDormState state;

    InMemoryDormSpaceRepository(InMemoryDormState state) { this.state = state; }

    @Override public synchronized DormBuildingDto createBuilding(Connection c, DormBuildingWriteRequest r, long actor) {
        ensureBuildingCode(r.getBuildingCode(), 0L);
        long id = state.nextBuilding++;
        DormBuildingDto value = building(id, r);
        state.buildings.put(id, value);
        return value;
    }

    @Override public synchronized DormBuildingDto updateBuilding(Connection c, DormBuildingWriteRequest r, long actor) {
        DormBuildingDto old = state.buildings.get(r.getId());
        if (old == null) throw missing("楼栋");
        ensureBuildingCode(r.getBuildingCode(), r.getId());
        DormBuildingDto value = building(r.getId(), r);
        state.buildings.put(r.getId(), value);
        return value;
    }

    @Override public synchronized DormRoomDto createRoom(Connection c, DormRoomWriteRequest r, long actor) {
        ensureBuilding(r.getBuildingId());
        ensureRoomNo(r.getBuildingId(), r.getRoomNo(), 0L);
        long id = state.nextRoom++;
        DormRoomDto value = room(id, r);
        state.rooms.put(id, value);
        return value;
    }

    @Override public synchronized DormRoomDto updateRoom(Connection c, DormRoomWriteRequest r, long actor) {
        if (!state.rooms.containsKey(r.getId())) throw missing("房间");
        ensureBuilding(r.getBuildingId());
        ensureRoomNo(r.getBuildingId(), r.getRoomNo(), r.getId());
        if (occupied(r.getId()) > r.getCapacity()) throw new DormRepositoryException(
                DormCommands.SPACE_OCCUPIED, "房间容量不能小于当前住宿人数");
        DormRoomDto value = room(r.getId(), r);
        state.rooms.put(r.getId(), value);
        return value;
    }

    @Override public synchronized DormBedDto createBed(Connection c, DormBedWriteRequest r, long actor) {
        ensureRoom(r.getRoomId());
        ensureBedNo(r.getRoomId(), r.getBedNo(), 0L);
        long id = state.nextBed++;
        DormBedDto value = bed(id, r, null);
        state.beds.put(id, value);
        return value;
    }

    @Override public synchronized DormBedDto updateBed(Connection c, DormBedWriteRequest r, long actor) {
        DormBedDto old = state.beds.get(r.getId());
        if (old == null) throw missing("床位");
        ensureRoom(r.getRoomId());
        ensureBedNo(r.getRoomId(), r.getBedNo(), r.getId());
        Long occupant = occupant(r.getId(), old);
        if (occupant != null && old.getRoomId() != r.getRoomId()) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "有活动住宿的床位不能更换房间");
        }
        if (occupant != null && !"OCCUPIED".equals(r.getStatus())) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "有活动住宿的床位不能设为非占用");
        }
        if (occupant == null && "OCCUPIED".equals(r.getStatus())) {
            throw new DormRepositoryException(DormCommands.SPACE_OCCUPIED, "没有住宿记录的床位不能设为占用");
        }
        DormBedDto value = bed(r.getId(), r, occupant);
        state.beds.put(r.getId(), value);
        return value;
    }

    private DormBuildingDto building(long id, DormBuildingWriteRequest r) {
        return new DormBuildingDto(id, r.getBuildingCode(), r.getBuildingName(), r.getAddress(),
                r.getGenderPolicy(), r.getStatus());
    }

    private DormRoomDto room(long id, DormRoomWriteRequest r) {
        DormBuildingDto building = state.buildings.get(r.getBuildingId());
        return new DormRoomDto(id, r.getBuildingId(), building.getBuildingCode(),
                building.getBuildingName(), r.getRoomNo(), r.getFloorNo(), r.getCapacity(),
                r.getRoomType(), r.getStatus(), r.getDescription(), occupied(id));
    }

    private DormBedDto bed(long id, DormBedWriteRequest r, Long occupant) {
        DormRoomDto room = state.rooms.get(r.getRoomId());
        DormBuildingDto building = state.buildings.get(room.getBuildingId());
        return new DormBedDto(id, r.getRoomId(), building.getBuildingCode(), room.getRoomNo(),
                r.getBedNo(), r.getStatus(), occupant);
    }

    private void ensureBuilding(long id) {
        if (!state.buildings.containsKey(id)) throw missing("楼栋");
    }

    private void ensureRoom(long id) {
        if (!state.rooms.containsKey(id)) throw missing("房间");
    }

    private void ensureBuildingCode(String code, long excluded) {
        for (DormBuildingDto value : state.buildings.values()) {
            if (value.getId() != excluded && value.getBuildingCode().equalsIgnoreCase(code)) {
                throw new DormRepositoryException(DormCommands.SPACE_DUPLICATE, "楼栋编码已存在");
            }
        }
    }

    private void ensureRoomNo(long buildingId, String roomNo, long excluded) {
        for (DormRoomDto value : state.rooms.values()) {
            if (value.getId() != excluded && value.getBuildingId() == buildingId
                    && value.getRoomNo().equalsIgnoreCase(roomNo)) {
                throw new DormRepositoryException(DormCommands.SPACE_DUPLICATE, "房间号已存在");
            }
        }
    }

    private void ensureBedNo(long roomId, String bedNo, long excluded) {
        for (DormBedDto value : state.beds.values()) {
            if (value.getId() != excluded && value.getRoomId() == roomId
                    && value.getBedNo().equalsIgnoreCase(bedNo)) {
                throw new DormRepositoryException(DormCommands.SPACE_DUPLICATE, "床位号已存在");
            }
        }
    }

    private int occupied(long roomId) {
        int count = 0;
        for (AccommodationDto value : state.accommodations.values()) {
            if (value.getRoomId() == roomId && "ACTIVE".equals(value.getStatus())) count++;
        }
        return count;
    }

    private Long occupant(long bedId, DormBedDto old) {
        if (old.getOccupantUserId() != null) return old.getOccupantUserId();
        for (AccommodationDto value : state.accommodations.values()) {
            if (value.getBedId() == bedId && "ACTIVE".equals(value.getStatus())) {
                return Long.valueOf(value.getStudentUserId());
            }
        }
        return null;
    }

    private static DormRepositoryException missing(String name) {
        return new DormRepositoryException(DormCommands.SPACE_NOT_FOUND, name + "不存在");
    }
}
