package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.server.db.TransactionWork;

/** 为未显式提供事务执行器的内存仓储调用补充用例级串行锁。 */
final class SynchronizedStoreTransactionRunner implements StoreTransactionRunner {
    private final StoreTransactionRunner delegate;
    private final Object lock;

    SynchronizedStoreTransactionRunner(StoreTransactionRunner delegate, Object lock) {
        this.delegate = delegate;
        this.lock = lock;
    }

    @Override
    public <T> T execute(TransactionWork<T> work) throws Exception {
        synchronized (lock) { return delegate.execute(work); }
    }
}
