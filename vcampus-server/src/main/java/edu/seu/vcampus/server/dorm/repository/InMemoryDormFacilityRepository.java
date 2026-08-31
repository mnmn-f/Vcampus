package edu.seu.vcampus.server.dorm.repository;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** 内存楼栋、房间、床位查询。 */
final class InMemoryDormFacilityRepository implements DormFacilityRepository {
    private final InMemoryDormState state;

    InMemoryDormFacilityRepository(InMemoryDormState state) { this.state = state; }

    @Override
    public synchronized DormPage<DormBuildingDto> listBuildings(Connection c, DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<DormBuildingDto> rows = new ArrayList<DormBuildingDto>();
        for (DormBuildingDto item : state.buildings.values()) {
            if (InMemoryDormSupport.status(query.getStatus(), item.getStatus())
                    && InMemoryDormSupport.matches(query.getKeyword(), item.getBuildingCode(),
                    item.getBuildingName(), item.getAddress())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized DormPage<DormRoomDto> listRooms(Connection c, DormPageQuery q) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<DormRoomDto> rows = new ArrayList<DormRoomDto>();
        for (DormRoomDto item : state.rooms.values()) {
            if ((query.getBuildingId() == null || query.getBuildingId() == item.getBuildingId())
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())
                    && InMemoryDormSupport.matches(query.getKeyword(), item.getRoomNo(),
                    item.getBuildingCode(), item.getBuildingName())) rows.add(item);
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized DormPage<DormBedDto> listBeds(Connection c, DormPageQuery q,
                                                       boolean includeOccupant) {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<DormBedDto> rows = new ArrayList<DormBedDto>();
        for (DormBedDto item : state.beds.values()) {
            if ((query.getRoomId() == null || query.getRoomId() == item.getRoomId())
                    && InMemoryDormSupport.status(query.getStatus(), item.getStatus())
                    && InMemoryDormSupport.matches(query.getKeyword(), item.getBedNo(),
                    item.getRoomNo(), item.getBuildingCode())) {
                rows.add(includeOccupant ? item : withoutOccupant(item));
            }
        }
        return InMemoryDormSupport.page(rows, query);
    }

    @Override
    public synchronized DormBedDto findBed(Connection c, long bedId, boolean forUpdate) {
        return state.beds.get(Long.valueOf(bedId));
    }

    @Override
    public synchronized boolean roomExists(Connection c, long roomId) {
        return state.rooms.containsKey(Long.valueOf(roomId));
    }

    private static DormBedDto withoutOccupant(DormBedDto item) {
        return new DormBedDto(item.getId(), item.getRoomId(), item.getBuildingCode(),
                item.getRoomNo(), item.getBedNo(), item.getStatus(), null);
    }
}
