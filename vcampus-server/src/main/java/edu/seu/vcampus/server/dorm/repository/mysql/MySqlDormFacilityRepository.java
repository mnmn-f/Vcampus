package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.DormRoomDto;
import edu.seu.vcampus.server.dorm.repository.DormFacilityRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** MySQL 楼栋、房间、床位查询。 */
public final class MySqlDormFacilityRepository implements DormFacilityRepository {
    private static final String BED_COLUMNS = "b.id,b.room_id,db.building_code,dr.room_no,b.bed_no,b.status,"
            + "ar.student_user_id AS occupant_user_id";

    @Override
    public DormPage<DormBuildingDto> listBuildings(Connection c, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = buildingWhere(query, p);
        return JdbcDormSupport.page(c, "SELECT COUNT(*) FROM dorm_buildings" + where,
                "SELECT id,building_code,building_name,address,gender_policy,status FROM dorm_buildings"
                        + where + " ORDER BY building_code LIMIT ? OFFSET ?", p, query.getPage(),
                query.getPageSize(), new JdbcDormSupport.Reader<DormBuildingDto>() { public DormBuildingDto read(ResultSet r) throws SQLException { return JdbcDormSupport.building(r); } });
    }

    @Override
    public DormPage<DormRoomDto> listRooms(Connection c, DormPageQuery q) throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = roomWhere(query, p);
        String from = " FROM dorm_rooms dr JOIN dorm_buildings db ON db.id=dr.building_id" + where;
        String select = "SELECT dr.id,dr.building_id,db.building_code,db.building_name,dr.room_no,"
                + "dr.floor_no,dr.capacity,dr.room_type,dr.status,dr.description,"
                + "(SELECT COUNT(*) FROM dorm_beds b WHERE b.room_id=dr.id AND b.status='OCCUPIED') occupied_beds";
        return JdbcDormSupport.page(c, "SELECT COUNT(*)" + from, select + from
                + " ORDER BY db.building_code,dr.room_no LIMIT ? OFFSET ?", p, query.getPage(),
                query.getPageSize(), new JdbcDormSupport.Reader<DormRoomDto>() { public DormRoomDto read(ResultSet r) throws SQLException { return JdbcDormSupport.room(r); } });
    }

    @Override
    public DormPage<DormBedDto> listBeds(Connection c, DormPageQuery q, boolean occupants)
            throws SQLException {
        DormPageQuery query = q == null ? DormPageQuery.all() : q;
        List<Object> p = new ArrayList<Object>();
        String where = bedWhere(query, p);
        String from = " FROM dorm_beds b JOIN dorm_rooms dr ON dr.id=b.room_id"
                + " JOIN dorm_buildings db ON db.id=dr.building_id"
                + " LEFT JOIN accommodation_records ar ON ar.bed_id=b.id AND ar.status='ACTIVE'" + where;
        String select = "SELECT " + BED_COLUMNS;
        DormPage<DormBedDto> page = JdbcDormSupport.page(c, "SELECT COUNT(DISTINCT b.id)" + from,
                select + from + " ORDER BY db.building_code,dr.room_no,b.bed_no LIMIT ? OFFSET ?",
                p, query.getPage(), query.getPageSize(), new JdbcDormSupport.Reader<DormBedDto>() { public DormBedDto read(ResultSet r) throws SQLException { return JdbcDormSupport.bed(r); } });
        if (occupants) return page;
        List<DormBedDto> hidden = new ArrayList<DormBedDto>();
        for (DormBedDto item : page.getItems()) hidden.add(new DormBedDto(item.getId(), item.getRoomId(),
                item.getBuildingCode(), item.getRoomNo(), item.getBedNo(), item.getStatus(), null));
        return new DormPage<DormBedDto>(page.getPageNumber(), page.getPageSize(),
                page.getTotalElements(), hidden);
    }

    @Override
    public DormBedDto findBed(Connection c, long bedId, boolean lock) throws SQLException {
        String sql = "SELECT " + BED_COLUMNS + " FROM dorm_beds b JOIN dorm_rooms dr ON dr.id=b.room_id"
                + " JOIN dorm_buildings db ON db.id=dr.building_id WHERE b.id=?"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, bedId);
            try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcDormSupport.bed(r) : null; }
        }
    }

    @Override
    public boolean roomExists(Connection c, long roomId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT 1 FROM dorm_rooms WHERE id=?")) {
            s.setLong(1, roomId);
            try (ResultSet r = s.executeQuery()) { return r.next(); }
        }
    }

    private static String buildingWhere(DormPageQuery q, List<Object> p) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        addText(w, p, q.getKeyword(), "(building_code LIKE ? OR building_name LIKE ? OR address LIKE ?)", 3);
        addEquals(w, p, q.getStatus(), "status");
        return w.toString();
    }

    private static String roomWhere(DormPageQuery q, List<Object> p) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        addLong(w, p, q.getBuildingId(), "dr.building_id");
        addEquals(w, p, q.getStatus(), "dr.status");
        if (JdbcDormSupport.clean(q.getKeyword()) != null) {
            String value = "%" + q.getKeyword().trim() + "%";
            w.append(" AND (dr.room_no LIKE ? OR db.building_code LIKE ? OR db.building_name LIKE ?)");
            p.add(value); p.add(value); p.add(value);
        }
        return w.toString();
    }

    private static String bedWhere(DormPageQuery q, List<Object> p) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        addLong(w, p, q.getRoomId(), "b.room_id");
        addEquals(w, p, q.getStatus(), "b.status");
        return w.toString();
    }

    private static void addEquals(StringBuilder w, List<Object> p, String value, String column) {
        if (JdbcDormSupport.clean(value) != null) { w.append(" AND ").append(column).append("=?"); p.add(value.trim()); }
    }
    private static void addLong(StringBuilder w, List<Object> p, Long value, String column) {
        if (value != null) { w.append(" AND ").append(column).append("=?"); p.add(value); }
    }
    private static void addText(StringBuilder w, List<Object> p, String value, String clause, int count) {
        if (JdbcDormSupport.clean(value) != null) {
            w.append(" AND ").append(clause);
            String item = "%" + value.trim() + "%";
            for (int i = 0; i < count; i++) p.add(item);
        }
    }
}
