package edu.seu.vcampus.server.identity.service;

import edu.seu.vcampus.server.db.TransactionWork;

/** 身份用例共享的事务执行边界。 */
public interface IdentityTransactionRunner {
    <T> T execute(TransactionWork<T> work) throws Exception;
}
