# MySQL 兼容性与真实验证报告

日期：2026-08-29  
范围：student、academic、library、store、dorm、campus 以及 identity/root 认证链的 MySQL DAO、V1 基线结构和 V2 演示数据。

## 结论

V1 在 MySQL 8.0.36 的空数据库上建库成功，V2 成功导入并可重复执行；已完成业务模块及 identity/root DAO 的表名、列名和 DTO/状态值与 V1/V2 一致。实际 JDBC 集成测试通过了种子读取、课程写入回滚、占床唯一冲突、已缴分摊 CHECK 冲突、身份资料/密码/状态/角色/审计链、生产组合路由以及教室并发审批。

本轮修复了两个会导致 MySQL 8 建表失败的问题，并清除了本轮范围内的 `VALUES()` 弃用警告：

1. `announcements.target_role_id` 同时参与角色 CHECK 和 `ON DELETE SET NULL` 外键时，MySQL 报 `ERROR 3823`；改为默认 `NO ACTION/RESTRICT`，避免删除被公告引用的角色后破坏公告约束。
2. `utility_allocations.paid_transaction_id` 同时参与已缴状态 CHECK 和 `ON DELETE SET NULL` 外键时具有同样问题；改为默认 `NO ACTION/RESTRICT`。
3. V2 的 upsert 改用 MySQL 8 推荐的 `VALUES (...) AS new` 行别名；`user_roles` 的 `INSERT ... SELECT` 改为显式使用当前系统管理员变量。student 成绩 DAO 使用行别名，store 购物车 DAO 使用绑定参数表达新增数量。
4. 根认证 `MySqlUserRepository` 的用户 upsert 同样改用 MySQL 8 行别名；`vw_store_sales` 只统计 `PAID`/`COMPLETED`，与“已支付订单”基线一致，排除已退款订单。

## 环境与迁移执行

Docker CLI 存在，但 `docker version` 在有限等待后无服务端响应，已中断；系统未发现 mysql/mysqld/mariadb。随后使用官方 MySQL Community 8.0.36 Windows ZIP，在仓库外的 `E:\.codex_tmp_read\mysql-test` 建立临时实例，TCP 端口 3307，仅用于本次验证。

在临时数据库执行：

```powershell
$mysql='E:\.codex_tmp_read\mysql-test\mysql-8.0.36-winx64\bin\mysql.exe'
$schema=Get-Content -Raw -Encoding UTF8 'vcampus-server/src/main/resources/db/migration/V1__baseline.sql'
$schema | & $mysql --no-defaults --protocol=tcp --host=127.0.0.1 --port=3307 `
  --user=integration --password=integration --show-warnings --default-character-set=utf8mb4 --batch
$data=Get-Content -Raw -Encoding UTF8 'vcampus-server/src/main/resources/db/migration/V2__demo_data.sql'
$data | & $mysql --no-defaults --protocol=tcp --host=127.0.0.1 --port=3307 `
  --user=integration --password=integration --show-warnings --default-character-set=utf8mb4 --batch
```

在修复后的干净数据库上结果为 `v1-clean-exit=0`、`v2-clean-exit=0`。再次执行 V2 结果为 `v2-repeat-clean-exit=0`，且没有 MySQL `VALUES()` 弃用警告（命令行密码提示不属于 SQL 警告）。重复执行后的关键计数如下：

| 数据 | 数量 | 数据 | 数量 |
| --- | ---: | --- | ---: |
| users（演示账号） | 9 | roles | 9 |
| permissions | 38 | courses | 1 |
| announcements | 2 | competitions | 1 |
| competition_registrations | 1 | srtp_records | 1 |
| classroom_reservations | 1 | books | 1 |
| borrow_records | 1 | dorm_buildings | 1 |
| accommodation_records | 1 | leave_requests | 1 |
| utility_bills | 1 | utility_allocations | 1 |
| store_orders | 1 | ai_chat_messages | 2 |
| BCrypt 哈希账号 | 9 |  |  |

数据库实际读取到的代表状态为：`DEMO-SE-001/PUBLISHED`、ACADEMIC 与 DORM 公告均为 `PUBLISHED`、演示图书 `ON_SHELF` 且可借 2 册、演示商品 `ON_SALE` 且库存 19、D1 楼栋 `OPEN`、演示学生 `ENROLLED`。

V1 在该实例中生成 50 张基线表。`information_schema` 核验到 courses 的 `capacity INT UNSIGNED NOT NULL`、`total_hours SMALLINT UNSIGNED NULL`，enrollments 的 `version INT UNSIGNED NOT NULL`，utility_allocations 的金额/支付列类型和可空性正确；住宿记录的 `uk_accommodation_active_student` 与 `uk_accommodation_active_bed` 两个生成列唯一索引有效。CHECK 约束包括课程、选课、比赛、住宿、订单、公告和水电分摊状态约束；修复后的两个外键在 MySQL 中显示 `NO ACTION`。

## DAO 与 schema 对照

逐个阅读本轮范围内的 MySQL DAO SQL，并与 V1 的 `CREATE TABLE`、列声明、CHECK、外键及 V2 自然键逐项核对。列名、`NULL`/非空、unsigned 数值列和 DTO/枚举值均未发现其他不一致。

| 模块 | DAO 负责的表 | 核验结果 |
| --- | --- | --- |
| student | `student_profiles`、`teacher_profiles`、`users`、`user_roles`、`roles`、`course_grades`、`enrollments`、`courses`、`course_instructors` | 本人档案、成绩查询及教师归属查询列名一致；成绩 upsert 已改为行别名。 |
| academic | `classrooms`、`courses`、`course_instructors`、`course_schedules`、`enrollments` | 课程/排课/选课 SQL 与 V1 一致；容量和日期可空绑定正确。 |
| library | `books`、`borrow_records`、`study_rooms`、`study_room_reservations`、`online_resources`、`online_resource_access_logs` | 图书、借阅、自习室和线上资源字段全部对应 V1。 |
| store | `accounts`、`account_transactions`、`products`、`shopping_carts`、`cart_items`、`store_orders`、`store_order_items` | 余额、账务、库存、订单状态和明细快照字段一致；购物车数量更新已消除旧式 `VALUES()`。 |
| dorm | `dorm_buildings`、`dorm_rooms`、`dorm_beds`、`accommodation_records`、`accommodation_requests`、`access_records`、`late_return_alerts`、`hygiene_inspections`、`repair_orders`、`utility_bills`、`utility_allocations`、`announcements` | 床位唯一占用、治理记录和水电分摊列与 V1 一致；支付外键按 MySQL 8 约束修复。 |
| campus | `announcements`、`competitions`、`competition_registrations`、`srtp_records`、`classroom_reservations`、`classrooms` | 代码使用实际存在的 `srtp_records`，没有误用旧参考中的 `srtp_projects`。公告模块/角色/时间窗口列一致；销售视图仅纳入 PAID/COMPLETED。 |
| identity/root | `users`、`roles`、`user_roles`、`user_sessions`、`login_audits`、`audit_logs`、`account_cancellation_requests` | root 登录按 users 与有效角色加载；identity 查询、资料、状态、密码、角色、审计和注销申请 DAO 使用 V1 列及 CHECK/FK；会话查询只返回脱敏摘要，不读写 raw token。注销申请的生成列唯一键保证每个用户最多一个 PENDING。 |

静态守护测试为 [`DatabaseCompatibilityTest.java`](../vcampus-server/src/test/java/edu/seu/vcampus/server/db/DatabaseCompatibilityTest.java)，覆盖上述 DAO 所依赖的表/列集合、两个 MySQL 8 外键约束、V2 事务边界、实际 `srtp_records` 表名和 9 个演示账号。V2 账号密码与 BCrypt 哈希由已有 `DemoPasswordSeedTest` 验证，9/9 通过；本轮没有将哈希校验误报为完整登录流程，真实登录 API 属于 identity 范围。

## 真实 JDBC 集成验证

可选测试位于 [`MySqlDaoIntegrationTest.java`](../vcampus-server/src/test/java/edu/seu/vcampus/server/db/MySqlDaoIntegrationTest.java)，默认跳过，不使普通单元测试依赖外部数据库。使用上面的临时 MySQL 实例执行：

```powershell
$mvnArgs=@('-pl','vcampus-server','-am',
  '-Dvcampus.mysql.integration=true',
  '-Dvcampus.db.url=jdbc:mysql://127.0.0.1:3307/vcampus',
  '-Dvcampus.db.user=integration','-Dvcampus.db.password=integration',
  '-Dsurefire.failIfNoSpecifiedTests=false','-Dtest=MySqlDaoIntegrationTest','test')
& '.\mvnw.cmd' @mvnArgs
```

结果：`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

- 用真实 MySQL DAO 读取 student、academic、library、store、dorm、campus 的 V2 种子行。
- 通过 `MySqlAcademicRepository.saveCourse` 写入临时课程，再由 `TransactionManager` 注入异常；回滚后按课程编码查询为 0 行。
- 直接验证数据库边界：重复使用活动床位触发生成唯一索引；写入缺少 `paid_transaction_id/paid_at` 的 `PAID` 分摊触发 CHECK；两次均回滚且没有测试数据残留。

### 3.6 identity/root、生产组合根与锁序

在同一临时 MySQL 8.0.36 实例上执行以下三个默认跳过的可选测试：

```powershell
$mvnArgs=@('-pl','vcampus-server','-am','-Dvcampus.mysql.integration=true',
  '-Dvcampus.db.url=jdbc:mysql://127.0.0.1:3307/vcampus',
  '-Dvcampus.db.user=integration','-Dvcampus.db.password=integration',
  '-Dsurefire.failIfNoSpecifiedTests=false',
  '-Dtest=MySqlIdentityIntegrationTest,ProductionRouterMySqlIntegrationTest,CampusClassroomLockIntegrationTest','test')
& '.\mvnw.cmd' @mvnArgs
```

结果为 `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`（DAO 3、身份/root 2、生产组合根 1、锁序并发 1）。

- `MySqlIdentityIntegrationTest` 注册隔离账号并在 finally 中删除；实际走 root `MySqlUserRepository`/BCrypt 登录、identity.profile.self/update、密码修改、角色分配与撤销、禁用/恢复、root upsert，再分页读取 login/business audit。数据库约束与事务由 `IdentityService` 的 `TransactionManager` 承担，测试不打印密码哈希或会话令牌，临时账号清理后计数为 0。
- `ProductionRouterMySqlIntegrationTest` 将同一个 `JdbcConnectionFactory` 和 `SessionManager` 传入 `ServerMain.createProductionRouter`，完成 `demo_student` 登录 → `identity.profile.self` → `academic.course.list` → logout；旧 token 再请求 profile 得到 `AUTH.UNAUTHORIZED`。
- `CampusClassroomLockIntegrationTest` 插入同一教室、相邻时段的两个 PENDING 申请并并发审批，两个请求均在 15 秒内 APPROVED。申请和审批现在统一按 `classroom FOR UPDATE → reservation FOR UPDATE → overlap check`，没有降低隔离级别或吞掉死锁。

### 3.7 账号注销申请

`account_cancellation_requests` 已加入 V1，使用 `pending_user_id` STORED 生成列和唯一索引约束同一用户最多一个待处理申请，并以 CHECK 约束限定状态及审核字段。`IdentityCancellationService` 的批准流程在同一事务中锁定用户和申请、写入审核结果、禁用用户并标记持久会话；事务提交后才清理运行时会话，回滚不会提前注销令牌。提交/撤回使用 `PROFILE_UPDATE`，本人列表使用 `PROFILE_READ`，管理员列表和审批使用 `USER_MANAGE`，范围身份均来自 `SessionContext`。

本轮 `AccountCancellationServiceTest` 4 项、`DatabaseCompatibilityTest` 2 项和客户端服务测试通过。在隔离 MySQL 8.0.36 上 V1、V2 及 V2 重复执行成功，`MySqlAccountCancellationIntegrationTest` 2/2 通过；另以数据库探针实测待处理唯一键返回 1062、用户外键返回 1452、非法状态/审核字段 CHECK 返回 3819，所有探针事务回滚且无残留。身份/DAO 7/7 以及生产路由、教室锁序 2/2 也在同一实例通过。

普通工程测试与质量检查：

```powershell
.\mvnw.cmd -pl vcampus-server -am test -DskipTests=false
.\mvnw.cmd -Pquality verify
```

2026-08-30 完成 Java 7 兼容迁移后，从干净目录执行 `mvnw.cmd clean -Pquality verify`：common 8、client 68、server 114，合计 190 项，0 失败，10 项真实 MySQL 条件测试默认跳过；Java 7 API 门禁与 CPD 均通过。源码和字节码均为 Java 7 级别，不再以 source/target 8 构建。

为满足 Java 7，JDBC 驱动统一为 Connector/J 5.1.49。上文 MySQL 8.0.36 的真实测试证明了 schema、事务和 DAO 逻辑，但发生在驱动统一之前；部署前仍应按本报告命令用最终 JAR 重跑 10 项条件测试。Connector/J 5.1.49 不能登录使用 `caching_sha2_password` 的数据库账号，因此 MySQL 8.0 应为应用账号配置 `mysql_native_password`；这只影响数据库连接账号，不改变 VCampus 业务用户的 BCrypt 密码哈希。

## 事务锁顺序与剩余边界

| 业务 | 当前锁/事务顺序 | 结论或剩余风险 |
| --- | --- | --- |
| academic 选课/退课 | `courses FOR UPDATE` → `enrollments FOR UPDATE` → 容量/冲突检查 → 写入 | 同一课程容量串行化；学生跨课程的冲突查询是读检查，未使用范围锁。 |
| library 借还 | 图书行 → 借阅记录 | 库存更新和借阅状态在同一事务；未发现表名或 FK 不一致。 |
| store 支付/退款 | 订单 → 账户 → 商品库存 | 余额、账务、库存一起提交；幂等键由 V1 唯一约束保护。 |
| dorm 入住/调宿/缴费 | 当前住宿/分摊 → 目标床位或账户 → 写记录 | 活动学生/床位生成列唯一；并发重复待审批申请没有数据库唯一键，是后续可加强的业务锁点。 |
| campus 教室 | 申请、审核均为教室 → 申请 → 冲突预约 | 通过真实 MySQL 两并发审批测试；审批先读取申请取得 classroom_id，再按统一顺序锁教室和申请，随后检查重叠并更新状态。 |

`leave_requests` 已由独立 DormLeaveService、MySqlDormLeaveRepository、命令和学生/宿管真实页面覆盖；它与入住/调宿/退宿的 `dorm.request` 是两个状态机，不合并复用。身份/root 链已在 3.6 实测，未将密码或 token 放入 DTO、日志或报告。

## 临时资源

验证结束后已停止临时 `mysqld`（TCP 3307），并删除仓库内及仓库外本轮创建的 `.codex_tmp_read/mysql-test` 临时二进制、数据目录和 ZIP；这些内容不属于交付物。仓库 `.codex_tmp_read` 中其他已有截图等文件不在本轮清理范围。
