package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** srtp_records 内存专责仓储。 */
final class InMemoryCampusSrtpRepository implements CampusSrtpRepository {
    private final InMemoryCampusState state;
    InMemoryCampusSrtpRepository(InMemoryCampusState state) { this.state = state; }

    @Override public synchronized CampusPage<SrtpRecordDto> list(Connection c, CampusPageQuery q,
            Long studentId) {
        List<SrtpRecordDto> rows = new ArrayList<SrtpRecordDto>();
        for (SrtpRecordDto value : state.srtp.values()) {
            if (studentId != null && value.getStudentUserId() != studentId.longValue()) continue;
            if (!InMemoryCampusSupport.matches(q, value.getStatus(), value.getProjectCode(), value.getTitle())) continue;
            rows.add(value);
        }
        return InMemoryCampusSupport.page(rows, q);
    }

    @Override public synchronized SrtpRecordDto findSrtp(Connection c, long id) {
        return state.srtp.get(Long.valueOf(id));
    }

    @Override public synchronized SrtpRecordDto lockSrtp(Connection c, long id) {
        return findSrtp(c, id);
    }

    @Override public synchronized SrtpRecordDto save(Connection c, SrtpSaveRequest r,
            long defaultStudent, long actor, boolean admin) {
        long id = r.getId() == null ? state.nextSrtp++ : r.getId().longValue();
        SrtpRecordDto old = state.srtp.get(Long.valueOf(id));
        if (r.getId() != null && old == null) {
            throw new CampusRepositoryException("CAMPUS.SRTP_NOT_FOUND", "SRTP记录不存在");
        }
        long student = admin && r.getStudentUserId() != null ? r.getStudentUserId().longValue()
                : (old == null ? defaultStudent : old.getStudentUserId());
        if (admin && old == null && r.getStudentUserId() == null) {
            throw new CampusRepositoryException("CAMPUS.INVALID_INPUT", "项目所属学生不能为空");
        }
        ensureUniqueCode(r.getProjectCode(), id);
        String status = r.getStatus() == null ? "SUBMITTED" : r.getStatus();
        SrtpRecordDto value = new SrtpRecordDto(id, r.getProjectCode(), student, r.getTitle(),
                r.getDescription(), r.getCredits(), status,
                old == null ? LocalDateTime.now() : old.getSubmittedAt(),
                old == null ? null : old.getReviewedBy(), old == null ? null : old.getReviewedAt(),
                old == null ? null : old.getReviewRemark());
        state.srtp.put(Long.valueOf(id), value);
        return value;
    }

    @Override public synchronized SrtpRecordDto review(Connection c, SrtpStatusRequest r,
            long reviewer) {
        SrtpRecordDto old = findSrtp(c, r.getRecordId());
        if (old == null) throw new CampusRepositoryException("CAMPUS.SRTP_NOT_FOUND", "SRTP记录不存在");
        SrtpRecordDto value = new SrtpRecordDto(old.getId(), old.getProjectCode(),
                old.getStudentUserId(), old.getTitle(), old.getDescription(), old.getCredits(),
                r.getStatus(), old.getSubmittedAt(), reviewer, LocalDateTime.now(), r.getRemark());
        state.srtp.put(Long.valueOf(old.getId()), value);
        return value;
    }

    private void ensureUniqueCode(String code, long id) {
        for (SrtpRecordDto value : state.srtp.values()) {
            if (value.getId() != id && code != null && code.equalsIgnoreCase(value.getProjectCode())) {
                throw new CampusRepositoryException("CAMPUS.INVALID_INPUT", "项目编号已存在");
            }
        }
    }
}
