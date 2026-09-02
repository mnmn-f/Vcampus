package edu.seu.vcampus.common.protocol.command;

/**
 * 宿舍扩展模块命令与稳定业务结果码的唯一登记处。
 *
 * <p>与 {@link DormCommands} 分开登记：扩展命令一律使用 {@code dorm.ext.} 前缀，
 * 因此不会与既有 38 个宿舍命令重号，也不需要改动原有登记类。</p>
 */
public final class DormExtCommands {
    /** 抄表读数分页查询（宿管）。 */
    public static final String METER_LIST = "dorm.ext.meter.list";
    /** 抄表读数录入或更新（宿管）；账单生成的唯一数据入口。 */
    public static final String METER_SUBMIT = "dorm.ext.meter.submit";
    /** 按账期把待出账读数生成房间账单与学生分摊（宿管）。 */
    public static final String BILL_GENERATE = "dorm.ext.bill.generate";
    /** 触发一次未归扫描（宿管手动；定时任务调用同一服务方法）。 */
    public static final String WARNING_SCAN = "dorm.ext.warning.scan";
    /** 未归预警分页查询（宿管）。 */
    public static final String WARNING_LIST = "dorm.ext.warning.list";
    /** 通知辅导员，预警转为已通知。 */
    public static final String WARNING_NOTIFY = "dorm.ext.warning.notify";
    /** 核实完毕，预警转为已核实。 */
    public static final String WARNING_VERIFY = "dorm.ext.warning.verify";
    /** 读取未归预警阈值。 */
    public static final String WARNING_CONFIG_GET = "dorm.ext.warning.config.get";
    /** 修改未归预警阈值。 */
    public static final String WARNING_CONFIG_SET = "dorm.ext.warning.config.set";
    /** 学生提交外来人员来访登记。 */
    public static final String VISITOR_SUBMIT = "dorm.ext.visitor.submit";
    /** 学生查询本人提交的来访登记。 */
    public static final String VISITOR_MINE = "dorm.ext.visitor.mine";
    /** 学生撤销尚未审核的来访登记。 */
    public static final String VISITOR_CANCEL = "dorm.ext.visitor.cancel";
    /** 宿管查询待审核及历史来访登记。 */
    public static final String VISITOR_LIST = "dorm.ext.visitor.list";
    /** 宿管审核来访登记。 */
    public static final String VISITOR_AUDIT = "dorm.ext.visitor.audit";
    /** 提交卫生检查五项分项打分，服务端汇总总分并按需生成复查任务。 */
    public static final String HYGIENE_SUBMIT = "dorm.ext.hygiene.submit";
    /** 查询某次卫生检查的分项明细。 */
    public static final String HYGIENE_DETAIL = "dorm.ext.hygiene.detail";
    /** 按楼栋生成周检查任务清单。 */
    public static final String HYGIENE_TASK_GENERATE = "dorm.ext.hygiene.task.generate";
    /** 卫生检查任务分页查询。 */
    public static final String HYGIENE_TASK_LIST = "dorm.ext.hygiene.task.list";
    /** 学生查询本人当前在宿状态。 */
    public static final String STAY_MINE = "dorm.ext.stay.mine";
    /** 宿管查询全部在住学生的在宿状态。 */
    public static final String STAY_LIST = "dorm.ext.stay.list";
    /** 学生查询本人进出记录（含晚归判定）。 */
    public static final String ACCESS_MINE = "dorm.ext.access.mine";
    /** 读取门禁策略。 */
    public static final String ACCESS_POLICY_GET = "dorm.ext.access.policy.get";
    /** 修改门禁策略。 */
    public static final String ACCESS_POLICY_SET = "dorm.ext.access.policy.set";
    /** 学生设置某张报修单是否允许不在场入内。 */
    public static final String REPAIR_PERMIT_SET = "dorm.ext.repair.permit.set";
    /** 学生查询本人报修单及入内许可。 */
    public static final String REPAIR_PERMIT_MINE = "dorm.ext.repair.permit.mine";
    /** 宿管删除空置房间。 */
    public static final String ROOM_DELETE = "dorm.ext.room.delete";
    /** 学生查看面向自己的宿舍公告（已发布、范围命中本人房间/楼栋）。 */
    public static final String NOTICE_MINE = "dorm.ext.notice.mine";
    /** 宿管查看全部宿舍公告及其类型/范围/置顶。 */
    public static final String NOTICE_LIST = "dorm.ext.notice.list";
    /** 宿管设置某条公告的类型、可见范围与置顶。 */
    public static final String NOTICE_EXTRA_SET = "dorm.ext.notice.extra.set";
    /** 扩展模块运行状态：模块版本、调度器状态与各定时任务的最近一次结果。 */
    public static final String STATUS = "dorm.ext.status";
    /** 立刻执行一个定时任务；载荷为任务名，留空表示全部。 */
    public static final String SCHEDULER_RUN = "dorm.ext.scheduler.run";

    public static final String METER_NOT_FOUND = "DORM_EXT.METER_NOT_FOUND";
    public static final String METER_LOCKED = "DORM_EXT.METER_LOCKED";
    public static final String ROOM_NOT_FOUND = "DORM_EXT.ROOM_NOT_FOUND";
    public static final String BILL_ALREADY_GENERATED = "DORM_EXT.BILL_ALREADY_GENERATED";
    public static final String NO_PENDING_READING = "DORM_EXT.NO_PENDING_READING";
    public static final String WARNING_NOT_FOUND = "DORM_EXT.WARNING_NOT_FOUND";
    public static final String WARNING_INVALID_STATE = "DORM_EXT.WARNING_INVALID_STATE";
    public static final String CONFIG_INVALID = "DORM_EXT.CONFIG_INVALID";
    public static final String VISITOR_NOT_FOUND = "DORM_EXT.VISITOR_NOT_FOUND";
    public static final String VISITOR_INVALID_STATE = "DORM_EXT.VISITOR_INVALID_STATE";
    public static final String NO_ACCOMMODATION = "DORM_EXT.NO_ACCOMMODATION";
    public static final String HYGIENE_NOT_FOUND = "DORM_EXT.HYGIENE_NOT_FOUND";
    public static final String HYGIENE_ITEMS_INVALID = "DORM_EXT.HYGIENE_ITEMS_INVALID";
    public static final String REPAIR_NOT_FOUND = "DORM_EXT.REPAIR_NOT_FOUND";
    public static final String ROOM_OCCUPIED = "DORM_EXT.ROOM_OCCUPIED";
    public static final String ROOM_HAS_HISTORY = "DORM_EXT.ROOM_HAS_HISTORY";
    public static final String POLICY_INVALID = "DORM_EXT.POLICY_INVALID";
    public static final String CONTACT_REQUIRED = "DORM_EXT.CONTACT_REQUIRED";
    public static final String SCHEDULER_UNAVAILABLE = "DORM_EXT.SCHEDULER_UNAVAILABLE";
    public static final String NOTICE_NOT_FOUND = "DORM_EXT.NOTICE_NOT_FOUND";
    public static final String NOTICE_SCOPE_INVALID = "DORM_EXT.NOTICE_SCOPE_INVALID";
    public static final String INVALID_INPUT = "DORM_EXT.INVALID_INPUT";
    public static final String INTERNAL_ERROR = "DORM_EXT.INTERNAL_ERROR";

    private DormExtCommands() { }
}
