package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.RoleAssignmentRequest;
import edu.seu.vcampus.common.dto.identity.RoleDto;
import edu.seu.vcampus.common.dto.identity.RoleRevokeRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.identity.repository.IdentityRoleRecord;
import edu.seu.vcampus.server.identity.repository.IdentityUserRecord;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.sql.Connection;
import java.util.List;

/** 角色列表、分配与撤销用例；操作后撤销目标会话以避免旧权限继续生效。 */
final class IdentityRoleService {
    private final IdentityRecordRepository repository;
    private final IdentityTransactionRunner transactions;
    private final SessionManager sessionManager;

    IdentityRoleService(IdentityRecordRepository repository, IdentityTransactionRunner transactions,
                        SessionManager sessionManager) {
        this.repository = repository;
        this.transactions = transactions;
        this.sessionManager = sessionManager;
    }

    List<RoleDto> list(SessionContext session) throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.ROLE_MANAGE);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<List<RoleDto>>() {
                    @Override public List<RoleDto> execute(Connection c) { return repository.listRoles(c); }
                });
    }

    ProfileDto assign(final SessionContext session, final RoleAssignmentRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.ROLE_MANAGE);
        validate(request == null ? 0L : request.getUserId(), request == null ? null : request.getRole());
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        IdentityUserRecord target = user(c, request.getUserId());
                        ensureRole(c, request.getRole());
                        if (!repository.assignRole(c, target.getUserId(), request.getRole(), session.getUserId())) {
                            throw IdentityServiceSupport.error(ResultCodes.CONFLICT, "角色已分配或不可用");
                        }
                        repository.revokeUserSessions(c, target.getUserId());
                        sessionManager.invalidateUserSessions(target.getUserId());
                        IdentityServiceSupport.audit(repository, c, session, "ROLE_ASSIGN", "USER",
                                Long.valueOf(target.getUserId()),
                                "{\"role\":\"" + request.getRole().name() + "\"}");
                        return user(c, target.getUserId()).toProfile();
                    }
                });
    }

    ProfileDto revoke(final SessionContext session, final RoleRevokeRequest request)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.ROLE_MANAGE);
        validate(request == null ? 0L : request.getUserId(), request == null ? null : request.getRole());
        if (request.getUserId() == session.getUserId()) {
            throw IdentityServiceSupport.error(ResultCodes.FORBIDDEN, "不能撤销当前会话所属账号角色");
        }
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<ProfileDto>() {
                    @Override public ProfileDto execute(Connection c) throws IdentityServiceException {
                        IdentityUserRecord target = user(c, request.getUserId());
                        if (repository.countRoles(c, target.getUserId()) <= 1) {
                            throw IdentityServiceSupport.error(ResultCodes.CONFLICT, "不能撤销用户最后一个角色");
                        }
                        if (!repository.revokeRole(c, target.getUserId(), request.getRole())) {
                            throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "用户未拥有该角色");
                        }
                        repository.revokeUserSessions(c, target.getUserId());
                        sessionManager.invalidateUserSessions(target.getUserId());
                        IdentityServiceSupport.audit(repository, c, session, "ROLE_REVOKE", "USER",
                                Long.valueOf(target.getUserId()),
                                "{\"role\":\"" + request.getRole().name() + "\"}");
                        return user(c, target.getUserId()).toProfile();
                    }
                });
    }

    private IdentityUserRecord user(Connection c, long id) throws IdentityServiceException {
        IdentityServiceSupport.positive(id, "用户编号");
        IdentityUserRecord found = repository.findById(c, id, true);
        if (found == null) throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "用户不存在");
        return found;
    }

    private void ensureRole(Connection c, Role role) throws IdentityServiceException {
        IdentityRoleRecord found =
                repository.findRole(c, role, true);
        if (found == null || !"ACTIVE".equalsIgnoreCase(found.getStatus())) {
            throw IdentityServiceSupport.error(ResultCodes.NOT_FOUND, "角色不存在或已停用");
        }
    }

    private static void validate(long userId, Role role) throws IdentityServiceException {
        IdentityServiceSupport.positive(userId, "用户编号");
        if (role == null) throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "角色不能为空");
    }
}
