package edu.seu.vcampus.common.protocol.command;

/** 校园商店与账户命令字唯一登记处。 */
public final class StoreCommands {
    public static final String PRODUCT_SEARCH = "store.product.search";
    public static final String PRODUCT_DETAIL = "store.product.detail";
    public static final String PRODUCT_SAVE = "store.product.save";
    public static final String PRODUCT_CREATE = "store.product.create";
    public static final String PRODUCT_UPDATE = "store.product.update";
    public static final String PRODUCT_STOCK_ADJUST = "store.product.stock-adjust";

    public static final String CART_GET = "store.cart.get";
    public static final String CART_ADD_ITEM = "store.cart.add-item";
    public static final String CART_UPDATE_ITEM = "store.cart.update-item";
    public static final String CART_REMOVE_ITEM = "store.cart.remove-item";

    public static final String ORDER_CREATE = "store.order.create";
    public static final String ORDER_PAY = "store.order.pay";
    public static final String ORDER_MINE = "store.order.mine";
    public static final String ORDER_DETAIL = "store.order.detail";
    public static final String ORDER_MANAGER_SEARCH = "store.order.manager-search";
    public static final String ORDER_STATUS_UPDATE = "store.order.status-update";
    public static final String SALES_REPORT = "store.sales.report";

    public static final String ACCOUNT_GET = "store.account.get";
    public static final String ACCOUNT_LEDGER = "store.account.ledger";
    public static final String ACCOUNT_RECHARGE = "store.account.recharge";

    private StoreCommands() {
    }
}
