# VCampus AI 助手模块说明

## 1. 文档目的

本文说明 VCampus AI 助手模块的设计目标、现有功能、系统边界和运行方式；模块边界以当前代码和需求追踪表为准。

AI 助手不是一套独立的校园业务系统。它是 VCampus 的自然语言入口，负责：

1. 回答普通问答以及系统操作步骤问题；
2. 基于校园知识库回答校纪校规、办事规则等问题；
3. 识别用户的校园业务意图；
4. 调用现有业务模块的服务接口查询数据或执行操作；
5. 将业务结果整理为对话内容返回；
6. 对写操作执行二次确认、权限校验和调用审计。

AI 模块不复制教务、图书、商店、宿舍等模块的业务逻辑，不直接访问这些模块的 DAO，也不为这些业务新增数据表。

## 2. 当前实现状态

| 能力 | 当前状态 |
|---|---|
| 问答、聊天、代办三种模式 | 已实现 |
| 多轮对话、历史会话与归档恢复 | 已实现，模型使用最近 12 条上下文；归档会话可恢复 |
| 流式回答、停止生成与稳定重试 | 已实现；生成期间锁定会话切换，重试复用逻辑 requestId |
| DeepSeek Responses API 模型调用 | 已实现，默认 `deepseek-v4-flash` |
| 聊天图片与办公文档附件 | 已实现，最多 3 个；图片自动使用视觉模型，PDF/DOCX/PPTX/XLS/XLSX 在客户端提取文字 |
| 聊天框上方横向快捷问题 | 已移除，输入区保持简洁 |
| API 不可用时知识库降级 | 已实现 |
| 系统操作步骤问答 | 已实现 |
| 校纪校规证据约束 | 已实现，已录入公寓管理与违纪处分主题知识 |
| 关键词与轻量文本向量混合检索 | 已实现 |
| 业务模块查询工具 | 已覆盖常用本人业务，并按用户所问字段精确返回 |
| 业务模块写工具 | 已实现 20 个，全部要求二次确认；补参后保持原写意图继续执行 |
| 三种模式边界提示 | 已实现，跨模式指令会提示切换到问答或代办模式 |
| 知识片段分页和事务批量导入 | 已实现，支持分段预览、修改、正文去重和选择性导入 |
| 标准答案、预期命中和批量回归测试 | 已实现 |
| 反馈处理闭环 | 已实现，支持筛选、处理状态和关联知识跳转 |
| 工具路由测试与运行指标 | 已实现；路由测试不执行工具，指标包含成功率、耗时和最近错误 |
| 会话、知识和工具运行监控 | 已实现 |
| 小松鼠桌宠入口、最小化收束与 AI 状态联动 | 已实现 |
| 独立向量数据库 | 当前无需使用；中小语料由 MySQL + 轻量混合排序承担 |
| PDF、Office、文本分段导入知识库 | 已实现；扫描件 OCR 尚未实现 |
| 后台自动向量索引任务 | 当前无需实现；尚无独立嵌入索引需要刷新 |

### 2.1 客户端小松鼠桌宠

桌宠属于客户端表现层，不改变 AI 协议、服务端 API 或其他业务模块：

- 主窗口展开时，松鼠默认位于右下角，可在分层面板内自由拖动且不参与业务页面布局计算；窗口内位置单独保存，单击可打开 `ModuleId.AI_ASSISTANT`。
- 有 AI 权限的职责最小化主窗口时，客户端记录正常/最大化状态和当前页面，隐藏主窗口并显示透明、无边框、置顶的 `JWindow`；单击恢复原状态，拖动超过 5 像素时只移动桌宠。
- 桌面位置通过 `Preferences` 保存，加载时会按当前多显示器可见区域校正；系统不支持逐像素透明时降级为普通无边框小窗。
- `PetMood` 包含 `IDLE`、`HOVER`、`THINKING`、`TALKING`、`ATTENTION`、`SUCCESS`、`ERROR`。气泡只使用固定短文案，不展示用户问题、模型回答或业务对象内容。
- 动作表按状态选择不同姿势：`IDLE` 待机、`HOVER` 招手、`THINKING` 托腮歪头、`TALKING` 开口招手、`ATTENTION` 举手提醒、`SUCCESS` 跳跃、`ERROR` 低落；悬停和回答状态会循环切帧，其他状态叠加相应补间动画。
- `AiChatPanel` 通过可选 `PetActivityListener` 上报状态，旧构造方法仍然可用。发送、首段回答、写操作确认、完成、失败和取消分别驱动对应状态。
- 权限变化、退出登录和窗口销毁会停止动画并销毁桌宠窗口。设置 JVM 属性 `vcampus.pet.animation=false` 可关闭动画。
- 右键菜单提供“喂一颗松果”和“摸摸小松鼠”，播放成功/亲昵动作并在本机 `Preferences` 累积最高 100 的好感度；互动不会自动发起模型请求或校园操作。`PetInteractionHandler` 仍作为后续互动扩展点。

桌宠素材为项目内透明 PNG：基础形象 `vcampus-client/src/main/resources/edu/seu/vcampus/client/pet/squirrel.png`，六姿势动作表 `vcampus-client/src/main/resources/edu/seu/vcampus/client/pet/squirrel-actions.png`。动作表固定为 3 列 × 2 行，渲染使用 Swing 双三次缩放、循环切帧和轻量呼吸、摇摆、跳跃、倾斜及阴影动画，不依赖 GIF 或外部动画运行库。登录页另使用 `squirrel-login-wave.png`：松鼠正面趴在放大的 `VCampus` 字样上，一只手扶字、一只手挥手且不露脚，品牌组合位于左侧绿色区域中央；这只调整品牌视觉，不改变登录框、注册入口和注册流程。

## 3. 总体架构

AI 请求仍使用 VCampus 原有的客户端、TCP 协议、登录会话和服务端路由。

```text
AiChatPanel
    ├─ PetActivityListener ──→ PetStateModel ──→ SquirrelPetWidget / JWindow
    └─ 专用流式 Socket ──→ AiCommands / AiQuery
                              ↓
                         AiAssistantService
                              ├─ ModeIntentClassifier ──→ 跨模式切换提示
                              ├─ ToolIntentParser ──→ AiToolRegistry ──→ ToolBridge
                              │                                            ↓
                              │                                      CommandRouter
                              │                                            ↓
                              │                                  原业务 Handler / Service / DAO
                              │
                              ├─ ToolResultFormatter ──→ 字段投影 ──→ ResponsesAiModel（可选整理）
                              └─ AiKnowledgeService ──→ AiKnowledgeRepository
                                                         ↓
                                                   KnowledgeRanker
                                                         ↓
                                                  ResponsesAiModel
```

主要组件职责：

| 组件 | 职责 |
|---|---|
| `AiAssistantService` | 编排会话、知识检索、模型回答、工具调用、确认和降级 |
| `ModeIntentClassifier` | 识别当前模式不应处理的校园查询或写操作，并返回切换模式提示 |
| `ToolIntentParser` | 使用可测试的确定性规则识别常见校园业务意图 |
| `ModelToolIntentResolver` | 规则未命中时由大模型在工具白名单内泛化识别表达 |
| `NamedEntityResolver` | 用实时列表把课程名、书名、商品名、竞赛名解析为编号 |
| `AiToolRegistry` | 保存 AI 工具名、原业务命令、读写类型和结构化参数说明 |
| `CampusCommandTool` | 把对话中提取的参数转换为 `vcampus-common` 中已有 DTO |
| `ToolBridge` | 使用当前会话 token 将工具请求重新交给 `CommandRouter` |
| `ToolResultFormatter` | 把业务 DTO、分页数据和列表转换为中文摘要，并依据问题中的字段词收敛结果 |
| `AiKnowledgeRepository` | 读取、维护并检索 MySQL 中的知识片段 |
| `KnowledgeRanker` | 对候选知识进行关键词和轻量文本向量混合排序 |
| `AiPromptBuilder` | 将用户问题、知识证据或已授权实时数据及回答约束组合为模型输入 |
| `ResponsesAiModel` | 调用兼容 Responses API 的 HTTPS 接口并解析 SSE 增量 |
| `PetStateModel` | 保存非敏感桌宠状态和短气泡，并负责临时状态超时复位 |
| `SquirrelPetController` | 管理权限可见性、主窗口最小化/恢复、透明桌面窗口、拖动和位置持久化 |
| `SquirrelPetWidget` | 加载透明基础图和六姿势动作表，按状态切帧并使用 Swing 绘制缩放、阴影、气泡和补间动画 |

## 4. 一次提问的处理流程

### 4.1 查询型业务问题

以“查询我的成绩”为例：

1. 客户端发送 `ai.query`；
2. `ToolIntentParser` 识别为 `student.grades.read`；
3. `AiToolRegistry` 将它映射到 `StudentCommands.SELF_GRADES`；
4. `ToolBridge` 携带当前用户会话进入原 `CommandRouter`；
5. 学籍模块按当前用户和活动角色再次鉴权；
6. 原服务查询数据库并返回业务 DTO；
7. `ToolResultFormatter` 依据问题中的对象词和字段词投影结果；
8. 配置模型 API 时，模型只在已授权的实时结果内做整理提取；模型不可用或输出越界时直接使用本地格式化结果；
9. AI 保存消息和工具日志，并流式返回客户端。

这类结果的业务事实来自真实业务服务，不接受问题中伪造的用户编号。模型只承担表达整理，不能补造数据库未返回的字段。字段粒度示例：

- “我的成绩是多少”调用成绩工具，只返回课程成绩和成绩汇总，不附带完整学籍；
- “我的学院是什么”只返回学籍中的学院字段；
- “我的完整学籍信息”才返回学号、姓名、学院、专业、年级等完整学籍字段；
- “《数据库系统》的作者是谁”按书名检索后只突出书名和作者；询问“完整信息/详情”时才展开 ISBN、出版社、分类、馆藏等字段。

### 4.2 写操作问题

以“选课 1001”为例：

1. 意图解析器识别 `academic.course.enroll` 和课程编号；
2. 若缺少必要参数，客户端显示参数补充卡；“补充并继续代办”把原始指令和带字段名的补充值一并重新提交，解析器继续保持原写工具，不先调用对应查询工具；
3. 参数齐全后，AI 创建一个有效期为 5 分钟的待确认动作；
4. 客户端显示确认信息，此时业务数据尚未改变；
5. 用户确认后，AI 才通过 `CommandRouter` 调用原选课命令；
6. 教务模块重新检查权限、课程状态、容量、重复选课和时间冲突；
7. 成功或失败结果写入工具日志并返回对话。

取消、超时、重复确认或原业务模块拒绝时，不得伪报成功。

### 4.3 模式边界

三种模式各自承担明确职责，`ModeIntentClassifier` 在工具执行或知识回答前检查跨模式意图：

- 问答模式用于校园事实和系统使用说明；收到选课、预约、借还、支付、提交等写指令时，回答用户切换到代办模式；
- 代办模式用于执行校园操作；收到成绩、学籍、图书信息、余额等只读问题时，回答用户切换到问答模式；
- 聊天模式用于通用对话；收到 VCampus 数据查询或操作指令时，分别提示切换到问答模式或代办模式。

模式提示只说明应切换的入口，不会在错误模式中偷偷执行校园工具。一般闲聊仍留在聊天模式。

### 4.4 知识问答

如果没有命中校园工具，请求进入知识问答流程：

1. 从 `ai_knowledge_chunks` 读取最多 500 条 `ACTIVE` 候选；
2. 对问题进行校园同义词扩展；
3. 计算标题/正文关键词分值；
4. 计算字符词项向量的余弦相似度；
5. 过滤仅有无意义单字重合的候选；
6. 选取前 5 条知识片段；
7. 将来源类型、标题和有界正文交给模型；
8. 要求模型用知识标题自然说明依据，并输出易读纯文本；
9. 模型不可用时，直接返回带标题的知识库降级结果。

当前方案属于轻量混合 RAG。每次从 MySQL 取最多 500 条活动知识，在进程内进行关键词、校园同义词和字符词项余弦混排，因此“独立向量数据库未使用”表示没有部署 Milvus、Qdrant、pgvector 等额外服务；“后台自动向量索引任务未实现”表示没有异步生成 embedding 并刷新索引的定时任务。这不是数据库未连接：知识片段来自 MySQL，课程、馆藏、订单等动态数据则通过业务工具实时读取。

当前几十条到数百条经审核知识上，单独引入向量数据库会增加嵌入成本、部署组件、索引一致性和权限隔离复杂度，未必提高正确率。知识增长到数千/数万段、同义改写召回明显下降，或需要跨大量文档语义检索时，再引入嵌入模型、增量索引队列和专业向量索引更合适；上层 `AiKnowledgeService` 接口可保持不变。

## 5. 系统操作步骤问答

V2 演示数据已经提供以下系统知识：

- 学生选课并加入课表；
- 教务老师添加课程和上课时段；
- 图书借阅与归还；
- 校园商店购物；
- 宿舍报修与水电查询；
- 账号资料、学籍和成绩查询；
- 选课业务规则；
- AI 业务代办安全规则；
- 校纪校规知识回答范围。

对于“如何、怎么、怎样、操作步骤、在哪里、从哪里”等说明性问法，AI 会进入知识问答，不会执行真实写操作。例如：

```text
怎么在课表里添加课程？
如何归还图书？
在哪里查看宿舍报修进度？
怎样查询自己的成绩？
```

操作类回答被要求包含：

1. 适用角色；
2. 功能入口；
3. 完整编号步骤；
4. 需要确认的操作；
5. 成功完成的页面标志；
6. 失败时应检查的条件。

系统步骤必须根据当前真实页面和业务实现维护。页面入口或流程发生变化时，应同步更新知识片段。

## 6. 校纪校规知识

`V11__ai_knowledge_and_tools.sql` 已按主题录入两份扫描材料的经核验摘要：学生公寓的入住调宿、门禁访客、消防用电、禁用电器、卫生设施和秩序要求，以及学生违纪处分的原则、处分种类、公共秩序、网络行为、宿舍消防、考试学术诚信和处分申诉程序。公寓文件在学校官网对应“2025年6月修订版”；用户提供的本地文件名标注“2026年”，知识条目明确记录这一差异，不把文件名误当作正式版本号。

政策知识用于问答提示和出处定位，不替代正式文件、处分决定或申诉告知；制度更新后，`AI_KNOWLEDGE_ADMIN` 应停用旧片段并录入新版本。

### 6.1 管理员质量工作台

AI 知识管理员页面分为知识库管理、知识测试、批量回归、用户反馈、路由测试、工具状态和运行监控。知识库列表按服务端分页读取。PDF、DOCX、PPTX、XLS/XLSX、CSV、文本和代码文件会在客户端提取文字并按不超过 3,500 字分段；管理员确认前可以预览、修改、取消选择并开启正文去重。选中的分段通过 `ai.knowledge.import` 在一个事务中提交，任一分段失败时整批回滚。每次新建、修改、停用和回滚都会向 `ai_knowledge_versions` 追加不可覆盖的版本快照。

“知识测试”复用学生问答的知识混合检索和提示词，但不写入学生会话，也禁止执行工具；管理员可以填写标准答案关键词和预期命中知识标题。批量回归逐题显示答案和知识命中是否通过。“用户反馈”只读取评价、类型、补充说明和脱敏问题摘要，可按评价、类型、处理状态和日期筛选，并更新为待处理、已处理或忽略；反馈可以关联知识片段并从反馈页跳转编辑。“路由测试”只运行意图识别与参数完整性检查，不进入 `ToolBridge`。“工具状态”同时显示接入状态、调用次数、成功率、平均耗时、最近调用和最近错误。运行监控保留最近 24 小时消息、工具失败、过期待确认操作和反馈汇总。

学生端会话侧栏默认收起，用户气泡位于右侧，助手气泡位于左侧；聊天框上方不显示横向滑动的快捷问题按钮。生成期间会话列表和会话操作被锁定，迟到的异步历史加载不会覆盖正在生成的消息。网络失败时原失败气泡保留错误说明；点击重试会移除该气泡并复用同一逻辑 requestId，不重复添加界面用户气泡或数据库用户消息。归档会话可在“已归档”列表恢复。附件可逐个移除，解析期间显示进度圈并禁止发送；带附件的首次模型调用若在输出前失败，服务端会透明重试一次。`【实时数据｜…】` 会转换为业务卡片，`【还需要一点信息】` 会转换为代办参数卡；自习室、教室和请假时间使用日期时间选择器，自习室预约始终提供房间编号。补参按钮保留原始写指令并附加字段标签，参数齐全后进入原写工具的确认流程。问答依据默认折叠。每条助手回复分别提供点赞、点踩和纠错。

AI 页面使用有鉴权的 `ai.ping` 每 12 秒探测一次服务端连接。连接失败会调用桌宠的独立连通性状态，松鼠显示持久的 `OFFLINE` 动作和“网络离线”气泡；恢复成功后短暂提示已重新连接。网络状态和角色是否拥有 AI 权限分别管理，离线不会错误隐藏桌宠。

知识版本依赖 `V12__ai_quality_workbench.sql`；反馈处理状态、关联知识和处理人依赖 `V16__ai_admin_workflow.sql`。服务端不会自动运行 SQL 迁移，升级已有库时需要在重启服务端前按版本顺序执行。

### 6.2 DeepSeek 与聊天附件

默认配置为：

```text
VCAMPUS_AI_ENDPOINT=https://api.deepseek.com/responses
VCAMPUS_AI_MODEL=deepseek-v4-flash
VCAMPUS_AI_VISION_MODEL=deepseek-v4-flash-vision-exp
DEEPSEEK_API_KEY=<server-only secret>
```

`VCAMPUS_AI_API_KEY` 仍作为供应商无关的优先密钥名保留。图片附件支持 PNG、JPEG、GIF、WebP，单张不超过 2 MB，并校验文件签名；文本和常见代码文件不超过 256 KB。PDF、DOCX、PPTX、XLS/XLSX 单个原文件不超过 10 MB，客户端使用 PDFBox/Apache POI 提取最多 60,000 字符，再按 UTF-8 纯文本上送；扫描版 PDF 没有文字层时会提示改传图片。附件最多 3 个，只用于当前聊天请求，不写入聊天消息表。

DeepSeek Responses API 不直接接收通用非图片文件输入，因此这里不把办公文档伪装为模型文件上传。图片请求会附带真实文件名和视觉输入，并要求模型依据实际画面回答；文档请求则依据本地提取的文字回答。服务端提示词和流式输出过滤器共同移除 Markdown 星号、方框、反引号和 `[1]` 形式的数字引用，业务 DTO 使用专用日期时间格式化，避免把 `chronology` 等 Java 内部字段展示给用户。

建议每个知识片段包含：

| 字段 | 建议内容 |
|---|---|
| `source_type` | 使用 `SYSTEM_RULE` 或项目约定的正式制度类型 |
| 标题 | 制度全称、章节和条款编号 |
| 正文 | 适用对象、具体要求、例外情况和生效范围 |
| 来源 | 在正文中记录官方发布部门、文件名或官方链接 |
| 状态 | 核对无误后设为 `ACTIVE`；失效制度及时停用 |

回答约束：

- 只能依据检索到的正式知识片段说明校规；
- 回答中用知识标题自然说明依据，不输出 `[编号]` 标记；
- 没有对应条款时必须说明“当前知识库未收录该规定”；
- 不得凭常识编造处分标准、申请期限、负责部门或条款编号；
- 对存在争议或可能已经修订的规定，建议用户咨询学校主管部门。

## 7. 已注册业务工具

当前共注册 47 个工具，其中 27 个只读工具、20 个需要确认的写工具。每个工具向意图模型公开字段、类型和必填约束；缺少参数时由服务端返回澄清问题，不创建待确认动作。

意图路由把“预约明天下午的自习室”等动作词与业务对象分开识别，不再要求关键词连续出现。明确要求写操作时不会降级为查询；规则解析优先从原始指令及参数卡的字段标签中恢复参数，模型只在白名单工具和白名单字段内辅助泛化识别，仍缺少必要信息时只询问缺项。若系统中只有一个开放自习室，可自动补上该房间编号，最终执行前仍展示摘要并要求用户确认。

### 7.1 账号、学籍与教务

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `identity.profile.read` | 只读 | 查询本人账号资料 |
| `student.profile.read` | 只读 | 查询本人学籍 |
| `student.grades.read` | 只读 | 查询本人成绩 |
| `academic.schedule.read` | 只读 | 查询本人课表 |
| `academic.enrollments.read` | 只读 | 查询本人已选课程 |
| `academic.course.search` | 只读 | 查询系统中可见课程 |
| `academic.course.enroll` | 写入、需确认 | 按课程编号选课 |
| `academic.course.drop` | 写入、需确认 | 按课程编号退课 |

### 7.2 图书馆

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `library.book.search` | 只读 | 按关键词检索图书 |
| `library.borrow.mine` | 只读 | 查询本人借阅记录 |
| `library.book.borrow` | 写入、需确认 | 按图书编号借书 |
| `library.book.return` | 写入、需确认 | 按借阅记录编号还书 |
| `library.study-room.search` | 只读 | 查询自习室 |
| `library.study-room.mine` | 只读 | 查询本人自习室预约 |
| `library.study-room.reserve` | 写入、需确认 | 预约自习室 |
| `library.study-room.cancel` | 写入、需确认 | 取消本人预约 |
| `library.resource.search` | 只读 | 查询在线资源 |

### 7.3 校园商店

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `store.product.search` | 只读 | 按关键词检索商品 |
| `store.account.read` | 只读 | 查询本人账户余额 |
| `store.ledger.read` | 只读 | 查询本人账户流水 |
| `store.cart.read` | 只读 | 查询本人购物车 |
| `store.orders.mine` | 只读 | 查询本人订单 |
| `store.cart.add` | 写入、需确认 | 将指定数量商品加入购物车 |
| `store.cart.update` | 写入、需确认 | 修改购物车商品数量 |
| `store.cart.remove` | 写入、需确认 | 移除购物车商品 |
| `store.order.create` | 写入、需确认 | 从购物车创建订单 |
| `store.order.pay` | 写入、需确认 | 余额支付订单 |
| `store.coupon.claim` | 写入、需确认 | 领取优惠券 |

创建订单和支付是两个独立确认动作。支付由 AI 适配器生成幂等键，商店服务仍负责余额、库存和订单状态校验。

### 7.4 宿舍服务

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `dorm.accommodation.read` | 只读 | 查询本人住宿信息 |
| `dorm.utility.read` | 只读 | 查询本人水电分摊 |
| `dorm.repair.mine` | 只读 | 查询本人报修 |
| `dorm.repair.create` | 写入、需确认 | 提交宿舍报修 |
| `dorm.leave.mine` | 只读 | 查询本人请假 |
| `dorm.leave.submit` | 写入、需确认 | 提交请假 |
| `dorm.leave.cancel` | 写入、需确认 | 取消本人待审核请假 |
| `dorm.utility.pay` | 写入、需确认 | 支付本人水电分摊 |
| `dorm.announcement.read` | 只读 | 查询宿舍公告 |

### 7.5 校园扩展服务

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `campus.announcement.read` | 只读 | 查询校园公告 |
| `campus.competition.search` | 只读 | 查询校园竞赛 |
| `campus.competition.mine` | 只读 | 查询本人已报名竞赛（AI 只读联合视图） |
| `campus.competition.register` | 写入、需确认 | 报名竞赛 |
| `campus.competition.cancel` | 写入、需确认 | 取消竞赛报名 |
| `campus.srtp.mine` | 只读 | 查询本人 SRTP 项目 |
| `campus.classroom.search` | 只读 | 查询可申请教室 |
| `campus.classroom.mine` | 只读 | 查询本人教室申请 |
| `campus.classroom.apply` | 写入、需确认 | 提交教室申请 |
| `campus.classroom.cancel` | 写入、需确认 | 取消本人教室申请 |

## 8. 权限与安全边界

### 8.1 当前会话是唯一身份来源

AI 工具使用当前 `SessionContext` 和活动角色。问题文本、模型输出和工具参数不能指定或覆盖当前用户身份。

### 8.2 原业务模块是最终校验边界

AI 的工具映射不代表用户一定有权执行。`CommandRouter` 和原业务服务仍会检查：

- 活动角色和权限；
- 数据是否属于当前用户；
- 对象当前状态；
- 课程容量和时间冲突；
- 图书库存和借阅状态；
- 商品库存、购物车和订单状态；
- 竞赛报名窗口与容量；
- 数据库事务和并发冲突。

### 8.3 写操作确认

所有 `isWriteOperation=true` 的工具统一进入 `PendingAction`，不能由模型绕过。确认动作只允许原申请用户处理，并具有有效期。

### 8.4 敏感信息

- API Key 只从服务端环境变量读取；
- API Key 不进入客户端、数据库、日志或仓库；
- 对话结果过滤密码、token 和 secret 类型 getter；
- AI 不返回密码哈希或原始会话 token；
- 工具参数和结果摘要需要保留审计，但不得包含敏感凭据。

## 9. 数据库边界

AI 模块使用六张自有存储表：

| 表 | 用途 |
|---|---|
| `ai_chat_sessions` | 对话会话 |
| `ai_chat_messages` | 用户、助手、系统和工具消息 |
| `ai_knowledge_chunks` | 系统指南、校规和校园知识片段 |
| `ai_tool_call_logs` | 工具参数、申请人、确认人、状态和结果摘要 |
| `ai_knowledge_versions` | 知识片段不可覆盖的版本快照 |
| `ai_answer_feedback` | 脱敏回答反馈、处理状态和关联知识 |

为补足原竞赛 DTO 未提供的“本人已报名竞赛”列表，AI 模块另有一个只读联合查询，按当前会话用户连接 `competition_registrations` 与 `competitions`。它不写入、不复制业务数据，且仍通过命令路由和 `COMPETITION_ENROLL` 权限校验。

课程、成绩、借阅、商品、订单、宿舍等业务事实仍保存在原业务表中。AI 不维护它们的副本。

新增操作步骤知识后，需要在 MySQL 客户端执行可重复的 AI 知识扩充脚本：

```sql
SOURCE E:/path/to/Vcampus/vcampus-server/src/main/resources/db/migration/V10__ai_assistant_knowledge.sql;
SOURCE E:/path/to/Vcampus/vcampus-server/src/main/resources/db/migration/V11__ai_knowledge_and_tools.sql;
SOURCE E:/path/to/Vcampus/vcampus-server/src/main/resources/db/migration/V12__ai_quality_workbench.sql;
SOURCE E:/path/to/Vcampus/vcampus-server/src/main/resources/db/migration/V16__ai_admin_workflow.sql;
```

不要把 `SOURCE` 命令直接输入 PowerShell；它应在 MySQL 客户端中执行。

## 10. 模型 API 配置

服务端默认调用 DeepSeek Responses API。启动服务端前，在同一个 PowerShell 窗口设置：

```powershell
$apiCredential = Get-Credential -UserName api-key -Message '请输入 AI Responses API Key'
if ($null -eq $apiCredential) {
    throw '未输入 AI API Key'
}

$env:DEEPSEEK_API_KEY = $apiCredential.GetNetworkCredential().Password
$env:VCAMPUS_AI_ENDPOINT = 'https://api.deepseek.com/responses'
$env:VCAMPUS_AI_MODEL = 'deepseek-v4-flash'
$env:VCAMPUS_AI_VISION_MODEL = 'deepseek-v4-flash-vision-exp'
```

配置项：

| JVM 属性 | 环境变量 | 默认值 |
|---|---|---|
| `vcampus.ai.endpoint` | `VCAMPUS_AI_ENDPOINT` | `https://api.deepseek.com/responses` |
| 无 | `VCAMPUS_AI_API_KEY` / `DEEPSEEK_API_KEY` | 无，前者优先；未设置时使用知识库降级 |
| `vcampus.ai.model` | `VCAMPUS_AI_MODEL` | `deepseek-v4-flash` |
| `vcampus.ai.vision-model` | `VCAMPUS_AI_VISION_MODEL` | `deepseek-v4-flash-vision-exp` |
| `vcampus.ai.connect-timeout` | 无 | `10000` 毫秒 |
| `vcampus.ai.read-timeout` | 无 | `120000` 毫秒 |
| `vcampus.ai.client-read-timeout` | 无 | `120000` 毫秒 |

远程 endpoint 必须使用 HTTPS，并支持 Responses API 的 SSE `response.output_text.delta` 事件。只有视觉模型接收图片输入；普通文本请求继续使用主模型。

## 11. 启动与演示

完成数据库和 API 环境变量配置后，在服务端终端运行：

```powershell
cd D:\Vcampus\Vcampus-main
.\scripts\start-server.ps1
```

再打开另一个 PowerShell：

```powershell
cd D:\Vcampus\Vcampus-main
.\scripts\start-client.ps1
```

演示账号：

| 账号 | 密码 | 用途 |
|---|---|---|
| `demo_student` | `student123` | 对话、知识问答、业务查询和确认代办 |
| `demo_ai` | `ai123` | 知识片段维护和 AI 运行监控 |

推荐演示问题：

```text
怎么在课表里添加课程？
查询我的成绩
查询我的课表
搜索图书 软件工程
查询图书馆在线资源
查询我的余额
查询我的报修进度
查询有哪些竞赛
选课 1001
```

输入框按 Enter 发送，按 Shift+Enter 换行。写操作请在代办模式使用；只有对象真实存在、用户确认且原业务模块校验通过后才会成功。

## 12. API 不可用时的行为

出现 API Key 未设置、网络超时、限流、服务商错误或流式解析失败时：

- VCampus 服务端仍可启动；
- 已注册校园工具仍可正常调用；
- 知识问答返回本地知识库降级结果；
- 降级回答包含知识标题，便于用户判断依据；
- 不会因为模型失败跳过原业务权限或确认流程。

如果知识库也没有相关片段，系统会明确说明没有检索到直接内容，而不是生成无依据校规。

## 13. 测试与质量门禁

AI 专项测试覆盖：

- 混合知识排序和无关候选过滤；
- 系统操作问题优先召回对应指南；
- 说明性问题不触发写工具；
- 47 个默认工具的模块覆盖、读写标记、结构化参数和缺参澄清；
- 对话参数到既有业务 DTO 的转换；
- 嵌套分页 DTO 的对话化格式；
- Responses API 配置和 SSE 增量解析。
- 带标点的图书和商品通用问法路由；
- 工具路由测试不进入业务执行桥接；
- 同一逻辑请求重试复用 requestId、只保留一个用户气泡，并锁定会话切换；
- 桌宠状态超时回到待机、AI 请求状态联动和取消隔离；
- 单击/拖动阈值、跨屏位置校正、透明 PNG 解码与离屏缩放渲染。

项目根目录执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pquality verify
```

当前完整 Maven 测试共运行 476 项，0 失败、0 错误；其中 12 项需要真实 MySQL 或环境配置，默认跳过。新增覆盖字段级查询结果、模式路由和代办补参续办；真实数据库测试通过 `vcampus.mysql.integration=true` 显式启用。

## 14. 接入新的业务工具

其他业务模块增加功能时，按以下顺序接入 AI：

1. 在 `vcampus-common` 定义稳定命令字和可序列化 DTO；
2. 业务模块在服务端 `CommandRouter` 注册处理器；
3. 原业务服务独立完成权限、数据范围、状态和事务校验；
4. 在 `AiToolRegistry` 增加“AI 工具名 → 原业务命令”映射；
5. 在 `CampusCommandTool.Kind` 增加必要的 DTO 参数适配；
6. 在 `ToolIntentParser` 增加明确、可测试的中文意图；
7. 写工具必须标记为写入操作；
8. 增加成功、无权限、模块未注册、取消、超时和重复确认测试；
9. 在本文和测试手册中更新工具清单与演示语句。

禁止以下接法：

- AI 直接调用其他模块 DAO；
- AI 自己复制一份课程、图书、订单或宿舍表；
- 信任模型提供的 userId、角色或审核人；
- 为方便演示而绕过 `CommandRouter` 权限；
- 让模型自动确认写操作；
- 将业务执行失败包装成成功回答。

## 15. 后续可扩展方向

1. 增加制度版本、生效日期和官方来源字段；
2. 为扫描版 PDF 和图片文档增加可审核 OCR 导入；
3. 知识规模扩大后接入专业向量索引或嵌入服务；
4. 在现有回归结果上增加用例持久化、导入导出和历史趋势；
5. 根据业务模块交付情况继续扩展复杂参数工具；
6. 增加真实 MySQL 环境下的 AI → CommandRouter → 业务表端到端集成测试。

扩展时必须继续保持：AI 负责编排和交互，原业务模块负责业务事实、权限和事务。
