package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBedWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingWriteRequest;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.dto.dorm.DormRoomWriteRequest;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.security.SessionContext;

/** 宿管维护楼栋、房间、床位；删除和住宿占用伪造均不提供入口。 */
final class DormSpaceService extends DormServiceSupport {
    private final DormRepository repository;

    DormSpaceService(DormRepository repository, TransactionManager transactions) {
        super(transactions); this.repository = repository;
    }

    DormBuildingDto createBuilding(final SessionContext s, final DormBuildingWriteRequest r) {
        require(s, Permission.DORM_MANAGE); final DormBuildingWriteRequest value = building(r, false);
        return execute(new Work<DormBuildingDto>() { public DormBuildingDto run(java.sql.Connection c) throws Exception { return repository.createBuilding(c, value, s.getUserId()); } });
    }
    DormBuildingDto updateBuilding(final SessionContext s, final DormBuildingWriteRequest r) {
        require(s, Permission.DORM_MANAGE); final DormBuildingWriteRequest value = building(r, true);
        return execute(new Work<DormBuildingDto>() { public DormBuildingDto run(java.sql.Connection c) throws Exception { return repository.updateBuilding(c, value, s.getUserId()); } });
    }
    DormRoomDto createRoom(final SessionContext s, final DormRoomWriteRequest r) {
        require(s, Permission.DORM_MANAGE); final DormRoomWriteRequest value = room(r, false);
        return execute(new Work<DormRoomDto>() { public DormRoomDto run(java.sql.Connection c) throws Exception { return repository.createRoom(c, value, s.getUserId()); } });
    }
    DormRoomDto updateRoom(final SessionContext s, final DormRoomWriteRequest r) {
        require(s, Permission.DORM_MANAGE); final DormRoomWriteRequest value = room(r, true);
        return execute(new Work<DormRoomDto>() { public DormRoomDto run(java.sql.Connection c) throws Exception { return repository.updateRoom(c, value, s.getUserId()); } });
    }
    DormBedDto createBed(final SessionContext s, final DormBedWriteRequest r) {
        require(s, Permission.DORM_MANAGE); final DormBedWriteRequest value = bed(r, false);
        if ("OCCUPIED".equals(value.getStatus())) throw new DormException(DormCommands.SPACE_OCCUPIED, "新增床位不能直接占用");
        return execute(new Work<DormBedDto>() { public DormBedDto run(java.sql.Connection c) throws Exception { return repository.createBed(c, value, s.getUserId()); } });
    }
    DormBedDto updateBed(final SessionContext s, final DormBedWriteRequest r) {
        require(s, Permission.DORM_MANAGE); final DormBedWriteRequest value = bed(r, true);
        return execute(new Work<DormBedDto>() { public DormBedDto run(java.sql.Connection c) throws Exception { return repository.updateBed(c, value, s.getUserId()); } });
    }

    private static DormBuildingWriteRequest building(DormBuildingWriteRequest r, boolean update) {
        if (r == null || (update && r.getId() <= 0)) throw invalid("楼栋参数不正确");
        String code = text(r.getBuildingCode(), "楼栋编码");
        String name = text(r.getBuildingName(), "楼栋名称");
        max(code, 64, "楼栋编码"); max(name, 200, "楼栋名称"); max(r.getAddress(), 255, "地址");
        String gender = enumValue(r.getGenderPolicy(), "MALE", "FEMALE", "MIXED");
        String status = enumValue(r.getStatus(), "OPEN", "MAINTENANCE", "CLOSED");
        return new DormBuildingWriteRequest(r.getId(), code, name, optional(r.getAddress()), gender, status);
    }

    private static DormRoomWriteRequest room(DormRoomWriteRequest r, boolean update) {
        if (r == null || (update && r.getId() <= 0)) throw invalid("房间参数不正确");
        validateId(r.getBuildingId(), "楼栋"); text(r.getRoomNo(), "房间号");
        if (r.getCapacity() <= 0) throw invalid("房间容量必须大于零");
        max(r.getRoomNo(), 64, "房间号"); max(r.getDescription(), 1000, "房间说明");
        return new DormRoomWriteRequest(r.getId(), r.getBuildingId(), r.getRoomNo().trim(), r.getFloorNo(),
                r.getCapacity(), enumValue(r.getRoomType(), "STANDARD", "SUITE", "SPECIAL"),
                enumValue(r.getStatus(), "AVAILABLE", "FULL", "MAINTENANCE", "CLOSED"), optional(r.getDescription()));
    }

    private static DormBedWriteRequest bed(DormBedWriteRequest r, boolean update) {
        if (r == null || (update && r.getId() <= 0)) throw invalid("床位参数不正确");
        validateId(r.getRoomId(), "房间"); String no = text(r.getBedNo(), "床位号"); max(no, 32, "床位号");
        return new DormBedWriteRequest(r.getId(), r.getRoomId(), no,
                enumValue(r.getStatus(), "AVAILABLE", "OCCUPIED", "MAINTENANCE"));
    }

    private static String enumValue(String value, String... allowed) {
        String normalized = text(value, "枚举值").toUpperCase();
        for (String item : allowed) if (item.equals(normalized)) return normalized;
        throw invalid("枚举值不正确");
    }
    private static String optional(String value) { return value == null ? null : value.trim(); }
    private static void max(String value, int length, String field) {
        if (value != null && value.length() > length) throw invalid(field + "长度超限");
    }
    private static void validateId(long value, String field) { if (value <= 0) throw invalid(field + "不正确"); }
    private static DormException invalid(String message) { return new DormException(DormCommands.INVALID_INPUT, message); }
}
