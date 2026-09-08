package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.*;
import edu.seu.vcampus.common.dto.dorm.ext.*;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

/** In-memory meter and utility-bill persistence. */
final class InMemoryDormMeterStore {
    private final InMemoryDormExtState state;
    InMemoryDormMeterStore(InMemoryDormExtState state) { this.state = state; }

    void addRoom(long id, String building, String roomNo) {
        state.rooms.put(Long.valueOf(id), building + "/" + roomNo);
    }

    void setResidents(long roomId, Long... ids) {
        List<Long> value = new ArrayList<Long>();
        Collections.addAll(value, ids);
        Collections.sort(value);
        state.residents.put(Long.valueOf(roomId), value);
    }

    void attachBill(long readingId, long billId) {
        MeterReadingDto old = state.readings.get(Long.valueOf(readingId));
        if (old != null) state.readings.put(Long.valueOf(readingId), withBill(old, Long.valueOf(billId)));
    }

    List<BigDecimal> allocationsOf(long billId) {
        List<BigDecimal> value = state.allocations.get(Long.valueOf(billId));
        return value == null ? Collections.<BigDecimal>emptyList() : Collections.unmodifiableList(value);
    }

    int billCount() { return state.allocations.size(); }

    DormPage<MeterReadingDto> list(DormPageQuery query) {
        DormPageQuery q = query == null ? DormPageQuery.all() : query;
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        for (MeterReadingDto item : state.readings.values()) {
            boolean room = q.getRoomId() == null || q.getRoomId().longValue() == item.getRoomId();
            if (room && InMemoryDormSupport.matches(q.getKeyword(), item.getRoomNo(), item.getBuildingCode())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, q);
    }

    MeterReadingDto save(MeterReadingRequest request, long actor) {
        String location = state.rooms.get(Long.valueOf(request.getRoomId()));
        if (location == null) throw new DormRepositoryException(DormExtCommands.ROOM_NOT_FOUND, "房间不存在");
        MeterReadingDto old = find(request.getRoomId(), request.getPeriodStart(), request.getPeriodEnd());
        if (old != null && old.isLocked()) throw new DormRepositoryException(DormExtCommands.METER_LOCKED, "该账期已生成账单，读数不可修改");
        long id = old == null ? state.nextReadingId++ : old.getId();
        String[] parts = location.split("/", 2);
        MeterReadingDto value = new MeterReadingDto(id, request.getRoomId(), parts[0], parts[1],
                request.getPeriodStart(), request.getPeriodEnd(), request.getElectricityUnits(), request.getWaterUnits(),
                request.getElectricityPrice(), request.getWaterPrice(), actor, LocalDateTime.now(), null);
        state.readings.put(Long.valueOf(id), value);
        return value;
    }

    List<MeterReadingDto> pending(Long roomId, LocalDate start, LocalDate end) {
        List<MeterReadingDto> rows = new ArrayList<MeterReadingDto>();
        for (MeterReadingDto item : state.readings.values()) {
            boolean room = roomId == null || roomId.longValue() == item.getRoomId();
            if (room && !item.isLocked() && item.getPeriodStart().equals(start) && item.getPeriodEnd().equals(end)) rows.add(item);
        }
        return rows;
    }

    List<Long> residents(long roomId) {
        List<Long> value = state.residents.get(Long.valueOf(roomId));
        return value == null ? Collections.<Long>emptyList() : new ArrayList<Long>(value);
    }

    boolean billExists(long roomId, LocalDate start, LocalDate end) {
        return state.billedPeriods.contains(InMemoryDormSupport.periodKey(roomId, start, end));
    }

    long createBill(MeterReadingDto reading) {
        long id = state.nextBillId++;
        state.billedPeriods.add(InMemoryDormSupport.periodKey(reading.getRoomId(), reading.getPeriodStart(), reading.getPeriodEnd()));
        state.allocations.put(Long.valueOf(id), new ArrayList<BigDecimal>());
        return id;
    }

    void allocation(long billId, BigDecimal amount) {
        List<BigDecimal> values = state.allocations.get(Long.valueOf(billId));
        if (values == null) throw new DormRepositoryException(DormExtCommands.INTERNAL_ERROR, "账单不存在");
        values.add(amount);
    }

    private MeterReadingDto find(long roomId, LocalDate start, LocalDate end) {
        for (MeterReadingDto item : state.readings.values()) {
            if (item.getRoomId() == roomId && item.getPeriodStart().equals(start) && item.getPeriodEnd().equals(end)) return item;
        }
        return null;
    }

    private static MeterReadingDto withBill(MeterReadingDto source, Long billId) {
        return new MeterReadingDto(source.getId(), source.getRoomId(), source.getBuildingCode(), source.getRoomNo(),
                source.getPeriodStart(), source.getPeriodEnd(), source.getElectricityUnits(), source.getWaterUnits(),
                source.getElectricityPrice(), source.getWaterPrice(), source.getRecordedBy(), source.getRecordedAt(), billId);
    }
}
