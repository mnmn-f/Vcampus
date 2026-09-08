package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.common.dto.identity.AuditQuery;
import edu.seu.vcampus.common.dto.identity.BusinessAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditPage;
import edu.seu.vcampus.common.dto.identity.LoginAuditWriteRequest;
import edu.seu.vcampus.common.dto.identity.MonitorSnapshotDto;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.IdentityRecordRepository;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.security.SessionManager;

import java.sql.Connection;

/** 审计查询、认证审计写入和最小系统监控用例。 */
final class IdentityAuditService {
    private final IdentityRecordRepository repository;
    private final IdentityTransactionRunner transactions;
    private final SessionManager sessionManager;

    IdentityAuditService(IdentityRecordRepository repository, IdentityTransactionRunner transactions,
                         SessionManager sessionManager) {
        this.repository = repository;
        this.transactions = transactions;
        this.sessionManager = sessionManager;
    }

    LoginAuditPage loginPage(SessionContext session, AuditQuery query)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.SYSTEM_MONITOR);
        final AuditQuery safe = query == null ? new AuditQuery() : query;
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<LoginAuditPage>() {
                    @Override public LoginAuditPage execute(Connection c) {
                        return repository.searchLoginAudits(c, safe);
                    }
                });
    }

    BusinessAuditPage businessPage(SessionContext session, AuditQuery query)
            throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.SYSTEM_MONITOR);
        final AuditQuery safe = query == null ? new AuditQuery() : query;
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<BusinessAuditPage>() {
                    @Override public BusinessAuditPage execute(Connection c) {
                        return repository.searchBusinessAudits(c, safe);
                    }
                });
    }

    void writeLogin(final LoginAuditWriteRequest request) throws IdentityServiceException {
        if (request == null || request.getUsernameSnapshot() == null
                || request.getUsernameSnapshot().trim().isEmpty()) {
            throw IdentityServiceSupport.error(ResultCodes.INVALID_INPUT, "登录审计请求不完整");
        }
        IdentityServiceSupport.inTransaction(transactions, new TransactionWork<Object>() {
            @Override public Object execute(Connection c) {
                repository.insertLoginAudit(c, request);
                return null;
            }
        });
    }

    MonitorSnapshotDto monitor(final SessionContext session) throws IdentityServiceException {
        IdentityServiceSupport.require(session, Permission.SYSTEM_MONITOR);
        return IdentityServiceSupport.inTransaction(transactions,
                new TransactionWork<MonitorSnapshotDto>() {
                    @Override public MonitorSnapshotDto execute(Connection c) {
                        MonitorSnapshotDto database = repository.snapshot(c);
                        return new MonitorSnapshotDto(database.isDatabaseHealthy(),
                                sessionManager.activeSessionCount(), database.getUserCount(),
                                database.getCheckedAt(), database.getMessage());
                    }
                });
    }
}
