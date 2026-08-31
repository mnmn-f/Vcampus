package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.StoreSalesPage;
import edu.seu.vcampus.common.dto.store.StoreSalesQuery;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.sql.Connection;

/** 商店管理员销售统计；筛选条件不携带或信任操作者身份。 */
final class StoreSalesService {
    private final StoreRecordRepository repository;
    private final StoreTransactionRunner transactions;

    StoreSalesService(StoreRecordRepository repository, StoreTransactionRunner transactions) {
        this.repository = repository;
        this.transactions = transactions;
    }

    StoreSalesPage report(final SessionContext session, StoreSalesQuery query)
            throws StoreServiceException {
        StoreServiceSupport.requirePermission(session, Permission.STORE_SALES_READ);
        StoreServiceSupport.requireRole(session, Role.STORE_MANAGER);
        final StoreSalesQuery safe = validate(query == null ? new StoreSalesQuery() : query);
        return StoreServiceSupport.inTransaction(transactions,
                new TransactionWork<StoreSalesPage>() {
                    @Override public StoreSalesPage execute(Connection connection) {
                        return repository.findSales(connection, safe);
                    }
                });
    }

    private static StoreSalesQuery validate(StoreSalesQuery query) throws StoreServiceException {
        if (query.getProductId() != null && query.getProductId().longValue() <= 0L) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "商品编号不正确");
        }
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getStartDate().isAfter(query.getEndDate())) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "日期范围不正确");
        }
        StoreServiceSupport.maxLength(query.getKeyword(), 200, "商品关键字");
        return query;
    }
}
