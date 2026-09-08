package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;

/** 将身份事务委托给统一数据库 TransactionManager。 */
public final class IdentityTransactionManagerRunner implements IdentityTransactionRunner {
    private final TransactionManager manager;

    public IdentityTransactionManagerRunner(TransactionManager manager) {
        if (manager == null) throw new IllegalArgumentException("transaction manager is required");
        this.manager = manager;
    }

    @Override public <T> T execute(TransactionWork<T> work) throws Exception {
        return manager.execute(work);
    }
}
