package edu.seu.vcampus.common.protocol.command;

/** 宿舍模块命令与稳定业务结果码的唯一登记处。 */
public final class DormCommands {
    public static final String BUILDING_LIST = "dorm.building.list";
    public static final String ROOM_LIST = "dorm.room.list";
    public static final String BED_LIST = "dorm.bed.list";
    public static final String ACCOMMODATION_MINE = "dorm.accommodation.mine";
    public static final String ACCOMMODATION_ASSIGN = "dorm.accommodation.assign";
    public static final String ACCOMMODATION_TRANSFER = "dorm.accommodation.transfer";
    public static final String ACCOMMODATION_CHECKOUT = "dorm.accommodation.checkout";
    public static final String REQUEST_SUBMIT = "dorm.request.submit";
    public static final String REQUEST_LIST = "dorm.request.list";
    public static final String REQUEST_APPROVE = "dorm.request.approve";
    public static final String ACCESS_RECORD = "dorm.access.record";
    public static final String ACCESS_LIST = "dorm.access.list";
    public static final String ALERT_LIST = "dorm.alert.list";
    public static final String ALERT_HANDLE = "dorm.alert.handle";
    public static final String HYGIENE_LIST = "dorm.hygiene.list";
    public static final String HYGIENE_SAVE = "dorm.hygiene.save";
    public static final String REPAIR_LIST = "dorm.repair.list";
    public static final String REPAIR_CREATE = "dorm.repair.create";
    public static final String REPAIR_UPDATE = "dorm.repair.update";
    public static final String REPAIR_EVALUATE = "dorm.repair.evaluate";
    public static final String LEAVE_SUBMIT = "dorm.leave.submit";
    public static final String LEAVE_MINE = "dorm.leave.mine";
    public static final String LEAVE_CANCEL = "dorm.leave.cancel";
    public static final String LEAVE_LIST = "dorm.leave.list";
    public static final String LEAVE_REVIEW = "dorm.leave.review";
    public static final String LEAVE_APPROVE = "dorm.leave.approve";
    public static final String LEAVE_REJECT = "dorm.leave.reject";
    public static final String BUILDING_CREATE = "dorm.building.create";
    public static final String BUILDING_UPDATE = "dorm.building.update";
    public static final String ROOM_CREATE = "dorm.room.create";
    public static final String ROOM_UPDATE = "dorm.room.update";
    public static final String BED_CREATE = "dorm.bed.create";
    public static final String BED_UPDATE = "dorm.bed.update";
    public static final String UTILITY_MINE = "dorm.utility.mine";
    /** 宿管全量水电分摊查询；与本人语义的 UTILITY_MINE 分开授权。 */
    public static final String UTILITY_MANAGER_LIST = "dorm.utility.manager.list";
    public static final String UTILITY_LIST = UTILITY_MANAGER_LIST;
    public static final String UTILITY_PAY = "dorm.utility.pay";
    public static final String ANNOUNCEMENT_LIST = "dorm.announcement.list";
    public static final String ANNOUNCEMENT_SAVE = "dorm.announcement.save";

    public static final String DORM_BUILDING_LIST = BUILDING_LIST;
    public static final String DORM_ROOM_LIST = ROOM_LIST;
    public static final String DORM_BED_LIST = BED_LIST;
    public static final String MY_ACCOMMODATION = ACCOMMODATION_MINE;
    public static final String ACCOMMODATION_REQUEST = REQUEST_SUBMIT;
    public static final String ACCOMMODATION_APPROVE = REQUEST_APPROVE;
    public static final String REPAIR_STATUS = REPAIR_UPDATE;
    public static final String BILL_LIST = UTILITY_MINE;
    public static final String BILL_PAY = UTILITY_PAY;

    public static final String BED_NOT_FOUND = "DORM.BED_NOT_FOUND";
    public static final String BED_OCCUPIED = "DORM.BED_OCCUPIED";
    public static final String BED_UNAVAILABLE = "DORM.BED_UNAVAILABLE";
    public static final String ACCOMMODATION_NOT_FOUND = "DORM.ACCOMMODATION_NOT_FOUND";
    public static final String ALREADY_ACCOMMODATED = "DORM.ALREADY_ACCOMMODATED";
    public static final String REQUEST_NOT_FOUND = "DORM.REQUEST_NOT_FOUND";
    public static final String REQUEST_DUPLICATE = "DORM.REQUEST_DUPLICATE";
    public static final String REQUEST_INVALID_STATE = "DORM.REQUEST_INVALID_STATE";
    public static final String APPROVAL_FORBIDDEN = "DORM.APPROVAL_FORBIDDEN";
    public static final String ALERT_NOT_FOUND = "DORM.ALERT_NOT_FOUND";
    public static final String ALERT_INVALID_STATE = "DORM.ALERT_INVALID_STATE";
    public static final String REPAIR_NOT_FOUND = "DORM.REPAIR_NOT_FOUND";
    public static final String REPAIR_INVALID_STATE = "DORM.REPAIR_INVALID_STATE";
    public static final String LEAVE_NOT_FOUND = "DORM.LEAVE_NOT_FOUND";
    public static final String LEAVE_INVALID_STATE = "DORM.LEAVE_INVALID_STATE";
    public static final String LEAVE_OVERLAP = "DORM.LEAVE_OVERLAP";
    public static final String SPACE_NOT_FOUND = "DORM.SPACE_NOT_FOUND";
    public static final String SPACE_DUPLICATE = "DORM.SPACE_DUPLICATE";
    public static final String SPACE_OCCUPIED = "DORM.SPACE_OCCUPIED";
    public static final String BILL_NOT_FOUND = "DORM.BILL_NOT_FOUND";
    public static final String BILL_ALREADY_PAID = "DORM.BILL_ALREADY_PAID";
    public static final String BILL_NOT_PAYABLE = "DORM.BILL_NOT_PAYABLE";
    public static final String PAYMENT_DUPLICATE = "DORM.PAYMENT_DUPLICATE";
    public static final String INVALID_INPUT = "DORM.INVALID_INPUT";
    public static final String NOT_FOUND = "DORM.NOT_FOUND";
    public static final String INTERNAL_ERROR = "DORM.INTERNAL_ERROR";

    private DormCommands() { }
}
