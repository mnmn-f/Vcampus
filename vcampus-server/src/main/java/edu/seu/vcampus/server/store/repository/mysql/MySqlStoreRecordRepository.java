package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.store.repository.DelegatingStoreRecordRepository;
import edu.seu.vcampus.server.store.repository.StoreAccountRepository;
import edu.seu.vcampus.server.store.repository.StoreCartRepository;
import edu.seu.vcampus.server.store.repository.StoreOrderRepository;
import edu.seu.vcampus.server.store.repository.StoreProductRepository;
import edu.seu.vcampus.server.store.repository.StoreSalesRepository;

/** 商店 MySQL DAO 总入口；所有子 DAO 共享服务层的事务连接。 */
public final class MySqlStoreRecordRepository extends DelegatingStoreRecordRepository {
    public MySqlStoreRecordRepository() {
        this(new MySqlStoreProductRepository(), new MySqlStoreCartRepository(),
                new MySqlStoreOrderRepository(), new MySqlStoreAccountRepository());
    }

    /** 兼容按连接工厂接线；连接生命周期仍由 TransactionManager 控制。 */
    public MySqlStoreRecordRepository(JdbcConnectionFactory ignored) {
        this();
    }

    public MySqlStoreRecordRepository(StoreProductRepository products,
                                      StoreCartRepository cart,
                                      StoreOrderRepository orders,
                                      StoreAccountRepository accounts) {
        this(products, cart, orders, accounts, new MySqlStoreSalesRepository());
    }

    public MySqlStoreRecordRepository(StoreProductRepository products,
                                      StoreCartRepository cart,
                                      StoreOrderRepository orders,
                                      StoreAccountRepository accounts,
                                      StoreSalesRepository sales) {
        super(products, cart, orders, accounts, sales);
    }
}
