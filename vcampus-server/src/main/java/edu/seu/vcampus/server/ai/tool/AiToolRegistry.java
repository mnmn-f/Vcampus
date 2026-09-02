package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.protocol.command.CampusCommands;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.protocol.command.IdentityCommands;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.server.router.CommandRouter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 工具描述与业务命令映射；模块未注册时工具自然处于未就绪状态。 */
public final class AiToolRegistry {
    private final Map<String, AiTool> tools = new LinkedHashMap<String, AiTool>();

    public static AiToolRegistry campusDefaults() {
        AiToolRegistry r = new AiToolRegistry();
        r.add(tool("identity.profile.read", "查询本人账号资料", IdentityCommands.PROFILE_SELF,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("student.profile.read", "查询本人学籍资料", StudentCommands.SELF_PROFILE,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("student.grades.read", "查询本人成绩", StudentCommands.SELF_GRADES,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("academic.schedule.read", "查询本人课表", AcademicCommands.STUDENT_SCHEDULE,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("academic.course.enroll", "按课程编号选课", AcademicCommands.STUDENT_ENROLL,
                true, CampusCommandTool.Kind.ID_ENROLL));
        r.add(tool("academic.course.drop", "按课程编号退课", AcademicCommands.STUDENT_DROP,
                true, CampusCommandTool.Kind.ID_ENROLL));
        r.add(tool("library.book.search", "按关键词检索图书", LibraryCommands.BOOK_SEARCH,
                false, CampusCommandTool.Kind.BOOK_SEARCH));
        r.add(tool("library.borrow.mine", "查询本人借阅记录", LibraryCommands.BORROW_MINE,
                false, CampusCommandTool.Kind.BORROW_MINE));
        r.add(tool("library.book.borrow", "按图书编号借书", LibraryCommands.BOOK_BORROW,
                true, CampusCommandTool.Kind.ID_BORROW));
        r.add(tool("library.book.return", "按借阅记录编号还书", LibraryCommands.BOOK_RETURN,
                true, CampusCommandTool.Kind.ID_RETURN_BORROW));
        r.add(tool("library.study-room.search", "查询可用自习室", LibraryCommands.STUDY_ROOM_SEARCH,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("library.study-room.mine", "查询本人自习室预约", LibraryCommands.STUDY_ROOM_RESERVATIONS,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("library.resource.search", "查询图书馆在线资源", LibraryCommands.RESOURCE_SEARCH,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("store.product.search", "按关键词检索校园商品", StoreCommands.PRODUCT_SEARCH,
                false, CampusCommandTool.Kind.PRODUCT_SEARCH));
        r.add(tool("store.account.read", "查询本人校园账户余额", StoreCommands.ACCOUNT_GET,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("store.cart.read", "查询本人购物车", StoreCommands.CART_GET,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("store.cart.add", "将商品加入购物车", StoreCommands.CART_ADD_ITEM,
                true, CampusCommandTool.Kind.CART_ITEM));
        r.add(tool("store.order.create", "从购物车创建订单", StoreCommands.ORDER_CREATE,
                true, CampusCommandTool.Kind.NONE));
        r.add(tool("store.orders.mine", "查询本人商店订单", StoreCommands.ORDER_MINE,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("store.ledger.read", "查询本人校园账户流水", StoreCommands.ACCOUNT_LEDGER,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("dorm.accommodation.read", "查询本人住宿信息", DormCommands.ACCOMMODATION_MINE,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("dorm.utility.read", "查询本人水电分摊", DormCommands.UTILITY_MINE,
                false, CampusCommandTool.Kind.DORM_PAGE));
        r.add(tool("dorm.repair.mine", "查询本人宿舍报修", DormCommands.REPAIR_LIST,
                false, CampusCommandTool.Kind.DORM_PAGE));
        r.add(tool("dorm.leave.mine", "查询本人宿舍请假", DormCommands.LEAVE_MINE,
                false, CampusCommandTool.Kind.NONE));
        r.add(tool("dorm.announcement.read", "查询宿舍公告", DormCommands.ANNOUNCEMENT_LIST,
                false, CampusCommandTool.Kind.DORM_PAGE));
        r.add(tool("campus.announcement.read", "查询校园公告", CampusCommands.ANNOUNCEMENT_LIST,
                false, CampusCommandTool.Kind.CAMPUS_PAGE));
        r.add(tool("campus.competition.search", "查询校园竞赛", CampusCommands.COMPETITION_LIST,
                false, CampusCommandTool.Kind.CAMPUS_PAGE));
        r.add(tool("campus.competition.register", "按竞赛编号报名", CampusCommands.COMPETITION_REGISTER,
                true, CampusCommandTool.Kind.COMPETITION_REGISTER));
        r.add(tool("campus.competition.cancel", "按竞赛编号取消报名", CampusCommands.COMPETITION_CANCEL,
                true, CampusCommandTool.Kind.CAMPUS_ID));
        r.add(tool("campus.srtp.mine", "查询本人 SRTP 项目", CampusCommands.SRTP_MINE,
                false, CampusCommandTool.Kind.CAMPUS_PAGE));
        r.add(tool("campus.classroom.mine", "查询本人教室申请", CampusCommands.CLASSROOM_MINE,
                false, CampusCommandTool.Kind.CAMPUS_PAGE));
        return r;
    }

    public AiTool get(String name) { return tools.get(name); }
    public boolean isReady(String name, CommandRouter router) {
        AiTool tool = get(name);
        return tool != null && router != null && router.isRegistered(tool.getTargetCommand());
    }
    public Map<String, AiTool> all() { return Collections.unmodifiableMap(tools); }
    public void add(AiTool tool) {
        if (tool == null || tools.containsKey(tool.getName())) {
            throw new IllegalArgumentException("AI tool must be unique");
        }
        tools.put(tool.getName(), tool);
    }

    private static AiTool tool(String name, String description, String command,
                               boolean write, CampusCommandTool.Kind kind) {
        return new CampusCommandTool(name, description, command, write, kind);
    }
}
