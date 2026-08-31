package edu.seu.vcampus.server.campus.repository.mysql;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.server.campus.repository.CampusCompetitionRepository;
import edu.seu.vcampus.server.campus.repository.CampusRepositoryException;
import edu.seu.vcampus.server.db.JdbcTemporal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** competitions 与 competition_registrations 的 MySQL 专责仓储。 */
public final class MySqlCampusCompetitionRepository implements CampusCompetitionRepository {
    private static final String COLUMNS = "c.id,c.title,c.description,c.organizer_id,c.start_at,c.end_at,c.registration_deadline,c.capacity,c.status,(SELECT COUNT(*) FROM competition_registrations cr WHERE cr.competition_id=c.id AND cr.status='REGISTERED') AS registered_count";
    private static final String FROM = " FROM competitions c";
    private static final String REG_COLUMNS = "competition_id,student_user_id,status,registered_at,cancelled_at";

    @Override public CampusPage<CompetitionDto> list(Connection c, CampusPageQuery q, boolean drafts)
            throws SQLException {
        CampusPageQuery query = q == null ? CampusPageQuery.all() : q;
        List<Object> values = new ArrayList<Object>();
        String where = " WHERE 1=1";
        if (!drafts) where += " AND c.status='PUBLISHED'";
        if (query.getStatus() != null) { where += " AND c.status=?"; values.add(query.getStatus()); }
        if (query.getKeyword() != null) { where += " AND (c.title LIKE ? OR c.description LIKE ?)"; values.add("%" + query.getKeyword() + "%"); values.add("%" + query.getKeyword() + "%"); }
        return JdbcCampusSupport.page(c, "SELECT COUNT(*)" + FROM + where,
                "SELECT " + COLUMNS + FROM + where + " ORDER BY c.start_at ASC,c.id DESC LIMIT ? OFFSET ?",
                values, query, new JdbcCampusSupport.Mapper<CompetitionDto>() { public CompetitionDto map(ResultSet r) throws SQLException { return JdbcCampusSupport.competition(r); } });
    }

    @Override public CompetitionDto findCompetition(Connection c, long id) throws SQLException {
        return find(c, id, false);
    }
    @Override public CompetitionDto lockCompetition(Connection c, long id) throws SQLException {
        return find(c, id, true);
    }

    @Override public CompetitionDto save(Connection c, CompetitionSaveRequest r, long organizer)
            throws SQLException {
        long id = r.getId() == null ? insert(c, r, organizer) : update(c, r);
        CompetitionDto value = findCompetition(c, id);
        if (value == null) throw new CampusRepositoryException(CampusCommands.COMPETITION_NOT_FOUND, "比赛不存在");
        return value;
    }

    @Override public CompetitionRegistrationDto findRegistration(Connection c, long competition,
            long student, boolean lock) throws SQLException {
        String sql = "SELECT " + REG_COLUMNS + " FROM competition_registrations WHERE competition_id=? AND student_user_id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, competition); s.setLong(2, student); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.registration(r) : null; } }
    }

    @Override public long countRegistered(Connection c, long competition) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM competition_registrations WHERE competition_id=? AND status='REGISTERED'")) { s.setLong(1, competition); try (ResultSet r = s.executeQuery()) { return r.next() ? r.getLong(1) : 0L; } }
    }

    @Override public CompetitionRegistrationDto register(Connection c, long competition, long student)
            throws SQLException {
        String sql = "INSERT INTO competition_registrations(competition_id,student_user_id,status,registered_at,cancelled_at) VALUES(?,?, 'REGISTERED',CURRENT_TIMESTAMP(3),NULL) ON DUPLICATE KEY UPDATE status='REGISTERED',registered_at=CURRENT_TIMESTAMP(3),cancelled_at=NULL";
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, competition); s.setLong(2, student); s.executeUpdate(); }
        return findRegistration(c, competition, student, false);
    }

    @Override public void cancelRegistration(Connection c, long competition, long student) throws SQLException {
        try (PreparedStatement s = c.prepareStatement("UPDATE competition_registrations SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP(3) WHERE competition_id=? AND student_user_id=? AND status='REGISTERED'")) {
            s.setLong(1, competition); s.setLong(2, student);
            if (s.executeUpdate() != 1) throw new CampusRepositoryException(CampusCommands.COMPETITION_INVALID_STATE, "报名不在可取消状态");
        }
    }

    @Override public CampusPage<CompetitionRegistrationDto> roster(Connection c, long competition,
            CampusPageQuery q) throws SQLException {
        CampusPageQuery query = q == null ? CampusPageQuery.all() : q;
        List<Object> values = new ArrayList<Object>(); values.add(competition);
        String where = " WHERE competition_id=?";
        if (query.getStatus() != null) { where += " AND status=?"; values.add(query.getStatus()); }
        return JdbcCampusSupport.page(c, "SELECT COUNT(*) FROM competition_registrations" + where,
                "SELECT " + REG_COLUMNS + " FROM competition_registrations" + where
                        + " ORDER BY registered_at ASC LIMIT ? OFFSET ?", values, query,
                new JdbcCampusSupport.Mapper<CompetitionRegistrationDto>() { public CompetitionRegistrationDto map(ResultSet r) throws SQLException { return JdbcCampusSupport.registration(r); } });
    }

    private long insert(Connection c, CompetitionSaveRequest r, long organizer) throws SQLException {
        String sql = "INSERT INTO competitions(title,description,organizer_id,start_at,end_at,registration_deadline,capacity,status) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(s, r, organizer, 1); s.executeUpdate();
            try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("competition id missing"); return keys.getLong(1); }
        }
    }

    private long update(Connection c, CompetitionSaveRequest r) throws SQLException {
        String sql = "UPDATE competitions SET title=?,description=?,start_at=?,end_at=?,registration_deadline=?,capacity=?,status=? WHERE id=?";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, r.getTitle()); s.setString(2, r.getDescription()); s.setTimestamp(3, JdbcTemporal.timestamp(r.getStartAt())); s.setTimestamp(4, JdbcTemporal.timestamp(r.getEndAt())); s.setTimestamp(5, JdbcTemporal.timestamp(r.getRegistrationDeadline()));
            if (r.getCapacity() == null) s.setNull(6, java.sql.Types.INTEGER); else s.setInt(6, r.getCapacity().intValue());
            s.setString(7, r.getStatus() == null ? "DRAFT" : r.getStatus()); s.setLong(8, r.getId().longValue());
            if (s.executeUpdate() != 1) throw new CampusRepositoryException(CampusCommands.COMPETITION_NOT_FOUND, "比赛不存在");
            return r.getId().longValue();
        }
    }

    private CompetitionDto find(Connection c, long id, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + FROM + " WHERE c.id=?" + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement s = c.prepareStatement(sql)) { s.setLong(1, id); try (ResultSet r = s.executeQuery()) { return r.next() ? JdbcCampusSupport.competition(r) : null; } }
    }

    private static int bind(PreparedStatement s, CompetitionSaveRequest r, long organizer, int i) throws SQLException {
        s.setString(i++, r.getTitle()); s.setString(i++, r.getDescription()); s.setLong(i++, organizer);
        s.setTimestamp(i++, JdbcTemporal.timestamp(r.getStartAt())); s.setTimestamp(i++, JdbcTemporal.timestamp(r.getEndAt())); s.setTimestamp(i++, JdbcTemporal.timestamp(r.getRegistrationDeadline()));
        if (r.getCapacity() == null) s.setNull(i++, java.sql.Types.INTEGER); else s.setInt(i++, r.getCapacity().intValue());
        s.setString(i, r.getStatus() == null ? "DRAFT" : r.getStatus()); return i;
    }
}
