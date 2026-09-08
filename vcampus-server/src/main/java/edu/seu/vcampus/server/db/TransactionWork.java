package edu.seu.vcampus.server.db;

import java.sql.Connection;

/** 在同一个数据库连接和事务中执行的业务工作。 */
public interface TransactionWork<T> {
    T execute(Connection connection) throws Exception;
}

