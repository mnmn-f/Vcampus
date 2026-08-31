package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;

/** 将模块事务适配到统一 TransactionManager。 */
public final class TransactionManagerRunner implements StudentTransactionRunner {
    private final TransactionManager transactionManager;

    public TransactionManagerRunner(TransactionManager transactionManager) {
        if (transactionManager == null) {
            throw new IllegalArgumentException("transactionManager is required");
        }
        this.transactionManager = transactionManager;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) throws Exception {
        return transactionManager.execute(work);
    }
}
