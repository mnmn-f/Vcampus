# VCampus 虚拟校园系统

VCampus 是一个基于 Java Swing、TCP Socket、MVC/分层架构和 MySQL 8 的桌面端虚拟校园系统。用户输入校园账号、学号或工号和密码后，服务端从数据库匹配用户、姓名和职责，登录成功后进入对应工作台；本文给出可复现的建库、构建、运行和验收入口，功能是否闭环以 [需求追踪矩阵](docs/REQUIREMENT_TRACEABILITY.md) 为准。

## 工程结构

- [vcampus-common](vcampus-common)：客户端和服务端共享的协议、DTO、枚举、角色与权限。
- [vcampus-client](vcampus-client)：Swing 视图、页面控制器、客户端服务和网络网关。
- [vcampus-server](vcampus-server)：TCP 服务、命令路由、业务服务、事务、DAO 和 MySQL 访问。
- [scripts/start-lan-server.ps1](scripts/start-lan-server.ps1)、[scripts/start-lan-client.ps1](scripts/start-lan-client.ps1)：Windows 局域网共享启动脚本；单机启动仍可使用 `start-server.ps1`、`start-client.ps1`。
- 设计与验收文档：[ARCHITECTURE](docs/ARCHITECTURE.md)、[FEATURE_BASELINE](docs/FEATURE_BASELINE.md)、[PAGE_MAP](docs/PAGE_MAP.md)、[IMPLEMENTATION_ROADMAP](docs/IMPLEMENTATION_ROADMAP.md)、[DATABASE](docs/DATABASE.md)、[DEPLOYMENT](docs/DEPLOYMENT.md)、[ROLE_MATRIX](docs/ROLE_MATRIX.md)、[UI_SPEC](docs/UI_SPEC.md)、[UI_PREVIEW_GALLERY](docs/UI_PREVIEW_GALLERY.md)、[AI_MODULE](docs/AI_MODULE.md)。

界面入口可先看 [UI 视觉证据画廊](docs/UI_PREVIEW_GALLERY.md)：其中包含登录页、九类职责主页和代表性业务页。画廊图片是离屏预览，用于核对布局与人员分流，不代替真实数据库验收。

## 运行前提

- JDK 17 或更高版本；源码和字节码统一按 Java 17 兼容级别构建。
- 正常运行需要 MySQL 8.0；显式界面预览模式不访问数据库。
- 项目自带 Maven Wrapper，无需预先安装 Maven。

构建通过 Maven Compiler Plugin 的 `release=17` 统一语言、字节码和 JDK API 级别；日期时间协议仍沿用现有 ThreeTen Backport 类型以保持客户端与服务端兼容。MySQL 使用官方 Connector/J 9.5.0，可连接 MySQL 8.0 并支持其默认认证方式。部署说明见 [DEPLOYMENT.md](docs/DEPLOYMENT.md)。

## MySQL 迁移

V1 建立基线结构；V2 写入演示数据；V3 增加学期、课程学分和绩点统计范围；V4 扩展商店；V5 扩展宿舍；V6 增加教师排课偏好；V7-V9 增加维修员、维修复核和住宿申请调整；V10/V11 增加 AI 系统指南、校纪校规和操作知识；V12 增加知识版本审计和脱敏回答反馈；V13 续期演示欢迎券；V14 增加订单物流；V15 增加图书 PDF 与详情字段；V16 增加 AI 反馈处理状态和关联知识；V17 扩展头像上传字段。执行顺序固定为：

    vcampus-server/src/main/resources/db/migration/V1__baseline.sql
    vcampus-server/src/main/resources/db/migration/V2__demo_data.sql
    vcampus-server/src/main/resources/db/migration/V3__academic_insights.sql
    vcampus-server/src/main/resources/db/migration/V4__store_experience.sql
    vcampus-server/src/main/resources/db/migration/V5__dorm_extension.sql
    vcampus-server/src/main/resources/db/migration/V6__teacher_time_preferences.sql
    vcampus-server/src/main/resources/db/migration/V7__dorm_repair_worker.sql
    vcampus-server/src/main/resources/db/migration/V8__dorm_repair_review.sql
    vcampus-server/src/main/resources/db/migration/V9__dorm_request_bed_optional.sql
    vcampus-server/src/main/resources/db/migration/V10__ai_assistant_knowledge.sql
    vcampus-server/src/main/resources/db/migration/V11__ai_knowledge_and_tools.sql
    vcampus-server/src/main/resources/db/migration/V12__ai_quality_workbench.sql
    vcampus-server/src/main/resources/db/migration/V13__store_coupon_refresh.sql
    vcampus-server/src/main/resources/db/migration/V14__store_order_shipping.sql
    vcampus-server/src/main/resources/db/migration/V15__library_pdf_and_book_details.sql
    vcampus-server/src/main/resources/db/migration/V16__ai_admin_workflow.sql
    vcampus-server/src/main/resources/db/migration/V17__identity_avatar_upload.sql

在已创建的 vcampus 数据库上，可以用 MySQL 客户端依次执行：

    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V1__baseline.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V2__demo_data.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V3__academic_insights.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V4__store_experience.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V5__dorm_extension.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V6__teacher_time_preferences.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V7__dorm_repair_worker.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V8__dorm_repair_review.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V9__dorm_request_bed_optional.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V10__ai_assistant_knowledge.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V11__ai_knowledge_and_tools.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V12__ai_quality_workbench.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V13__store_coupon_refresh.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V14__store_order_shipping.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V15__library_pdf_and_book_details.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V16__ai_admin_workflow.sql
    mysql --default-character-set=utf8mb4 -u <db-user> -p < vcampus-server/src/main/resources/db/migration/V17__identity_avatar_upload.sql

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
    java -Dvcampus.db.url="jdbc:mysql://127.0.0.1:3306/vcampus?useSSL=false&allowPublicKeyRetrieval=true" -Dvcampus.db.user=<db-user> -Dvcampus.db.password=<db-password> -jar .\vcampus-server\target\vCampusServer.jar
    java -jar .\vcampus-client\target\vCampusClient.jar

客户端默认进入网络模式并连接 `127.0.0.1:8888`。跨电脑或修改端口时使用 `vcampus.server.host` 和 `vcampus.server.port`，具体参数见下方“真实网络模式”。

## AI 校园助手与小松鼠桌宠

校园助手提供问答、聊天、代办三种模式，支持多轮会话、流式纯文本回答、Enter 发送（Shift+Enter 换行）、校园知识检索、实时业务查询和需要二次确认的校园操作；聊天框上方不再显示横向滑动的快捷问题按钮。查询结果会结合问题中的对象和字段词做精细投影，例如“我的成绩”只返回成绩，“完整学籍信息”才展开学籍，“《书名》的作者”只突出作者；业务事实始终来自原业务服务，配置模型 API 后只允许模型在已授权的实时结果范围内整理和提取，API 不可用时由本地规则完成同样的字段收敛。问答模式收到写指令、代办模式收到查询指令、聊天模式收到 VCampus 查询或操作指令时，会提示切换到相应模式。

聊天区使用右侧用户气泡、左侧助手气泡和独立业务结果卡；缺少代办参数时显示可填写的参数卡，自习室、教室和请假时间使用日期时间选择器。“补充并继续代办”会连同原始写指令重新提交已标注参数，保持原工具意图，参数齐全后进入二次确认而不是降级为查询。聊天模式可上传最多 3 个图片或文档，附件处理期间显示进度圈并禁止发送，首次模型初始化失败会在尚未输出内容时自动重试。会话侧栏默认收起，生成期间禁止切换会话；失败重试复用同一逻辑 requestId 并原位替换失败气泡，不重复保存用户消息。侧栏支持搜索、重命名、归档、恢复已归档会话和纯文本导出。每条助手回复可单独点赞、点踩或纠错。默认模型服务为 DeepSeek Responses API，服务端可配置 `VCAMPUS_AI_API_KEY` 或 `DEEPSEEK_API_KEY`。

知识库迁移 `V11__ai_knowledge_and_tools.sql` 已录入学生公寓管理、学生违纪处分、系统操作与新增代办指南。课程、图书、订单、竞赛、学籍等动态事实不复制进知识库，而是继续通过原业务服务实时查询。

AI 知识管理员拥有知识库管理、知识测试、批量回归、用户反馈、路由测试、工具状态和运行监控七个工作区。知识库支持服务端分页；文档导入可预览和修改分段、正文去重并选择性导入，整批写入共用一个事务。知识测试可填写标准答案关键词和预期命中知识，批量回归用于检查知识更新影响。反馈可按评价、问题类型、处理状态和时间筛选，支持待处理、已处理、忽略以及关联知识跳转。路由测试只解析工具和参数，不执行校园业务；工具状态显示最近调用、成功率、平均耗时和最近错误。升级已有数据库时必须依次执行 `V12__ai_quality_workbench.sql` 和 `V16__ai_admin_workflow.sql`。

登录职责拥有 `AI_ASSISTANT` 入口时，小松鼠桌宠默认显示在主窗口右下角：

- 在主窗口内可自由拖动松鼠，位置会自动保存；单击可进入校园助手，已经位于 AI 页面时只播放回应动画。
- 最小化主窗口后，原窗口隐藏并收束为透明、无边框、置顶的桌面松鼠；可自由拖动，单击恢复最小化前的页面和窗口状态。
- 右键可以“喂一颗松果”或“摸摸小松鼠”，好感度在本机持久化；桌面模式另提供“展开主界面”和“打开校园助手”。
- 松鼠只显示固定的非敏感状态气泡，并跟随 AI 状态播放不同动作：待机呼吸、悬停招手、思考歪头、回答摆手、待确认提醒、成功跳跃和失败低落。
- 桌宠控制器每 12 秒执行一次轻量连接探测（主窗口最小化后仍继续）；服务器断开后松鼠持续显示“网络离线”，连接恢复后自动回到待机状态。
- 无 AI 权限的职责不显示松鼠，仍使用系统原生最小化行为；退出登录或关闭程序会释放桌宠窗口和动画计时器。

桌宠使用内置透明 PNG 基础形象 [squirrel.png](vcampus-client/src/main/resources/edu/seu/vcampus/client/pet/squirrel.png) 和六姿势动作表 [squirrel-actions.png](vcampus-client/src/main/resources/edu/seu/vcampus/client/pet/squirrel-actions.png)。登录页左侧品牌区使用正面趴扶、挥手且不露脚的 [squirrel-login-wave.png](vcampus-client/src/main/resources/edu/seu/vcampus/client/pet/squirrel-login-wave.png)，与放大的 `VCampus` 字样作为一组居中展示；该视觉调整不改变原登录表单、注册入口或注册流程。Swing 按状态循环切帧并叠加呼吸、摇摆、跳跃、倾斜和阴影补间，不依赖 GIF 播放库。自动化测试或低性能环境可添加 JVM 参数 `-Dvcampus.pet.animation=false` 禁用补间和循环切帧，但仍保留对应状态的静态姿势。完整设计、运行边界与扩展接口见 [AI_MODULE](docs/AI_MODULE.md) 和 [UI_SPEC](docs/UI_SPEC.md)。

## 本地界面预览

只有需要查看登录、角色主页和导航分流而暂不启动数据库时，才显式启用预览模式：

    java -Dvcampus.client.mode=demo -jar .\vcampus-client\target\vCampusClient.jar

预览模式不访问数据库，主要用于验证登录页、人员信息和角色分流。图书馆模块额外提供进程内可操作数据，可检查图书借还、自习室预约和线上资源流程；关闭客户端后数据重置。其他业务模块仍需连接服务端。登录页只填写账号和密码，页面不提供人员或角色选择；`demo_teacher` 登录成功后可切换其已有的教师和教务职责：

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

图书馆预览可直接运行 `.\scripts\start-library-demo.ps1`。该预览不替代真实权限、事务和 MySQL 验收，正式业务仍应使用网络模式。

## 真实网络模式

先启动已初始化数据库的服务端：

    java -Dvcampus.db.url="jdbc:mysql://127.0.0.1:3306/vcampus?useSSL=false&allowPublicKeyRetrieval=true" -Dvcampus.db.user=<db-user> -Dvcampus.db.password=<db-password> -Dvcampus.server.port=8888 -jar vcampus-server\target\vCampusServer.jar

再在客户端电脑运行：

    java -Dvcampus.client.mode=network -Dvcampus.server.host=127.0.0.1 -Dvcampus.server.port=8888 -jar vcampus-client\target\vCampusClient.jar

局域网部署优先使用 [start-lan-server.ps1](scripts/start-lan-server.ps1) 和 [start-lan-client.ps1](scripts/start-lan-client.ps1)；参数、共享数据、防火墙和断线语义见 [DEPLOYMENT.md](docs/DEPLOYMENT.md)。跨电脑时把客户端的 server.host 改为服务端局域网 IPv4，不要把客户端自己的 127.0.0.1 当作服务端地址。

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
- AI 助手已接入 DeepSeek Responses API、校园知识 RAG、校纪校规与系统操作知识、聊天附件、稳定重试和归档恢复，以及 47 个业务工具；涉及数据变更的 20 个工具必须二次确认。AI 知识管理员可分页维护知识、批量回归、处理反馈、检查工具路由和查看运行指标。配置、权限边界、工具清单和运行方法见 [AI_MODULE](docs/AI_MODULE.md)。
