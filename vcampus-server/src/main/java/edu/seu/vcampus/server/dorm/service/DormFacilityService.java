package edu.seu.vcampus.server.dorm.service;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.dorm.repository.DormRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.security.SessionContext;

/** 楼栋、房间、床位查询。 */
final class DormFacilityService extends DormServiceSupport {
    private final DormRepository repository;

    DormFacilityService(DormRepository repository, TransactionManager transactions) {
        super(transactions);
        this.repository = repository;
    }

    DormPage<DormBuildingDto> buildings(final SessionContext session, final DormPageQuery query) {
        requireAny(session, Permission.DORM_SELF_READ, Permission.DORM_MANAGE);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<DormPage<DormBuildingDto>>() { public DormPage<DormBuildingDto> run(java.sql.Connection c) throws Exception { return repository.listBuildings(c, q); } });
    }

    DormPage<DormRoomDto> rooms(final SessionContext session, final DormPageQuery query) {
        requireAny(session, Permission.DORM_SELF_READ, Permission.DORM_MANAGE);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        return execute(new Work<DormPage<DormRoomDto>>() { public DormPage<DormRoomDto> run(java.sql.Connection c) throws Exception { return repository.listRooms(c, q); } });
    }

    DormPage<DormBedDto> beds(final SessionContext session, final DormPageQuery query) {
        requireAny(session, Permission.DORM_SELF_READ, Permission.DORM_MANAGE);
        final DormPageQuery q = query == null ? DormPageQuery.all() : query;
        page(q.getPage(), q.getPageSize());
        final boolean occupants = session.allows(Permission.DORM_MANAGE);
        return execute(new Work<DormPage<DormBedDto>>() { public DormPage<DormBedDto> run(java.sql.Connection c) throws Exception { return repository.listBeds(c, q, occupants); } });
    }
}
