package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.server.db.TransactionWork;

/** 学籍服务使用的事务执行边界，生产实现委托给公共 TransactionManager。 */
public interface StudentTransactionRunner {
    <T> T execute(TransactionWork<T> work) throws Exception;
}
