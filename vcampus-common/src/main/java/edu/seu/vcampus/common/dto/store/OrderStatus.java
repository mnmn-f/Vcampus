package edu.seu.vcampus.common.dto.store;

/** store_orders.status 的状态机取值。 */
public enum OrderStatus {
    CREATED,
    PAID,
    CANCELLED,
    REFUNDED,
    COMPLETED
}
