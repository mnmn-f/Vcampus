package edu.seu.vcampus.server.store.repository;

import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;

import java.sql.Connection;

/** 商店销售聚合查询边界，与订单生命周期写入职责分离。 */
public interface StoreSalesRepository {
    StoreSalesPage findSales(Connection connection, StoreSalesQuery query);
}
