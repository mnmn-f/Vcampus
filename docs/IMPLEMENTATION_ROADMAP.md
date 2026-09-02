# 实施路线与验收门禁

本路线图按当前代码和已执行证据收口，状态不是下一轮愿望清单。各模块的逐项映射见 [REQUIREMENT_TRACEABILITY.md](REQUIREMENT_TRACEABILITY.md)，可见页面见 [PAGE_MAP.md](PAGE_MAP.md)。

## 迭代完成状态

| 迭代 | 实际交付 | 当前状态 |
|---|---|---|
| 0. 基础设施 | common 协议、DTO、结果码、角色权限；TCP Socket；客户端/服务端 MVC 分层；MySQL V1/V2 迁移；安全反序列化 | 已完成。协议、权限和跨端类型已有 common 唯一入口 |
| 1. 身份与学籍 | 注册、登录、登出、职责切换、本人资料/改密、账号管理、角色和会话管理、登录/业务审计、注销申请；学生档案、东南大学 4.8 制成绩指标、分学期导出、教师登记和学籍管理员核对 | 已完成。绩点和汇总由服务端统一计算，本人查询不接受 payload 指定 userId |
| 2. 教务与校园 | 课程查询/维护、排课时段行内增改删、选退课、个人课表、教师本人课程；公告、比赛、SRTP、教室申请审批和冲突检查 | 业务闭环已完成。教务管理员在 AcademicCoursesPanel 选课后使用 CourseScheduleEditorPanel 行内维护时段，冲突由服务端校验并回显当前区 |
| 3. 图书馆与商店 | 图书、借还、自习室、线上资源访问/日志、借阅台账/CSV；商品图片和分类、购物车结算、促销/优惠券、订单、评价、好友代付、销售统计/趋势 | 已完成。商店价格、折扣、库存、优惠券消费和代付扣款均在服务端事务内裁决；趋势只统计 PAID/COMPLETED 的实付金额 |
| 4. 宿舍 | 楼栋/房间/床位、空间维护、住宿关系、入住/调宿/退宿申请审批、请假、门禁/未归、卫生、报修与评价、水电分摊缴费、宿舍公告 | 已完成真实学生/宿管页面和 InMemory/MySQL 服务链。后台自动生成预警/账单尚未作为定时任务交付 |
| 5. 系统收口 | 统一组合根、activeRole 权限、网络异常边界、累计反序列化预算、跨模块安全、MySQL 真实验证和质量命令 | 核心收口已完成；文档与视觉页面继续作为验收前整理项 |

## 验收门禁

答辩演示或交付前按以下顺序检查；任一门禁失败，不把相邻的静态页面描述为功能完成。

1. **结构门禁**：从 [ServerMain.java](../vcampus-server/src/main/java/edu/seu/vcampus/server/ServerMain.java) 的生产组合根核对一个 JdbcConnectionFactory、一个 TransactionManager、一个 SessionManager 和一个 CommandRouter；从 [ClientBusinessServices.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/composition/ClientBusinessServices.java) 核对一个网络请求边界和一个客户端会话。
2. **数据库门禁**：新库按 V1 → V2 → [V3__academic_insights.sql](../vcampus-server/src/main/resources/db/migration/V3__academic_insights.sql) → [V4__store_experience.sql](../vcampus-server/src/main/resources/db/migration/V4__store_experience.sql) 执行；检查结果以 [DB_COMPATIBILITY_REPORT.md](DB_COMPATIBILITY_REPORT.md) 为准。
3. **身份门禁**：登录后所有请求的 userId、activeRole 和权限均来自服务端 SessionContext；角色切换必须经过 auth.switch-role；普通角色不能调用其他业务管理员写命令。
4. **业务门禁**：选课容量/重复/时段冲突、比赛容量/报名窗口、教室占用、床位唯一占用、支付幂等、报修/请假/住宿审批状态机和注销审批回滚均有服务端测试。
5. **事务门禁**：写操作由服务层 TransactionManager 提交/回滚，DAO 不自行提交；锁顺序固定且不通过降低隔离级别掩盖冲突。教室申请和审批统一 classroom → reservation → overlap 检查。
6. **页面门禁**：按 [PAGE_MAP.md](PAGE_MAP.md) 登录对应真实页面；页面操作能从真实 client service 走到 registry、service、repository 和数据库，不能只检查按钮存在。
7. **网络门禁**：执行客户端/服务端网络测试，确认 EOF、超时、坏帧不会泄露堆栈；登出或强制下线后旧 token 得到未授权，普通 Socket 断开不自动伪造重放非幂等请求。
8. **质量门禁**：在项目根目录执行以下命令，三模块测试和质量检查均无失败：

        .\mvnw.cmd test
        .\mvnw.cmd -Pquality verify
        .\mvnw.cmd package

9. **敏感信息门禁**：演示账号只用于本地演示，密码必须在部署前替换或停用；报告、日志、DTO 和会话快照不得出现明文密码、密码哈希或 raw token。

## 交付边界

以下项目不能在验收时写成“已完成”：

- AI 只有 AiAssistantGateway、页面和 Disabled 降级实现；真实模型、RAG、外部知识库和写操作工具未接入。
- 当前未发现服务端后台定时任务；门禁预警和账单页面提供查询/人工处理，不等价于后台调度器。
- 宿舍公告与校园通用公告都复用 announcements 表，但客户端暂保留 DormAnnouncementService/Panel 和 CampusAnnouncementService/Panel 两个边界；后续可在不改表的前提下统一适配。
