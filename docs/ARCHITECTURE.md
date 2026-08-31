# 统一架构

## 1. 调用方向与唯一组合根

业务依赖只向下流动：

    Swing View
      -> Controller / Page
      -> Client Service
      -> NetworkClientService
      -> SocketClientGateway
      -> TCP Server
      -> CommandRouter
      -> Server Service
      -> Repository / DAO
      -> MySQL

服务端生产组合根是 [ServerMain.java](../vcampus-server/src/main/java/edu/seu/vcampus/server/ServerMain.java) 的 createProductionRouter。它装配一个 JdbcConnectionFactory、一个 TransactionManager、一个 SessionManager 和一个 CommandRouter，再把各模块 Registry 接到同一个路由器。测试用 createRouter 可替换仓储，但不改变生产入口。

客户端网络组合根是 [ClientBusinessServices.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/composition/ClientBusinessServices.java)。[AppLauncher.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/AppLauncher.java) 默认创建一个 SocketClientGateway、一个 NetworkClientService 和一个 ClientSession；身份、学籍、教务、校园、图书、商店和宿舍服务共享这条请求边界。只有显式指定 `vcampus.client.mode=demo` 才进入本地外壳预览；该模式不维护第二套静态业务页面，所有业务模块复用同一个“需要连接服务端”空状态页。

下层通过接口和构造器接收依赖，禁止通过全局静态对象跨层调用。模块只登记自己的命令、DTO、服务和仓储，不复制 Socket 循环或路由逻辑。

所有模块以 Java 7 源码和字节码级别构建。Common、Client、Server 统一使用 ThreeTen Backport 承载协议和数据库日期时间，避免客户端与服务端出现两套时间类型；质量构建通过 Animal Sniffer 阻止误用 Java 8+ API。MySQL 驱动统一由父 POM 管理为 Connector/J 5.1.49，子模块不重复声明版本。

## 2. MVC 与服务边界

- Model：Common DTO/Domain、Server Service、Repository/DAO、数据库。
- View：Swing Panel、Frame、TableModel 和页面反馈组件。
- Controller：绑定界面事件，调用 Client Service，维护加载、成功、空和错误状态。
- Client Service：整理 DTO、发送命令和映射结果，不承载权限、容量、金额、时间冲突等服务端规则。
- Server Service：读取 SessionContext、执行细粒度授权、校验状态机、定义事务并调用 DAO。
- DAO：只执行 PreparedStatement 和结果映射，不拼界面文案、不决定权限、不自行提交事务。

客户端可以提前校验必填项，但服务端是唯一业务校验边界。页面按钮可隐藏无权操作，但隐藏不是授权。

## 3. activeRole 是唯一授权角色

一个用户可以拥有多个角色，但每个会话只有一个 SessionContext.activeRole。SessionContext.allows(permission) 只按 activeRole 调用 common RolePolicy；CommandRouter 根据 handler 的 requiredPermission 先做鉴权，各业务 Service 再做对象归属和状态校验。不能通过 session.getRoles() 对多个角色取并集放权，也不能信任 payload 中的 userId、角色或 reviewerId。

角色切换必须通过 auth.switch-role 到服务端，再由客户端同步 ClientSession.activeRole 和菜单。本人业务的 userId、操作人和审计角色始终从 SessionContext 读取。系统管理员不因拥有 USER_MANAGE 而获得教务、图书、商店或宿舍业务写权限。

## 4. 事务、约束与锁序

[TransactionManager.java](../vcampus-server/src/main/java/edu/seu/vcampus/server/db/TransactionManager.java) 在服务层用例外部打开连接、关闭自动提交、提交或回滚；DAO 使用当前连接，不自行提交。跨表状态必须在一个清晰用例事务中完成。

当前关键锁序如下：

| 用例 | 锁/校验顺序 |
|---|---|
| 选课/退课 | courses 行 FOR UPDATE → enrollment/已有课表检查 → 容量、重复和时段冲突 → 写入 |
| 教室申请/审批 | classroom 行 FOR UPDATE → reservation 行 FOR UPDATE → overlap 检查 → 状态写入；申请和审批保持相同方向 |
| 图书借还 | books 行 → borrow_records 行 → 库存和借阅状态一起写入 |
| 商店支付 | order → account → product 库存 → 重算明细金额、写流水和订单状态 |
| 宿舍入住/调宿/缴费 | 当前住宿或分摊 → 目标 bed/account → 写记录和状态；活动学生/床位由生成列唯一键保护 |

数据库唯一键、外键和 CHECK 负责单行边界，服务层事务负责跨行冲突和身份范围。遇到并发冲突返回稳定结果码，不通过降低隔离级别或吞掉死锁掩盖问题。教室锁序由 CampusClassroomLockIntegrationTest 覆盖。

## 5. 公告与公共模型复用

同构的校园、教务和图书馆公告共用 announcements 表，并由 CampusAnnouncementService 按 module_code、visible_scope、target_role_id、publish_at 和 expire_at 查询，客户端复用 CampusAnnouncementsPanel。宿舍当前保留 DormAnnouncementService、DormAnnouncementDto 和 DormAnnouncementsPanel，以兼容宿舍专用请求边界，但仍写入同一表的 DORM 模块行；这是兼容层，不是重复建表。后续可将宿舍页面适配通用服务。

账户流水、销售事实和业务历史同样保留明确实体；销售统计由已支付订单明细聚合，V1 的 vw_store_sales 只作兼容查询辅助，不作为 MySqlStoreSalesRepository 的唯一事实来源。

## 6. Token、Socket 与反序列化

登录页只采集校园账号、学号或工号和密码，不采集或选择角色。客户端将两项凭证一次封装为 `AUTH_LOGIN` 请求；服务端统一查询 `users.username`、`student_profiles.student_no` 和 `teacher_profiles.employee_no`，匹配唯一用户后加载姓名和有效角色，校验密码、创建会话并返回 `LoginResult`。登录后的工作台由返回的角色决定；同一账号的多角色只能在登录成功后通过 `auth.switch-role` 切换。

登录后 token 只作为 Message.sessionToken 在请求中传输，由服务端同一个 SessionManager 解析为 SessionContext。Socket EOF、超时或重连只影响连接，不自动注销有效 token；logout、强制下线、账号批准注销或服务停止才使令牌失效。token 不进入页面 DTO、审计文本或脱敏会话快照。

SafeObjectInputStream 只允许 common DTO/协议、必要的数值/时间/枚举/集合类型，拒绝代理类和非协议容器；限制是每条持久 TCP 连接所有对象读取的累计 8 MiB 预算，而非每个请求单独重置。非法类、断帧和异常关闭对应连接，不向客户端返回堆栈。

## 7. 安全、审计与密码

密码只以 BCrypt 哈希存储和校验；登录成功/失败、状态变化、角色变更、业务写操作和注销审批写入相应审计表。DAO 全部使用 PreparedStatement。系统管理员查看会话时只得到编号、账号、activeRole 和时间，不得到 raw token。账号注销批准先在同一事务写审核结果、禁用用户并标记持久会话，事务提交后才调用 SessionManager.invalidateUserSessions。

## 8. AI 边界

AiAssistantGateway、AiQuery 和 AiStreamListener 只定义问答、流式片段和取消边界；当前没有真实模型服务，也不向任何角色授予 AI 权限或显示入口。ai_chat_* 和 ai_tool_call_logs 是存储预留，不代表已经接入真实模型、RAG、外部知识库或写操作工具。

## 9. 当前诚实边界

- 排课已形成真实页面闭环：教务管理员在 AcademicCoursesPanel 选中课程后，由 CourseScheduleEditorPanel 行内新增、修改和删除时段；冲突与非法状态仍由服务端校验。
- 线上资源访问由 RESOURCE_ACCESS 写入访问日志，图书管理员通过 RESOURCE_ACCESS_LOGS 分页查看；借阅台账通过 BORROW_ADMIN_LIST 筛选并支持最多 5000 条 CSV 导出，覆盖已有文件前由页面确认。
- 未发现服务端后台预警/账单定时调度器；查询和人工处理不等于自动任务。
- 宿舍公告和通用公告的客户端服务尚未合并，保留明确兼容边界。
