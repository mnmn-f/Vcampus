package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationView;
import edu.seu.vcampus.common.dto.library.StudyRoomSearchRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomUpsertRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

final class DemoStudyRoomService {
    private final List<StudyRoomView> rooms = DemoLibraryData.rooms();
    private final List<StudyRoomReservationView> reservations = DemoLibraryData.reservations();
    private long nextReservationId = 402L;

    PageResult<StudyRoomView> search(StudyRoomSearchRequest request) {
        StudyRoomSearchRequest query = request == null ? new StudyRoomSearchRequest() : request;
        List<StudyRoomView> result = new ArrayList<StudyRoomView>();
        for (StudyRoomView value : rooms) {
            boolean capacity = query.getMinCapacity() == null
                    || value.getCapacity() >= query.getMinCapacity().intValue();
            if (capacity && DemoLibrarySupport.matches(
                    value.getBuildingName() + " " + value.getRoomNo(), query.getKeyword())
                    && DemoLibrarySupport.same(query.getStatus(), value.getStatus())) {
                result.add(value);
            }
        }
        return DemoLibrarySupport.page(result, query.getPage(), query.getPageSize());
    }

    StudyRoomReservationView reserve(StudyRoomReservationRequest request)
            throws NetworkClientException {
        if (request == null || request.getStartAt() == null || request.getEndAt() == null) {
            throw DemoLibrarySupport.error("预约信息不能为空");
        }
        StudyRoomView room = requireRoom(request.getRoomId());
        if (!"OPEN".equals(room.getStatus())) throw DemoLibrarySupport.error("自习室当前不可预约");
        if (!request.getEndAt().isAfter(request.getStartAt())
                || request.getStartAt().isBefore(LocalDateTime.now())
                || !request.getStartAt().toLocalDate().equals(request.getEndAt().toLocalDate())) {
            throw DemoLibrarySupport.error("预约时间不正确");
        }
        if (request.getStartAt().toLocalTime().isBefore(room.getOpenTime())
                || request.getEndAt().toLocalTime().isAfter(room.getCloseTime())) {
            throw DemoLibrarySupport.error("预约时间不在开放时段内");
        }
        for (StudyRoomReservationView current : reservations) {
            if (!"RESERVED".equals(current.getStatus())) continue;
            if (overlaps(current, request)) {
                throw DemoLibrarySupport.error(current.getRoomId() == room.getId()
                        ? "该自习室时段已被预约" : "你在该时段已有其他预约");
            }
        }
        StudyRoomReservationView value = new StudyRoomReservationView(nextReservationId++,
                room.getId(), room.getBuildingName() + " " + room.getRoomNo(), 1L,
                request.getStartAt(), request.getEndAt(), "RESERVED", null);
        reservations.add(0, value);
        return value;
    }

    StudyRoomReservationView cancel(long id) throws NetworkClientException {
        for (int i = 0; i < reservations.size(); i++) {
            StudyRoomReservationView old = reservations.get(i);
            if (old.getId() != id) continue;
            if (!"RESERVED".equals(old.getStatus())) {
                throw DemoLibrarySupport.error("该预约已经取消");
            }
            StudyRoomReservationView value = new StudyRoomReservationView(old.getId(),
                    old.getRoomId(), old.getRoomName(), old.getUserId(), old.getStartAt(),
                    old.getEndAt(), "CANCELLED", LocalDateTime.now());
            reservations.set(i, value);
            return value;
        }
        throw DemoLibrarySupport.error("未找到预约记录");
    }

    PageResult<StudyRoomReservationView> reservations(
            StudyRoomReservationSearchRequest request) {
        StudyRoomReservationSearchRequest query = request == null
                ? new StudyRoomReservationSearchRequest() : request;
        List<StudyRoomReservationView> result = new ArrayList<StudyRoomReservationView>();
        for (StudyRoomReservationView value : reservations) {
            if (DemoLibrarySupport.same(query.getStatus(), value.getStatus())) result.add(value);
        }
        return DemoLibrarySupport.page(result, query.getPage(), query.getPageSize());
    }

    StudyRoomView save(StudyRoomUpsertRequest request) throws NetworkClientException {
        if (request == null) throw DemoLibrarySupport.error("自习室信息不能为空");
        DemoLibrarySupport.required(request.getBuildingName(), "楼栋不能为空");
        DemoLibrarySupport.required(request.getRoomNo(), "房间号不能为空");
        if (request.getCapacity() <= 0 || request.getOpenTime() == null
                || request.getCloseTime() == null
                || !request.getCloseTime().isAfter(request.getOpenTime())) {
            throw DemoLibrarySupport.error("容量或开放时段不正确");
        }
        long id = request.getId() > 0L ? request.getId() : nextRoomId();
        StudyRoomView value = new StudyRoomView(id, request.getBuildingName(),
                request.getRoomNo(), request.getCapacity(), request.getOpenTime(),
                request.getCloseTime(), DemoLibrarySupport.text(request.getStatus(), "OPEN"),
                request.getDescription());
        replaceRoom(value);
        return value;
    }

    private StudyRoomView requireRoom(long id) throws NetworkClientException {
        for (StudyRoomView value : rooms) if (value.getId() == id) return value;
        throw DemoLibrarySupport.error("未找到自习室");
    }

    private void replaceRoom(StudyRoomView value) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getId() == value.getId()) { rooms.set(i, value); return; }
        }
        rooms.add(value);
    }

    private long nextRoomId() {
        long id = 1L;
        for (StudyRoomView value : rooms) id = Math.max(id, value.getId() + 1L);
        return id;
    }

    private static boolean overlaps(StudyRoomReservationView current,
                                    StudyRoomReservationRequest request) {
        return current.getStartAt().isBefore(request.getEndAt())
                && current.getEndAt().isAfter(request.getStartAt());
    }
}
