package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditWriteRequest;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;

/** 登录审计和业务审计读写边界。 */
public interface IdentityAuditRepository {
    void insertLoginAudit(Connection connection, LoginAuditWriteRequest request);
    void insertBusinessAudit(Connection connection, SessionContext actor, String action,
                             String resourceType, Long resourceId, String outcome,
                             String detailJson);
    LoginAuditPage searchLoginAudits(Connection connection, AuditQuery query);
    BusinessAuditPage searchBusinessAudits(Connection connection, AuditQuery query);
}
