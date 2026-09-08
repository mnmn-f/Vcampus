package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.AccommodationDto;
import edu.seu.vcampus.common.dto.dorm.AccommodationRequestDto;
import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.protocol.command.DormCommands;

import java.sql.Connection;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 内存住宿记录仓储；所有高风险变更在一个 synchronized 方法内完成。 */
final class InMemoryDormAccommodationRepository implements DormAccommodationRepository {
    private final InMemoryDormState state;

    InMemoryDormAccommodationRepository(InMemoryDormState state) { this.state = state; }

    @Override
    public synchronized AccommodationDto findCurrent(Connection c, long studentId, boolean lock) {
        for (AccommodationDto item : state.accommodations.values()) {
            if (item.getStudentUserId() == studentId && "ACTIVE".equals(item.getStatus())) return item;
        }
        return null;
    }

    @Override
    public synchronized AccommodationDto findById(Connection c, long id, boolean lock) {
        return state.accommodations.get(Long.valueOf(id));
    }

    @Override
    public synchronized AccommodationDto assign(Connection c, long studentId, long bedId,
                                                LocalDate date, long actor) {
        requireStudent(studentId);
        if (findCurrent(c, studentId, true) != null) {
            throw new DormRepositoryException(DormCommands.ALREADY_ACCOMMODATED, "学生已有有效住宿");
        }
        DormBedDto bed = requireAvailableBed(bedId);
        LocalDate start = date == null ? LocalDate.now() : date;
        AccommodationDto value = record(state.nextAccommodation++, studentId, bed, start, null,
                "ACTIVE");
        state.accommodations.put(Long.valueOf(value.getId()), value);
        replaceBed(bed, "OCCUPIED", Long.valueOf(studentId));
        return value;
    }

    @Override
    public synchronized AccommodationDto transfer(Connection c, long studentId, long recordId,
                                                  long targetBedId, LocalDate date, long actor) {
        requireStudent(studentId);
        AccommodationDto old = findCurrent(c, studentId, true);
        if (old == null || old.getId() != recordId) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        DormBedDto target = requireAvailableBed(targetBedId);
        DormBedDto source = state.beds.get(Long.valueOf(old.getBedId()));
        LocalDate end = date == null ? LocalDate.now() : date;
        state.accommodations.put(Long.valueOf(old.getId()), record(old.getId(), studentId,
                source == null ? target : source, old.getStartDate(), end, "ENDED"));
        if (source != null) replaceBed(source, "AVAILABLE", null);
        AccommodationDto value = record(state.nextAccommodation++, studentId, target, end, null,
                "ACTIVE");
        state.accommodations.put(Long.valueOf(value.getId()), value);
        replaceBed(target, "OCCUPIED", Long.valueOf(studentId));
        return value;
    }

    @Override
    public synchronized AccommodationDto checkout(Connection c, long studentId, long recordId,
                                                  LocalDate date, long actor) {
        requireStudent(studentId);
        AccommodationDto old = findCurrent(c, studentId, true);
        if (old == null || old.getId() != recordId) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        LocalDate end = date == null ? LocalDate.now() : date;
        DormBedDto bed = state.beds.get(Long.valueOf(old.getBedId()));
        state.accommodations.put(Long.valueOf(old.getId()), record(old.getId(), studentId,
                bed, old.getStartDate(), end, "ENDED"));
        if (bed != null) replaceBed(bed, "AVAILABLE", null);
        return state.accommodations.get(Long.valueOf(old.getId()));
    }

    @Override
    public synchronized AccommodationRequestDto submitRequest(Connection c, long studentId,
                                                               String type, Long currentId,
                                                               Long targetBedId, String reason) {
        requireStudent(studentId);
        if (type == null) throw new DormRepositoryException(DormCommands.INVALID_INPUT, "申请类型不能为空");
        for (AccommodationRequestDto item : state.requests.values()) {
            if (item.getStudentUserId() == studentId && "PENDING".equals(item.getStatus())) {
                throw new DormRepositoryException(DormCommands.REQUEST_DUPLICATE, "已有待审批住宿申请");
            }
        }
        AccommodationDto current = findCurrent(c, studentId, false);
        if ("CHECK_IN".equals(type) && current != null) {
            throw new DormRepositoryException(DormCommands.ALREADY_ACCOMMODATED, "学生已有有效住宿");
        }
        if ("TRANSFER".equals(type) && (current == null || currentId == null
                || current.getId() != currentId.longValue())) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        if ("CHECK_OUT".equals(type) && (current == null || currentId == null
                || current.getId() != currentId.longValue())) {
            throw new DormRepositoryException(DormCommands.ACCOMMODATION_NOT_FOUND, "当前住宿记录不存在");
        }
        // 床位不再由学生填：入住和调宿申请可以不带目标床位，交给宿管审批时统一调配。
        // 带了就校验一次，免得存进一条指向已占用床位的申请。
        if (targetBedId != null && ("CHECK_IN".equals(type) || "TRANSFER".equals(type))) {
            requireAvailableBed(targetBedId.longValue());
        }
        AccommodationRequestDto value = new AccommodationRequestDto(state.nextRequest++, studentId,
                type, currentId, targetBedId, reason, "PENDING", null, null, null,
                LocalDateTime.now());
        state.requests.put(Long.valueOf(value.getId()), value);
        return value;
    }

    @Override
    public synchronized DormPage<AccommodationRequestDto> listRequests(Connection c,
                                                                         Long studentId,
                                                                         DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<AccommodationRequestDto> rows = new ArrayList<AccommodationRequestDto>();
        for (AccommodationRequestDto item : state.requests.values()) {
            if ((studentId == null || item.getStudentUserId() == studentId.longValue())
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized AccommodationRequestDto lockRequest(Connection c, long requestId) {
        return state.requests.get(Long.valueOf(requestId));
    }

    @Override
    public synchronized AccommodationRequestDto finishRequest(Connection c, long requestId,
                                                               long reviewerId, boolean approved,
                                                               String remark) {
        AccommodationRequestDto old = state.requests.get(Long.valueOf(requestId));
        if (old == null) throw new DormRepositoryException(DormCommands.REQUEST_NOT_FOUND, "住宿申请不存在");
        if (!"PENDING".equals(old.getStatus())) {
            throw new DormRepositoryException(DormCommands.REQUEST_INVALID_STATE, "申请已处理");
        }
        AccommodationRequestDto value = new AccommodationRequestDto(old.getId(), old.getStudentUserId(),
                old.getRequestType(), old.getCurrentRecordId(), old.getRequestedBedId(), old.getReason(),
                approved ? "APPROVED" : "REJECTED", reviewerId, LocalDateTime.now(), remark,
                old.getCreatedAt());
        state.requests.put(Long.valueOf(value.getId()), value);
        return value;
    }

    private DormBedDto requireAvailableBed(long bedId) {
        DormBedDto bed = state.beds.get(Long.valueOf(bedId));
        if (bed == null) throw new DormRepositoryException(DormCommands.BED_NOT_FOUND, "床位不存在");
        if (!"AVAILABLE".equals(bed.getStatus())) {
            throw new DormRepositoryException(DormCommands.BED_OCCUPIED, "床位已被占用或不可用");
        }
        return bed;
    }

    private void requireStudent(long studentId) {
        if (studentId <= 0 || (!state.activeStudents.isEmpty()
                && !state.activeStudents.contains(Long.valueOf(studentId)))) {
            throw new DormRepositoryException(DormCommands.INVALID_INPUT, "学生账号不存在或已停用");
        }
    }

    private static AccommodationDto record(long id, long studentId, DormBedDto bed,
                                           LocalDate start, LocalDate end, String status) {
        if (bed == null) throw new DormRepositoryException(DormCommands.BED_NOT_FOUND, "床位不存在");
        return new AccommodationDto(id, studentId, bed.getId(), bed.getRoomId(), 0L,
                bed.getBuildingCode(), null, bed.getRoomNo(), bed.getBedNo(), start, end, status);
    }

    private void replaceBed(DormBedDto old, String status, Long occupant) {
        state.beds.put(Long.valueOf(old.getId()), new DormBedDto(old.getId(), old.getRoomId(),
                old.getBuildingCode(), old.getRoomNo(), old.getBedNo(), status, occupant));
    }
}
