package edu.seu.vcampus.server.store.registry;

import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.store.handler.StoreCommandHandler;
import edu.seu.vcampus.server.store.repository.mysql.MySqlStoreRecordRepository;
import edu.seu.vcampus.server.store.service.StoreService;

/** 商店模块唯一命令登记入口；中央 ServerMain 可在组装路由时调用。 */
public final class StoreCommandRegistry {
    private StoreCommandRegistry() { }

    public static StoreService createMySqlService(TransactionManager transactions) {
        if (transactions == null) throw new IllegalArgumentException("transactions is required");
        return new StoreService(new MySqlStoreRecordRepository(), transactions);
    }

    public static CommandRouter registerAll(CommandRouter router, StoreService service) {
        if (router == null || service == null) throw new IllegalArgumentException("store dependencies required");
        register(router, StoreCommands.PRODUCT_SEARCH, service);
        register(router, StoreCommands.PRODUCT_DETAIL, service);
        register(router, StoreCommands.PRODUCT_IMAGE, service);
        register(router, StoreCommands.PRODUCT_SAVE, service);
        register(router, StoreCommands.PRODUCT_CREATE, service);
        register(router, StoreCommands.PRODUCT_UPDATE, service);
        register(router, StoreCommands.PRODUCT_STOCK_ADJUST, service);
        register(router, StoreCommands.CATEGORY_LIST, service);
        register(router, StoreCommands.CATEGORY_SAVE, service);
        register(router, StoreCommands.CART_GET, service);
        register(router, StoreCommands.CART_ADD_ITEM, service);
        register(router, StoreCommands.CART_UPDATE_ITEM, service);
        register(router, StoreCommands.CART_REMOVE_ITEM, service);
        register(router, StoreCommands.ORDER_CREATE, service);
        register(router, StoreCommands.ORDER_PAY, service);
        register(router, StoreCommands.CHECKOUT_PREVIEW, service);
        register(router, StoreCommands.CHECKOUT_CONFIRM, service);
        register(router, StoreCommands.ORDER_MINE, service);
        register(router, StoreCommands.ORDER_DETAIL, service);
        register(router, StoreCommands.ORDER_MANAGER_SEARCH, service);
        register(router, StoreCommands.ORDER_STATUS_UPDATE, service);
        register(router, StoreCommands.ORDER_SHIPPING_UPDATE, service);
        register(router, StoreCommands.SALES_REPORT, service);
        register(router, StoreCommands.SALES_TREND, service);
        register(router, StoreCommands.PROMOTION_LIST, service);
        register(router, StoreCommands.PROMOTION_SAVE, service);
        register(router, StoreCommands.COUPON_CLAIM, service);
        register(router, StoreCommands.COUPON_MINE, service);
        register(router, StoreCommands.REVIEW_CREATE, service);
        register(router, StoreCommands.REVIEW_LIST, service);
        register(router, StoreCommands.REVIEW_CANDIDATES, service);
        register(router, StoreCommands.FRIEND_PAY_CREATE, service);
        register(router, StoreCommands.FRIEND_PAY_MINE, service);
        register(router, StoreCommands.FRIEND_PAY_WITHDRAW, service);
        register(router, StoreCommands.FRIEND_PAY_DECIDE, service);
        register(router, StoreCommands.ACCOUNT_GET, service);
        register(router, StoreCommands.ACCOUNT_LEDGER, service);
        register(router, StoreCommands.ACCOUNT_RECHARGE, service);
        return router;
    }

    public static CommandRouter register(CommandRouter router, StoreService service) {
        return registerAll(router, service);
    }

    private static void register(CommandRouter router, String command,
                                 StoreService service) {
        router.register(command, new StoreCommandHandler(command, service));
    }
}
