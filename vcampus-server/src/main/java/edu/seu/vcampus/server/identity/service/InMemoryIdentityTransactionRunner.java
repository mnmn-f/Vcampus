package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.identity.repository.InMemoryIdentityRecordRepository;

/** 内存测试事务：共享锁并在异常时恢复聚合仓储快照。 */
public final class InMemoryIdentityTransactionRunner implements IdentityTransactionRunner {
    private final Object lock;
    private final InMemoryIdentityRecordRepository repository;

    public InMemoryIdentityTransactionRunner(InMemoryIdentityRecordRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
        this.lock = repository.transactionLock();
    }

    public InMemoryIdentityTransactionRunner(Object lock) {
        if (lock == null) throw new IllegalArgumentException("lock is required");
        this.lock = lock;
        this.repository = null;
    }

    @Override public <T> T execute(TransactionWork<T> work) throws Exception {
        synchronized (lock) {
            Object snapshot = repository == null ? null : repository.snapshotState();
            try {
                return work.execute(null);
            } catch (Exception ex) {
                restore(snapshot);
                throw ex;
            } catch (Error ex) {
                restore(snapshot);
                throw ex;
            }
        }
    }

    private void restore(Object snapshot) {
        if (repository != null) repository.restoreState(snapshot);
    }
}
