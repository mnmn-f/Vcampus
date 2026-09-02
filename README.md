# VCampus 虚拟校园系统

VCampus 是一个基于 Java Swing、TCP Socket、MVC/分层架构和 MySQL 8 的桌面端虚拟校园系统。用户输入校园账号、学号或工号和密码后，服务端从数据库匹配用户、姓名和职责，登录成功后进入对应工作台；本文给出可复现的建库、构建、运行和验收入口，功能是否闭环以 [需求追踪矩阵](docs/REQUIREMENT_TRACEABILITY.md) 为准。

## 工程结构

- [vcampus-common](vcampus-common)：客户端和服务端共享的协议、DTO、枚举、角色与权限。
- [vcampus-client](vcampus-client)：Swing 视图、页面控制器、客户端服务和网络网关。
- [vcampus-server](vcampus-server)：TCP 服务、命令路由、业务服务、事务、DAO 和 MySQL 访问。
- [scripts/start-server.ps1](scripts/start-server.ps1)、[scripts/start-client.ps1](scripts/start-client.ps1)：Windows 服务端/客户端网络启动脚本。
- 设计与验收文档：[ARCHITECTURE](docs/ARCHITECTURE.md)、[FEATURE_BASELINE](docs/FEATURE_BASELINE.md)、[PAGE_MAP](docs/PAGE_MAP.md)、[IMPLEMENTATION_ROADMAP](docs/IMPLEMENTATION_ROADMAP.md)、[DATABASE](docs/DATABASE.md)、[DEPLOYMENT](docs/DEPLOYMENT.md)、[ROLE_MATRIX](docs/ROLE_MATRIX.md)、[UI_SPEC](docs/UI_SPEC.md)、[UI_PREVIEW_GALLERY](docs/UI_PREVIEW_GALLERY.md)。

界面入口可先看 [UI 视觉证据画廊](docs/UI_PREVIEW_GALLERY.md)：其中包含登录页、九类职责主页和代表性业务页。画廊图片是离屏预览，用于核对布局与人员分流，不代替真实数据库验收。

## 运行前提

- JDK 17 或更高版本；源码和字节码统一按 Java 17 兼容级别构建。
- 正常运行需要 MySQL 8.0；显式界面预览模式不访问数据库。
- 项目自带 Maven Wrapper，无需预先安装 Maven。

构建通过 Maven Compiler Plugin 的 `release=17` 统一语言、字节码和 JDK API 级别；日期时间协议仍沿用现有 ThreeTen Backport 类型以保持客户端与服务端兼容。MySQL 使用官方 Connector/J 9.5.0，可连接 MySQL 8.0 并支持其默认认证方式。部署说明见 [DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## MySQL 迁移

V1 建立基线结构；V2 写入演示数据；V3 增加学期、课程学分和绩点统计范围；V4 增加商品图片、分类、促销、优惠券、评价、好友代付和订单价格快照。执行顺序固定为：

    vcampus-server/src/main/resources/db/migration/V1__baseline.sql
    vcampus-server/src/main/resources/db/migration/V2__demo_data.sql
    vcampus-server/src/main/resources/db/migration/V3__academic_insights.sql
    vcampus-server/src/main/resources/db/migration/V4__store_experience.sql

在已创建的 vcampus 数据库上，可以用 MySQL 客户端依次执行：

    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V1__baseline.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V2__demo_data.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V3__academic_insights.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V4__store_experience.sql

脚本包含幂等键和重复保护；正式环境不要直接导入演示账号。学生平均学分绩点按东南大学 4.8 制在服务端统一计算，统计和导出不接受客户端指定他人 userId。结算价格、促销、优惠券、库存和代付扣款同样由服务端事务重算。迁移、约束和真实 MySQL 证据见 [DB_COMPATIBILITY_REPORT.md](docs/DB_COMPATIBILITY_REPORT.md)。

服务端从 JVM 属性或环境变量读取数据库连接：

    vcampus.db.url / VCAMPUS_DB_URL
    vcampus.db.user / VCAMPUS_DB_USER
    vcampus.db.password / VCAMPUS_DB_PASSWORD

不要把数据库密码写入脚本、仓库或命令历史。

## 构建、测试与质量检查

在项目根目录执行：

    .\mvnw.cmd test
    .\mvnw.cmd -Pquality verify
    .\mvnw.cmd package

质量构建成功后生成：

- vcampus-server/target/vCampusServer.jar，入口为 edu.seu.vcampus.server.ServerMain。
- vcampus-client/target/vCampusClient.jar，入口为 edu.seu.vcampus.client.AppLauncher。

可选的真实 MySQL DAO、身份链、生产组合根和并发锁序测试命令与结果见 [DB_COMPATIBILITY_REPORT.md](docs/DB_COMPATIBILITY_REPORT.md)；普通测试默认不依赖外部数据库。

## 正常运行

在项目根目录执行下面两步；第一次运行或代码变更后先构建：

    .\mvnw.cmd package
    java -Dvcampus.db.url="jdbc:mysql://127.0.0.1:3306/vcampus" -Dvcampus.db.user=<db-user> -Dvcampus.db.password=<db-password> -jar .\vcampus-server\target\vCampusServer.jar
    java -jar .\vcampus-client\target\vCampusClient.jar

客户端默认进入网络模式并连接 `127.0.0.1:8888`。跨电脑或修改端口时使用 `vcampus.server.host` 和 `vcampus.server.port`，具体参数见下方“真实网络模式”。

## 本地界面预览

只有需要查看登录、角色主页和导航分流而暂不启动数据库时，才显式启用预览模式：

    java -Dvcampus.client.mode=demo -jar .\vcampus-client\target\vCampusClient.jar

预览模式不访问数据库，只验证登录页、人员信息和角色分流；业务模块统一提示连接服务端，不再维护另一套静态业务页面。登录页只填写账号和密码，页面不提供人员或角色选择；`demo_teacher` 登录成功后可切换其已有的教师和教务职责：

| 登录账号 | 密码 | 登录后职责 |
|---|---|---|
| `demo_student` | `student123` | 学生 |
| `demo_teacher` | `teacher123` | 任课教师 / 教务老师 |
| `demo_registrar` | `registrar123` | 学籍管理员 |
| `demo_academic` | `academic123` | 教务老师 |
| `demo_librarian` | `library123` | 图书管理员 |
| `demo_store` | `store123` | 商店管理员 |
| `demo_dorm` | `dorm123` | 宿管员 |
| `demo_ai` | `ai123` | AI 知识管理员 |
| `demo_system` | `system123` | 系统管理员 |

预览模式不提供业务数据和业务操作；完整功能必须通过服务端和 MySQL 验收。

## 真实网络模式

先启动已初始化数据库的服务端：

    java -Dvcampus.db.url="jdbc:mysql://127.0.0.1:3306/vcampus" -Dvcampus.db.user=<db-user> -Dvcampus.db.password=<db-password> -Dvcampus.server.port=8888 -jar vcampus-server\target\vCampusServer.jar

再在客户端电脑运行：

    java -Dvcampus.client.mode=network -Dvcampus.server.host=127.0.0.1 -Dvcampus.server.port=8888 -jar vcampus-client\target\vCampusClient.jar

局域网部署优先使用 [start-server.ps1](scripts/start-server.ps1) 和 [start-client.ps1](scripts/start-client.ps1)；参数、超时、防火墙和断线语义见 [DEPLOYMENT.md](docs/DEPLOYMENT.md)。跨电脑时把客户端的 server.host 改为服务端局域网 IPv4，不要把客户端自己的 127.0.0.1 当作服务端地址。

执行 V2 后可用于验收的本地账号为：

- demo_student/student123
- demo_teacher/teacher123
- demo_registrar/registrar123
- demo_academic/academic123
- demo_librarian/library123
- demo_store/store123
- demo_dorm/dorm123
- demo_ai/ai123
- demo_system/system123

以上密码仅用于本地演示。正式部署前必须修改或停用全部演示账号；日志、审计、DTO 和会话快照不得记录明文密码、密码哈希或 raw token。

## 当前明确边界

- 排课已由 AcademicCoursesPanel → CourseScheduleEditorPanel 在真实页面行内维护时段，服务端负责冲突和权限校验。
- 线上资源支持访问记录和图书管理员分页日志；借阅台账支持筛选与 CSV 导出，单次最多 5000 条，覆盖已有文件前需确认。
- 未实现后台预警/账单定时调度器。
- AI 当前只保留协议和存储预留，未接入真实模型、RAG、外部知识库或写操作工具，因此不在任何角色的导航中显示。
