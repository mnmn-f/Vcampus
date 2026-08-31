package edu.seu.vcampus.server.identity.repository;

/** 身份模块聚合仓储，子接口保持用户、角色、会话、审计和监控职责分离。 */
public interface IdentityRecordRepository extends IdentityUserRepository, IdentityRoleRepository,
        IdentitySessionRepository, IdentityAuditRepository, IdentityMonitorRepository,
        IdentityCancellationRepository {
}
