# VCampus 数据库基线（MySQL 8）

## 1. 基线与执行顺序

数据库名称为 `vcampus`，目标版本为 MySQL 8.0.16 及以上。字符集统一使用 `utf8mb4`，存储引擎统一使用 InnoDB。V1 创建基线，V2 写入演示数据，V3 增加学期/学分/绩点统计字段，V4 扩展商店，V5 扩展宿舍，V6 增加教师排课偏好，V7-V9 增加维修员、维修复核和住宿申请调整，V10/V11 增加 AI 知识片段，V12 增加 AI 知识版本和回答反馈，V13 续期已过期的演示欢迎券，V14 增加订单物流。

执行顺序：

```text
V1__baseline.sql -> V2__demo_data.sql -> V3__academic_insights.sql -> V4__store_experience.sql -> V5__dorm_extension.sql -> V6__teacher_time_preferences.sql -> V7__dorm_repair_worker.sql -> V8__dorm_repair_review.sql -> V9__dorm_request_bed_optional.sql -> V10__ai_assistant_knowledge.sql -> V11__ai_knowledge_and_tools.sql -> V12__ai_quality_workbench.sql -> V13__store_coupon_refresh.sql -> V14__store_order_shipping.sql
```

PowerShell 或命令行执行示例：

```bash
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V1__baseline.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V2__demo_data.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V3__academic_insights.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V4__store_experience.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V5__dorm_extension.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V6__teacher_time_preferences.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V7__dorm_repair_worker.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V8__dorm_repair_review.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V9__dorm_request_bed_optional.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V10__ai_assistant_knowledge.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V11__ai_knowledge_and_tools.sql
mysql --default-character-set=utf8mb4 -u <user> -p < \
  vcampus-server/src/main/resources/db/migration/V12__ai_quality_workbench.sql
  vcampus-server/src/main/resources/db/migration/V13__store_coupon_refresh.sql
  vcampus-server/src/main/resources/db/migration/V14__store_order_shipping.sql
```

脚本使用 `CREATE DATABASE IF NOT EXISTS`、`CREATE TABLE IF NOT EXISTS` 和自然键/幂等键，因此可以在同一个演示库重复执行。它面向全新库；如果已有表的结构与基线不一致，不应靠重复执行修复，而应新增后续版本迁移。生产环境不应直接执行演示数据脚本。

V2 中的 `password_hash` 是 README 所列本地演示密码的 BCrypt 哈希，用于验证真实 TCP/MySQL 登录链路。正式部署必须修改密码或停用演示账号；任何注册、改密和重置密码流程都禁止写入明文。

## 2. 建模原则

### 2.1 用户、角色与职责

`users` 保存账号和公共资料，`student_profiles`、`teacher_profiles` 保存对应的业务身份资料。`user_roles` 是多对多关系，同一用户可以拥有多个职责角色；`user_sessions.current_role_id` 只表示当前工作台，不改变用户拥有的角色集合。

角色按业务职责拆分，而不是使用一个笼统的管理员角色：

- `STUDENT`：个人学籍、成绩、选退课、借阅预约、购物、宿舍申请与 AI 查询。
- `TEACHER`：本人授课课程、本人课程成绩和教室申请。
- `REGISTRAR`：学籍档案与成绩档案。
- `ACADEMIC_ADMIN`：课程排期、教务公告、比赛、SRTP、教室审批。
- `LIBRARIAN`：图书借还、自习室、线上资源和图书馆公告。
- `STORE_MANAGER`：商品、库存、订单和销售统计。
- `DORM_MANAGER`：住宿、调宿退宿、门禁预警、卫生、报修、水电和宿舍公告。
- `AI_KNOWLEDGE_ADMIN`：知识片段和 AI 调用日志。
- `SYSTEM_ADMIN`：账号、角色和系统运行状态。

权限代码与 `vcampus-common` 中的 `Role`、`Permission` 枚举保持一致。客户端可以隐藏没有权限的导航和操作，服务端必须根据会话中的用户和角色再次鉴权。`audit_logs.actor_role_id` 记录实际执行操作时使用的职责角色。

### 2.2 业务实体与公共复用

本基线不使用带大量空字段的 `record_type` 宽表：

- 公告是真正同构的数据，统一放在 `announcements`，以 `module_code` 和可见范围区分业务来源。
- 账户资金变化是真正同构的数据，统一放在 `account_transactions`；充值、消费、退款、宿舍缴费使用不同 `transaction_type`。
- 商品订单使用 `store_orders` 和 `store_order_items`，销售统计由 MySqlStoreSalesRepository 直接聚合已支付订单明细，不重复保存销售事实；V1 的 `vw_store_sales` 仅作兼容/查询辅助。
- 图书借阅、自习室预约、宿舍报修、卫生检查、水电分摊和 AI 调用各自保留明确实体。
- AI 使用 `ai_chat_sessions`、`ai_chat_messages`、`ai_knowledge_chunks` 和 `ai_tool_call_logs` 四张自有存储表，已实现可选模型、混合知识检索和白名单工具编排；课程、图书、订单、宿舍等事实仍由原业务表和服务维护，不在 AI 表中复制。

### 2.3 时间、金额和状态

- 时间统一使用 `DATETIME(3)`，数据库连接时区按项目约定使用东八区；跨端对象传输时使用明确的时间格式。
- 金额统一使用 `DECIMAL(12,2)`，禁止使用浮点数。`account_transactions.amount` 为有符号金额：充值/退款为正，消费/水电缴费为负。
- 状态字段使用 `VARCHAR` 加 MySQL 8 `CHECK`，状态常量必须同步到 Common 模块，不能在业务代码中散落字符串。
- 主键统一使用 `BIGINT UNSIGNED AUTO_INCREMENT`；外键明确表达业务归属，正常业务不物理删除用户、订单、借阅和审计历史，而是改为状态。

## 3. 表分组

| 分组 | 表 |
|---|---|
| 身份与审计 | `users`、`roles`、`permissions`、`user_roles`、`role_permissions`、`user_sessions`、`login_audits`、`audit_logs` |
| 身份资料 | `student_profiles`、`teacher_profiles` |
| 教务 | `courses`、`course_instructors`、`course_schedules`、`classrooms`、`enrollments`、`course_grades`、`announcements`、`competitions`、`competition_registrations`、`srtp_records`、`classroom_reservations` |
| 图书馆 | `books`、`borrow_records`、`study_rooms`、`study_room_reservations`、`online_resources`、`online_resource_access_logs` |
| 商店与账户 | `accounts`、`account_transactions`、`products`、`shopping_carts`、`cart_items`、`store_orders`、`store_order_items`、`store_categories`、`store_promotions`、`store_coupons`、`store_user_coupons`、`store_product_reviews`、`store_friend_payments` |
| 宿舍 | `dorm_buildings`、`dorm_rooms`、`dorm_beds`、`accommodation_records`、`accommodation_requests`、`leave_requests`、`access_records`、`late_return_alerts`、`hygiene_inspections`、`repair_orders`、`utility_bills`、`utility_allocations` |
| AI 接口存储 | `ai_chat_sessions`、`ai_chat_messages`、`ai_knowledge_chunks`、`ai_tool_call_logs` |

## 4. 事务边界与关键约束

事务必须由服务器业务服务层开启和提交，DAO 只执行持久化语句，不自行提交事务。一个事务只围绕一个清晰的业务用例；跨模块调用时由外层用例服务统一决定事务边界。

### 4.1 登录、角色与会话

登录事务/流程：客户端只提交校园账号、学号或工号和密码；服务端通过 `users.username`、`student_profiles.student_no`、`teacher_profiles.employee_no` 匹配唯一用户并加载姓名和有效角色 → 校验状态和密码哈希 → 创建 `user_sessions` → 写入 `login_audits`。密码校验失败也写登录审计，但不创建会话。登出或管理员强制下线只更新 `revoked_at`。

角色分配事务：校验操作者具有 `ROLE_MANAGE` → 写入或删除 `user_roles` → 写入 `audit_logs`。删除最后一个角色前必须由服务层阻止，避免出现没有工作台权限的账号；`SYSTEM_ADMIN` 不应通过客户端给自己无限扩权。

### 4.2 选课和退课

选课事务：

1. 校验学生状态、课程状态和选课时间窗口。
2. 对课程行执行 `SELECT ... FOR UPDATE`，检查当前有效选课数量是否小于 `courses.capacity`。
3. 查询学生已有有效课程及 `course_schedules`，检查同一星期、时间段和日期范围是否重叠。
4. 检查 `enrollments` 的唯一键，插入或更新为 `ENROLLED`。
5. 写审计日志并提交。

退课在同一事务中更新 `enrollments.status = 'DROPPED'` 与 `dropped_at`。不要只在客户端判断容量、重复选课或时间冲突。

### 4.3 成绩登记

教师登记成绩前必须确认当前用户存在于该课程的 `course_instructors`；学籍管理员可以按其权限核对。服务层校验 0—100 分、学生选课状态和重复成绩，随后在 `course_grades` 的唯一 `enrollment_id` 上执行插入或更新，并记录 `recorded_by`。

### 4.4 公告、比赛、SRTP和教室

- 公告发布事务更新状态、发布时间和失效时间；`visible_scope = 'ROLE'` 时必须提供 `target_role_id`。
- 比赛报名事务锁定比赛行，检查报名截止时间、发布状态、容量和 `competition_registrations` 唯一键。
- SRTP审核事务只能由教务职责角色执行，审核结果必须写 `reviewed_by`、`reviewed_at` 和意见。
- 教室申请/审批事务锁定申请行，并查询同一教室在 `PENDING`/`APPROVED` 状态下的时间重叠：`existing.start_at < new_end_at AND existing.end_at > new_start_at`。数据库索引帮助检索，跨行的“不重叠”规则必须由服务层在事务中执行。

### 4.5 借还图书

借书事务：锁定 `books` 行 → 校验图书状态和 `available_copies > 0` → 插入 `borrow_records` → 将可借数量减一 → 提交。还书事务：锁定借阅记录和图书行 → 防止重复归还 → 写 `returned_at` 和 `status = 'RETURNED'` → 可借数量加一。逾期状态可由定时任务或查询时更新，但不得绕过事务修改库存。

### 4.6 自习室预约

预约事务锁定自习室行，检查开放状态、开放时间、时间段重叠和用户已有重复预约，再插入 `study_room_reservations`。取消只能由预约人或图书管理员执行，并保留取消记录。数据库无法用普通唯一键表达任意时间段不重叠，因此必须在服务层使用事务和一致的锁顺序。

### 4.7 商店支付

支付是一个不可拆分的事务：

1. 锁定订单、账户和所有商品行，并确认订单为 `CREATED`。
2. 重新计算订单明细总额，不能信任客户端传来的总额。
3. 检查商品仍为 `ON_SALE`、库存充足、账户为 `ACTIVE` 且余额足够。
4. 扣减库存、扣减账户余额、插入一条负金额 `account_transactions`、更新订单为 `PAID`。
5. 使用唯一 `idempotency_key` 防止网络重试造成重复扣款；全部成功后提交。

销售统计只读取 `store_orders.status IN ('PAID','COMPLETED')` 的订单明细。退款必须另起事务写正金额流水并更新订单状态，不能修改历史支付流水。

### 4.8 宿舍入住、调宿、退宿和缴费

- 入住/调宿/退宿审批事务锁定申请、相关床位和现有住宿记录，校验床位状态和学生当前住宿关系，更新 `accommodation_records`、`dorm_beds.status`，再更新申请状态。
- `accommodation_records` 的两个生成列唯一键保证最多一个有效学生关系和一个有效床位关系；历史 `ENDED`/`CANCELLED` 记录允许重复。
- 请假审批只更新申请状态和审批信息，不由后台任务静默改变门禁记录。
- 水电账单生成使用 `(room_id, period_start, period_end)` 唯一键保证幂等；分摊总额由服务层校验不超过账单总额。
- 学生缴费事务锁定 `utility_allocations`、`utility_bills` 和 `accounts`，检查分摊状态和余额，插入负金额 `account_transactions`，更新分摊为 `PAID` 并刷新账单状态。
- 门禁预警、账单生成等定时任务只生成可审计的记录；不在后台直接执行不可逆审批或调宿。

### 4.9 AI 会话、知识与工具确认

AI 对话先写入会话和消息表，知识问答只检索 `ai_knowledge_chunks.status = 'ACTIVE'` 的片段；`V11__ai_knowledge_and_tools.sql` 增加公寓管理、违纪处分和系统操作摘要。当前未部署独立向量库：服务端从 MySQL 读取有界候选并在内存做关键词、同义词和轻量文本向量混排，`embedding_json` 保留为空。写工具先在 `ai_tool_call_logs` 创建具有有效期的待确认记录；参数不完整时先澄清，不创建待确认记录。确认时由数据库原子领取，随后携带当前登录会话重新进入原 `CommandRouter`，由原业务服务执行权限、状态和事务校验。AI 不直接写课程、借阅、购物车、订单、竞赛或宿舍业务表，也不接受问题文本指定他人身份。聊天附件只随当前请求传输、不写入消息表。客户端小松鼠桌宠没有数据库表，只在本机 `Preferences` 中保存桌面位置和好感度。

## 5. 跨表约束（由服务层保证）

以下规则不能只依靠单表 `CHECK` 或普通唯一键，必须在服务层、事务和测试中明确：

- `student_user_id` 必须拥有学生资料，`teacher_user_id` 必须拥有教师资料。
- 教师只能登记本人授课课程的成绩；管理员只能处理其职责范围内的业务。
- 课程容量、选课时间冲突、比赛报名容量和教室/自习室时间冲突。
- 住宿楼性别规则、房间容量与床位数量同步。
- 水电分摊合计、订单明细合计、账户当前余额与流水余额链一致。
- 状态流转只能按照业务状态机进行，例如报修 `SUBMITTED -> ACCEPTED -> IN_PROGRESS -> COMPLETED`，不允许客户端直接跳过审批。
- 外键只保证引用存在，不代表角色、所有权和数据范围已经授权。

## 6. 代码映射约定

服务器 DAO 按表/聚合边界提供接口，业务 Service 负责校验、权限、事务和状态机；客户端只通过 Common DTO 和命令访问服务器。建议 Common DTO 使用与表字段等价但不暴露数据库内部细节的命名，例如 `studentUserId`、`courseId`、`startAt`。

建议的关键命名映射：

| 数据库 | 服务层聚合 | 主要职责角色 |
|---|---|---|
| `users`、`user_roles`、`user_sessions` | `AuthService` / `UserService` | 系统管理员、所有用户的本人资料 |
| `student_profiles`、`course_grades` | `StudentRecordService` | 学生、学籍管理员、任课教师 |
| `courses`、`enrollments`、`classroom_reservations` | `AcademicService` | 学生、教师、教务老师 |
| `books`、`borrow_records`、`study_room_reservations` | `LibraryService` | 学生、图书管理员 |
| `accounts`、`products`、`store_orders` | `StoreService` | 学生、商店管理员 |
| `accommodation_records`、`repair_orders`、`utility_bills` | `DormService` | 学生、宿管员 |
| `ai_chat_*`、`ai_tool_call_logs` | `AiAssistantGateway` | 学生、AI知识管理员 |

页面新增、编辑、审批和错误反馈优先放在同一页面的详情区、侧栏或内嵌表单；数据库事务完成后由服务返回结构化结果，客户端按统一加载、空状态、成功和错误状态更新页面。只有危险操作的最终确认使用对话框。

## 7. 静态审查记录

本机当前没有 `mysql`/`mysqld`/MariaDB 客户端或可用 MySQL 服务，因此无法执行真实建库和外键检查。已完成以下静态审查：

- V1 表的创建顺序覆盖所有被引用表，外键目标均在前面创建。
- 外键约束名、主键、唯一键和表名无重复定义；V1 不包含 `record_type` 宽表。
- 所有状态字段均有 `CHECK`，时间段、成绩、金额、库存和状态转移相关的单表边界均有约束。
- V2 使用自然键、唯一键或 `NOT EXISTS`，并在整个演示种子外层使用事务，可重复执行而不会新增重复演示行。
- V2 只保存 BCrypt 哈希；README 列出本地演示密码，正式环境必须替换或停用这些账号。

在首次接入 MySQL 8 后，应执行两份脚本并补充 `SHOW CREATE TABLE`、外键、检查约束、视图和重复执行结果的集成验证。
