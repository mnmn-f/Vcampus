package edu.seu.vcampus.server.store.repository;

/** 商店聚合的持久化边界；业务事务连接由服务层统一提供。 */
public interface StoreRecordRepository extends StoreProductRepository, StoreCartRepository,
        StoreOrderRepository, StoreAccountRepository, StoreSalesRepository {
}
