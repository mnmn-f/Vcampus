package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.BusinessAuditDto;
import edu.seu.vcampus.common.dto.identity.LoginAuditDto;
import edu.seu.vcampus.common.security.Role;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** InMemory 身份聚合共享状态；快照用于模拟数据库回滚。 */
final class InMemoryIdentityState {
    final Map<Long, IdentityUserRecord> users = new LinkedHashMap<Long, IdentityUserRecord>();
    final Map<String, Long> idsByAccount = new LinkedHashMap<String, Long>();
    final Map<Role, IdentityRoleRecord> roles = new EnumMap<Role, IdentityRoleRecord>(Role.class);
    final Map<Long, IdentitySessionRecord> sessions = new LinkedHashMap<Long, IdentitySessionRecord>();
    final Map<String, Long> idsByToken = new LinkedHashMap<String, Long>();
    final Map<Long, AccountCancellationRecord> cancellationRequests =
            new LinkedHashMap<Long, AccountCancellationRecord>();
    final List<LoginAuditDto> loginAudits = new ArrayList<LoginAuditDto>();
    final List<BusinessAuditDto> businessAudits = new ArrayList<BusinessAuditDto>();
    long userSequence = 1L;
    long sessionSequence = 1L;
    long loginAuditSequence = 1L;
    long businessAuditSequence = 1L;
    long cancellationSequence = 1L;

    InMemoryIdentityState() {
        for (Role role : Role.values()) {
            roles.put(role, new IdentityRoleRecord(role.ordinal() + 1L, role,
                    role.getDisplayName(), null, "ACTIVE"));
        }
    }

    Snapshot snapshot() {
        Snapshot result = new Snapshot();
        result.state.users.putAll(users);
        result.state.idsByAccount.putAll(idsByAccount);
        result.state.roles.putAll(roles);
        result.state.sessions.putAll(sessions);
        result.state.idsByToken.putAll(idsByToken);
        result.state.cancellationRequests.putAll(cancellationRequests);
        result.state.loginAudits.addAll(loginAudits);
        result.state.businessAudits.addAll(businessAudits);
        result.state.userSequence = userSequence;
        result.state.sessionSequence = sessionSequence;
        result.state.loginAuditSequence = loginAuditSequence;
        result.state.businessAuditSequence = businessAuditSequence;
        result.state.cancellationSequence = cancellationSequence;
        return result;
    }

    void restore(Snapshot snapshot) {
        users.clear();
        users.putAll(snapshot.state.users);
        idsByAccount.clear();
        idsByAccount.putAll(snapshot.state.idsByAccount);
        roles.clear();
        roles.putAll(snapshot.state.roles);
        sessions.clear();
        sessions.putAll(snapshot.state.sessions);
        idsByToken.clear();
        idsByToken.putAll(snapshot.state.idsByToken);
        cancellationRequests.clear();
        cancellationRequests.putAll(snapshot.state.cancellationRequests);
        loginAudits.clear();
        loginAudits.addAll(snapshot.state.loginAudits);
        businessAudits.clear();
        businessAudits.addAll(snapshot.state.businessAudits);
        userSequence = snapshot.state.userSequence;
        sessionSequence = snapshot.state.sessionSequence;
        loginAuditSequence = snapshot.state.loginAuditSequence;
        businessAuditSequence = snapshot.state.businessAuditSequence;
        cancellationSequence = snapshot.state.cancellationSequence;
    }

    static final class Snapshot {
        final InMemoryIdentityState state = new InMemoryIdentityState();
    }
}
