package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditDto;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditDto;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditWriteRequest;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 登录和业务审计内存仓储。 */
final class InMemoryIdentityAuditRepository implements IdentityAuditRepository {
    private final InMemoryIdentityState state;

    InMemoryIdentityAuditRepository(InMemoryIdentityState state) { this.state = state; }

    @Override public void insertLoginAudit(Connection c, LoginAuditWriteRequest request) {
        synchronized (state) {
            state.loginAudits.add(new LoginAuditDto(state.loginAuditSequence++, request.getUserId(),
                    request.getUsernameSnapshot(), request.getRole(), request.getResultCode(),
                    request.getClientIp(), LocalDateTime.now()));
        }
    }

    @Override public void insertBusinessAudit(Connection c, SessionContext actor, String action,
                                              String resourceType, Long resourceId, String outcome,
                                              String detailJson) {
        synchronized (state) {
            state.businessAudits.add(new BusinessAuditDto(state.businessAuditSequence++,
                    actor == null ? null : Long.valueOf(actor.getUserId()),
                    actor == null ? null : actor.getActiveRole(), action, resourceType, resourceId,
                    outcome, detailJson, LocalDateTime.now()));
        }
    }

    @Override public LoginAuditPage searchLoginAudits(Connection c, AuditQuery query) {
        AuditQuery q = query == null ? new AuditQuery() : query;
        synchronized (state) {
            List<LoginAuditDto> found = new ArrayList<LoginAuditDto>();
            for (LoginAuditDto value : state.loginAudits) if (match(value, q)) found.add(value);
            Collections.sort(found, new Comparator<LoginAuditDto>() {
                @Override public int compare(LoginAuditDto a, LoginAuditDto b) {
                    return safe(b.getOccurredAt()).compareTo(safe(a.getOccurredAt()));
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new LoginAuditPage(new ArrayList<LoginAuditDto>(found.subList(from, to)),
                    q.getPage(), q.getPageSize(), found.size());
        }
    }

    @Override public BusinessAuditPage searchBusinessAudits(Connection c, AuditQuery query) {
        AuditQuery q = query == null ? new AuditQuery() : query;
        synchronized (state) {
            List<BusinessAuditDto> found = new ArrayList<BusinessAuditDto>();
            for (BusinessAuditDto value : state.businessAudits) if (match(value, q)) found.add(value);
            Collections.sort(found, new Comparator<BusinessAuditDto>() {
                @Override public int compare(BusinessAuditDto a, BusinessAuditDto b) {
                    return safe(b.getOccurredAt()).compareTo(safe(a.getOccurredAt()));
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new BusinessAuditPage(new ArrayList<BusinessAuditDto>(found.subList(from, to)),
                    q.getPage(), q.getPageSize(), found.size());
        }
    }

    void addLogin(LoginAuditDto value) {
        synchronized (state) {
            state.loginAudits.add(value);
            state.loginAuditSequence = Math.max(state.loginAuditSequence, value.getId() + 1L);
        }
    }

    void addBusiness(BusinessAuditDto value) {
        synchronized (state) {
            state.businessAudits.add(value);
            state.businessAuditSequence = Math.max(state.businessAuditSequence, value.getId() + 1L);
        }
    }

    private static boolean match(LoginAuditDto value, AuditQuery q) {
        return (q.getUserId() == null || q.getUserId().equals(value.getUserId()))
                && (q.getOutcome() == null || q.getOutcome().equalsIgnoreCase(value.getResultCode()));
    }

    private static boolean match(BusinessAuditDto value, AuditQuery q) {
        return (q.getUserId() == null || q.getUserId().equals(value.getActorUserId()))
                && (q.getAction() == null || q.getAction().equalsIgnoreCase(value.getAction()))
                && (q.getOutcome() == null || q.getOutcome().equalsIgnoreCase(value.getOutcome()));
    }

    private static LocalDateTime safe(LocalDateTime value) {
        return value == null ? LocalDateTime.MIN : value;
    }
}
