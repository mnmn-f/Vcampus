package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.server.db.TransactionWork;

/** 商店用例的事务执行边界。 */
public interface StoreTransactionRunner {
    <T> T execute(TransactionWork<T> work) throws Exception;
}
