package edu.seu.vcampus.common.ai;

/** Shared deterministic regression scenarios; never contain personal records or perform writes. */
public final class AiRouteExamples {
    private AiRouteExamples() { }
    public static String[][] all() {
        return new String[][] {
            {"identity.profile.read", "查看我的个人资料"},
            {"student.profile.read", "查看我的完整学籍信息"},
            {"student.grades.read", "我的成绩是多少"},
            {"academic.schedule.read", "我的课表"},
            {"academic.enrollments.read", "我选了哪些课"},
            {"academic.course.search", "有哪些课程"},
            {"academic.course.enroll", "帮我选课"},
            {"academic.course.drop", "退选课程，编号是TEST-C-016"},
            {"library.book.search", "我想知道扩展测试图书35的信息"},
            {"library.borrow.mine", "查看我的借阅记录"},
            {"library.book.borrow", "帮我借《扩展测试图书35》"},
            {"library.book.return", "帮我还书"},
            {"library.study-room.search", "查询可用自习室"},
            {"library.study-room.mine", "我预约了什么自习室"},
            {"library.study-room.reserve", "帮我预约自习室"},
            {"library.study-room.cancel", "取消自习室预约"},
            {"library.resource.search", "查询图书馆在线资源"},
            {"store.product.search", "商店里有什么商品"},
            {"store.account.read", "我的账户余额"},
            {"store.cart.read", "查看购物车"},
            {"store.cart.add", "把商品 20 加入购物车，数量 3"},
            {"store.cart.update", "修改购物车\n对象编号：20\n商品数量：3"},
            {"store.cart.remove", "把水杯移出购物车"},
            {"store.order.create", "帮我把购物车下单"},
            {"store.order.pay", "帮我支付订单 12"},
            {"store.coupon.claim", "领取优惠券"},
            {"store.orders.mine", "查询未支付订单"},
            {"store.ledger.read", "查看账户流水"},
            {"dorm.accommodation.read", "我的住宿信息"},
            {"dorm.utility.read", "查询水电账单"},
            {"dorm.repair.mine", "查询本人报修"},
            {"dorm.repair.create", "帮我报修"},
            {"dorm.leave.mine", "查询本人请假"},
            {"dorm.leave.submit", "帮我提交请假"},
            {"dorm.leave.cancel", "取消请假"},
            {"dorm.utility.pay", "帮我交水电费"},
            {"dorm.announcement.read", "查看宿舍公告"},
            {"campus.announcement.read", "查询校园公告"},
            {"campus.competition.search", "有哪些校园竞赛"},
            {"campus.competition.mine", "我报名了什么竞赛"},
            {"campus.competition.register", "帮我报名竞赛"},
            {"campus.competition.cancel", "取消报名竞赛"},
            {"campus.srtp.mine", "查询我的SRTP项目"},
            {"campus.classroom.mine", "查询我的教室申请"},
            {"campus.classroom.search", "查询可申请教室"},
            {"campus.classroom.apply", "帮我申请教室"},
            {"campus.classroom.cancel", "取消教室申请"},
        };
    }
}

