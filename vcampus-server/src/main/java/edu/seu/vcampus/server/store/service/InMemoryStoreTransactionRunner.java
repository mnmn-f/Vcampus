package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.store.repository.InMemoryStoreRecordRepository;

/** 内存仓储测试用事务执行器；以仓储共享锁模拟串行化提交边界。 */
public final class InMemoryStoreTransactionRunner implements StoreTransactionRunner {
    private final Object lock;
    private final InMemoryStoreRecordRepository repository;

    public InMemoryStoreTransactionRunner(Object lock) {
        if (lock == null) throw new IllegalArgumentException("lock is required");
        this.lock = lock;
        this.repository = null;
    }

    public InMemoryStoreTransactionRunner(InMemoryStoreRecordRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.lock = repository.transactionLock();
        this.repository = repository;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) throws Exception {
        synchronized (lock) {
            Object snapshot = repository == null ? null : repository.snapshotState();
            try {
                return work.execute(null);
            } catch (RuntimeException ex) {
                if (repository != null) repository.restoreState(snapshot);
                throw ex;
            } catch (Exception ex) {
                if (repository != null) repository.restoreState(snapshot);
                throw ex;
            }
        }
    }
}
