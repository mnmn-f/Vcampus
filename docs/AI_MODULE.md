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
| 多轮对话与历史会话 | 已实现 |
| 流式回答与停止生成 | 已实现 |
| Responses API 模型调用 | 已实现 |
| API 不可用时知识库降级 | 已实现 |
| 系统操作步骤问答 | 已实现 |
| 校纪校规证据约束 | 已实现，正式制度内容需要管理员录入 |
| 关键词与轻量文本向量混合检索 | 已实现 |
| 业务模块查询工具 | 已覆盖常用本人业务 |
| 业务模块写工具 | 已实现 8 个，全部要求二次确认 |
| 知识片段管理 | 已实现 |
| 会话、知识和工具运行监控 | 已实现 |
| 独立向量数据库 | 未使用 |
| PDF、Word、OCR 自动导入 | 未实现 |
| 后台自动向量索引任务 | 未实现 |

## 3. 总体架构

AI 请求仍使用 VCampus 原有的客户端、TCP 协议、登录会话和服务端路由。

```text
AiChatPanel
    ↓ 专用流式 Socket
AiCommands / AiQuery
    ↓
AiAssistantService
    ├─ ToolIntentParser ──→ AiToolRegistry ──→ ToolBridge
    │                                            ↓
    │                                      CommandRouter
    │                                            ↓
    │                                  原业务 Handler / Service / DAO
    │
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
| `ToolIntentParser` | 使用可测试的确定性规则识别校园业务意图 |
| `AiToolRegistry` | 保存 AI 工具名、原业务命令、读写类型和参数适配方式 |
| `CampusCommandTool` | 把对话中提取的参数转换为 `vcampus-common` 中已有 DTO |
| `ToolBridge` | 使用当前会话 token 将工具请求重新交给 `CommandRouter` |
| `ToolResultFormatter` | 把业务 DTO、分页数据和列表转换为适合对话展示的中文摘要 |
| `AiKnowledgeRepository` | 读取、维护并检索 MySQL 中的知识片段 |
| `KnowledgeRanker` | 对候选知识进行关键词和轻量文本向量混合排序 |
| `AiPromptBuilder` | 将用户问题、知识证据和回答约束组合为模型输入 |
| `ResponsesAiModel` | 调用兼容 Responses API 的 HTTPS 接口并解析 SSE 增量 |

## 4. 一次提问的处理流程

### 4.1 查询型业务问题

以“查询我的成绩”为例：

1. 客户端发送 `ai.query`；
2. `ToolIntentParser` 识别为 `student.grades.read`；
3. `AiToolRegistry` 将它映射到 `StudentCommands.SELF_GRADES`；
4. `ToolBridge` 携带当前用户会话进入原 `CommandRouter`；
5. 学籍模块按当前用户和活动角色再次鉴权；
6. 原服务查询数据库并返回业务 DTO；
7. `ToolResultFormatter` 将结果整理为对话文本；
8. AI 保存消息和工具日志，并流式返回客户端。

这类结果来自真实业务服务，不由模型生成，也不接受问题中伪造的用户编号。

### 4.2 写操作问题

以“选课 1001”为例：

1. 意图解析器识别 `academic.course.enroll` 和课程编号；
2. AI 只创建一个有效期为 5 分钟的待确认动作；
3. 客户端显示确认信息，此时业务数据尚未改变；
4. 用户确认后，AI 才通过 `CommandRouter` 调用原选课命令；
5. 教务模块重新检查权限、课程状态、容量、重复选课和时间冲突；
6. 成功或失败结果写入工具日志并返回对话。

取消、超时、重复确认或原业务模块拒绝时，不得伪报成功。

### 4.3 知识问答

如果没有命中校园工具，请求进入知识问答流程：

1. 从 `ai_knowledge_chunks` 读取最多 500 条 `ACTIVE` 候选；
2. 对问题进行校园同义词扩展；
3. 计算标题/正文关键词分值；
4. 计算字符词项向量的余弦相似度；
5. 过滤仅有无意义单字重合的候选；
6. 选取前 5 条知识片段；
7. 将来源类型、标题和有界正文交给模型；
8. 要求模型引用 `[编号]` 回答；
9. 模型不可用时，直接返回带标题的知识库降级结果。

当前方案属于轻量混合 RAG。它不需要独立向量数据库或嵌入模型，适合当前 Java 17、MySQL 和中小规模校园知识库。如果后续知识量明显增长，可以保持 `AiKnowledgeService` 上层接口不变，只替换底层检索实现。

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

项目没有凭空编写学校正式制度。校纪校规应由 `AI_KNOWLEDGE_ADMIN` 从学校官方文件录入知识库。

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
- 回答中使用 `[编号]` 指向本次知识依据；
- 没有对应条款时必须说明“当前知识库未收录该规定”；
- 不得凭常识编造处分标准、申请期限、负责部门或条款编号；
- 对存在争议或可能已经修订的规定，建议用户咨询学校主管部门。

## 7. 已注册业务工具

当前共注册 31 个工具，其中 23 个只读工具、8 个需要确认的写工具。

### 7.1 账号、学籍与教务

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `identity.profile.read` | 只读 | 查询本人账号资料 |
| `student.profile.read` | 只读 | 查询本人学籍 |
| `student.grades.read` | 只读 | 查询本人成绩 |
| `academic.schedule.read` | 只读 | 查询本人课表 |
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
| `store.order.create` | 写入、需确认 | 从购物车创建订单 |

创建订单不等于支付。支付会涉及余额、库存和幂等处理，目前仍应在原商店页面完成。

### 7.4 宿舍服务

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `dorm.accommodation.read` | 只读 | 查询本人住宿信息 |
| `dorm.utility.read` | 只读 | 查询本人水电分摊 |
| `dorm.repair.mine` | 只读 | 查询本人报修 |
| `dorm.leave.mine` | 只读 | 查询本人请假 |
| `dorm.announcement.read` | 只读 | 查询宿舍公告 |

### 7.5 校园扩展服务

| 工具 | 类型 | 原业务功能 |
|---|---|---|
| `campus.announcement.read` | 只读 | 查询校园公告 |
| `campus.competition.search` | 只读 | 查询校园竞赛 |
| `campus.competition.register` | 写入、需确认 | 报名竞赛 |
| `campus.competition.cancel` | 写入、需确认 | 取消竞赛报名 |
| `campus.srtp.mine` | 只读 | 查询本人 SRTP 项目 |
| `campus.classroom.mine` | 只读 | 查询本人教室申请 |

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

AI 模块只使用四张自有表：

| 表 | 用途 |
|---|---|
| `ai_chat_sessions` | 对话会话 |
| `ai_chat_messages` | 用户、助手、系统和工具消息 |
| `ai_knowledge_chunks` | 系统指南、校规和校园知识片段 |
| `ai_tool_call_logs` | 工具参数、申请人、确认人、状态和结果摘要 |

课程、成绩、借阅、商品、订单、宿舍等业务事实仍保存在原业务表中。AI 不维护它们的副本。

新增操作步骤知识后，需要在 MySQL 客户端重新执行可重复的 V2 数据脚本：

```sql
SOURCE D:/Vcampus/Vcampus-main/vcampus-server/src/main/resources/db/migration/V2__demo_data.sql;
```

不要把 `SOURCE` 命令直接输入 PowerShell；它应在 MySQL 客户端中执行。

## 10. 模型 API 配置

服务端默认调用兼容 OpenAI Responses API 的 HTTPS 接口。启动服务端前，在同一个 PowerShell 窗口设置：

```powershell
$apiCredential = Get-Credential -UserName api-key -Message '请输入 AI Responses API Key'
if ($null -eq $apiCredential) {
    throw '未输入 AI API Key'
}

$env:VCAMPUS_AI_API_KEY = $apiCredential.GetNetworkCredential().Password
$env:VCAMPUS_AI_ENDPOINT = 'https://api.openai.com/v1/responses'
$env:VCAMPUS_AI_MODEL = 'gpt-4.1-mini'
```

配置项：

| JVM 属性 | 环境变量 | 默认值 |
|---|---|---|
| `vcampus.ai.endpoint` | `VCAMPUS_AI_ENDPOINT` | `https://api.openai.com/v1/responses` |
| 无 | `VCAMPUS_AI_API_KEY` | 无，未设置时使用知识库降级 |
| `vcampus.ai.model` | `VCAMPUS_AI_MODEL` | `gpt-4.1-mini` |
| `vcampus.ai.connect-timeout` | 无 | `10000` 毫秒 |
| `vcampus.ai.read-timeout` | 无 | `120000` 毫秒 |
| `vcampus.ai.client-read-timeout` | 无 | `120000` 毫秒 |

远程 endpoint 必须使用 HTTPS，并支持 Responses API 的 SSE `response.output_text.delta` 事件。

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

最后一条会产生待确认动作，只有课程真实存在且原教务模块校验通过后才会选课成功。

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
- 31 个默认工具的模块覆盖和读写标记；
- 对话参数到既有业务 DTO 的转换；
- 嵌套分页 DTO 的对话化格式；
- Responses API 配置和 SSE 增量解析。

项目根目录执行：

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Pquality verify
```

当前合并基线为 357 个测试，0 失败、0 错误；默认跳过 12 个需要真实 MySQL 或环境配置的集成测试。真实数据库测试通过 `vcampus.mysql.integration=true` 显式启用。

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

1. 导入学校正式校纪校规和办事指南；
2. 增加制度版本、生效日期和官方来源字段；
3. 提供 Word、PDF 文档解析和分段审核流程；
4. 知识规模扩大后接入专业向量索引或嵌入服务；
5. 增加知识召回命中率、无答案率和用户反馈指标；
6. 根据业务模块交付情况继续增加报修提交、请假申请等复杂参数工具；
7. 为每个工具增加更结构化的参数说明和对话澄清流程；
8. 增加真实 MySQL 环境下的 AI → CommandRouter → 业务表端到端集成测试。

扩展时必须继续保持：AI 负责编排和交互，原业务模块负责业务事实、权限和事务。
