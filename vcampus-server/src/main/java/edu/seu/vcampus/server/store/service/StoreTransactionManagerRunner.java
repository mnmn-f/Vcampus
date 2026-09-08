package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;

/** 将公共 TransactionManager 适配到商店服务。 */
public final class StoreTransactionManagerRunner implements StoreTransactionRunner {
    private final TransactionManager manager;

    public StoreTransactionManagerRunner(TransactionManager manager) {
        if (manager == null) throw new IllegalArgumentException("transactionManager is required");
        this.manager = manager;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) throws Exception {
        return manager.execute(work);
    }
}
