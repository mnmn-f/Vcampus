# 页面与人员职责地图

页面先按登录后的当前职责分流，再进入该职责可用的校园业务。本文区分“页面入口”和“业务闭环”：入口由 Swing 客户端控制，数据范围、权限、状态机和事务仍由服务端校验。无数据库的 Demo 预览只用于查看布局和角色差异，不代表真实业务数据。

## 统一外壳

- 登录与匿名注册：[LoginFrame.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/LoginFrame.java)、[LoginBrandPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/LoginBrandPanel.java)、[RegistrationPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/RegistrationPanel.java)。左侧品牌区由正面趴扶挥手的小松鼠和放大的 `VCampus` 字样组成；登录页仍只采集校园账号、学号或工号和密码，原注册入口及注册流程不变。服务端查询账号、学号和工号，匹配唯一用户后返回姓名、用户编号和有效职责。
- 登录后外壳：[AppShell.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/AppShell.java)、[WorkspaceController.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/controller/WorkspaceController.java)。外壳包含品牌侧栏、顶部身份栏和内容区。
- 角色文案与快捷任务：[RoleWorkspace.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/RoleWorkspace.java)。主页标题、侧栏名称和快捷卡片随 `activeRole` 改变。
- 侧栏与顶部身份：[SidebarPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/SidebarPanel.java)、[TopBarPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/TopBarPanel.java)。侧栏只保留当前职责有权进入的模块；多角色账号才显示身份切换器。
- 个人中心：[PersonalCenterPage.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/PersonalCenterPage.java)。所有登录人员可查看本人资料和账号安全；本人注销申请在非系统管理员的个人中心内办理。
- 网络组合根：[ClientBusinessServices.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/composition/ClientBusinessServices.java)。登录成功后的业务页共用同一 `NetworkClientService`、`ClientSession` 和当前身份。
- AI 桌宠：[SquirrelPetController.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/pet/SquirrelPetController.java)、[SquirrelPetWidget.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/pet/SquirrelPetWidget.java)。仅对可进入校园助手的职责显示；窗口内和最小化后的桌面形态均可拖动并分别保存位置，窗口内单击进入 AI 页面，桌面形态单击恢复原页面。

主导航保留固定侧栏。进入模块后，查询、维护、审批、日志等二级任务统一使用顶部 [TaskTabs.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/ui/components/TaskTabs.java)；图书馆也通过同一组件呈现，不再维护独立的模块内左栏。

## 按职责的真实页面入口

| 当前职责 | 工作台标题 | 主导航可见入口 | 该职责的重点任务 |
|---|---|---|---|
| 学生 | 学生主页 | 我的学籍、选课与课表、图书馆、校园商店、宿舍生活 | 本人学籍/成绩导出、选退课与课表、图书服务、商店交易、住宿/请假/访客/报修许可与评价/在宿门禁/账单 |
| 任课教师 | 教师主页 | 我的教学、图书馆 | 本人课程与成绩登记、教室申请/进度，以及图书馆只读能力 |
| 学籍管理员 | 学籍工作台 | 学籍管理 | 学生档案查询、新建与修改，成绩档案核对 |
| 教务老师 | 教务工作台 | 教务管理 | 课程维护、选中课程后的排课时段维护、教务公告/竞赛/SRTP、教室申请审批 |
| 图书管理员 | 图书馆工作台 | 馆务管理 | 书目与库存、借还、自习室、线上资源与访问日志、借阅台账筛选和 CSV、图书馆公告 |
| 商店管理员 | 商店工作台 | 商店运营 | 商品图片、分类和库存、促销、订单处理、销售统计与趋势图 |
| 宿管员 | 宿管工作台 | 宿舍管理 | 空间与住宿、申请/请假/访客审批、门禁/连续未归/卫生、报修、抄表出账、分范围公告和调度状态 |
| AI 知识管理员 | 知识服务工作台 | 校园助手、安全与运行 | 分页维护和事务导入知识、单题测试与批量回归、反馈处理、工具路由测试、调用状态和运行监控；未配置模型时使用本地知识检索 |
| 系统管理员 | 系统管理工作台 | 账号与角色、安全与运行 | 用户/状态/角色/会话/注销申请、登录与业务审计、系统监控；不显示其他业务模块写入口 |

`ModuleId.isVisibleTo(role)` 控制主导航入口；`WorkspaceController` 在打开页面前再次检查。上表描述的是人员分流，不替代服务端的 `activeRole`、`userId`、细粒度权限和数据范围校验。

## 页面与服务边界

| 页面组件 | 真实客户端服务 | 主要范围 |
|---|---|---|
| `AcademicCoursesPanel`、`CourseEditorPanel`、`CourseScheduleEditorPanel`、`StudentSchedulePanel`、`TeacherGradePanel` | `AcademicClientService`、`StudentRecordClientService` | 课程、排课和选退课；排课时段在课程页内行编辑，冲突与权限由服务端处理 |
| `CampusAnnouncementsPanel`、`CampusCompetitionsPanel`、`CampusSrtpPanel`、`CampusClassroomsPanel` | `CampusClientService` | 公告、竞赛、SRTP 和教室申请/审批 |
| `LibraryBooksPanel`、`LibraryBorrowingsPanel`、`LibraryBorrowingLedgerPanel`、`LibraryRoomsPanel`、`LibraryResourcesPanel`、`LibraryResourceAccessLogsPanel` | `LibraryClientService` | 图书、本人借还、预约、线上资源访问/日志、管理员借阅台账和 CSV；单次导出最多 5000 条 |
| `StoreProductsPanel`、`StoreCartPanel`、`StoreOrdersPanel`、`StoreCouponPanel`、`StoreReviewPanel`、`StoreFriendPaymentPanel`、`StoreSalesPanel`、`StoreSalesTrendPanel` | `StoreClientService` | 商品图片/分类、购物车、服务端结算、订单、优惠券、评价、好友代付和销售趋势；价格、库存、余额、身份和幂等由服务端裁决 |
| `DormAccommodationPanel`、`DormStudentLeavePanel`、`DormStudentRepairsPanel`、`DormExtVisitorPanel`、`DormExtStayPanel`、`DormExtNoticePanel` | `DormClientService`、`DormExtClientService` | 学生本人住宿、请假、访客、报修许可/评价、在宿门禁、账单和公告；用户身份由会话确定 |
| `DormManagerSpacePanel`、`DormManagerLeavePanel`、`DormManagerRequestsPanel`、`DormExtVisitorAuditPanel`、`DormExtWarningPanel`、`DormExtHygienePanel`、`DormExtBillingPanel`、`DormExtNoticePanel` | `DormClientService`、`DormExtClientService` | 宿管空间与审批、门禁/连续未归、卫生、报修、抄表出账和分范围公告；两个服务共享同一网络组合根 |
| `IdentityUsersPanel`、`IdentitySessionsPanel`、`IdentityCancellationPanel`、`IdentityAuditPanel`、`IdentityMonitorPanel` | `IdentityClientService` | 用户、状态、角色、会话、注销申请、审计与系统快照；会话展示不返回 raw token |
| `AiAssistantPage`、`AiChatPanel`、`AiMessageCard`、`AiDateTimeField`、`SquirrelPetWidget` | `AiAssistantClientService` | 问答、聊天、代办、字段级实时结果、跨模式提示、补参续办、上传进度、同请求重试、会话归档恢复与流式响应；输入区无横向快捷问题按钮，桌宠只接收固定状态事件，不接触回答正文或业务凭据 |
| `AiKnowledgePanel`、`AiKnowledgeTestPanel`、`AiKnowledgeRegressionPanel`、`AiFeedbackPanel`、`AiToolRouteTestPanel`、`AiToolStatusPanel`、`AiMonitorPanel` | `AiAssistantClientService` | 知识分页与事务导入、标准答案/预期命中测试和批量回归、反馈闭环、只解析不执行的路由测试、调用指标与运行监控 |

## 高风险操作确认

真实页面在提交会改变状态、余额、库存或账号的操作前使用 `RealUi.confirm`；查询、新增、普通编辑和成功提示不弹模态框。最终确认不是安全边界，服务端仍负责权限、状态机、事务和并发校验。

- 账号注销提交/撤回/审批、强制会话下线、密码重置和角色撤销：`IdentityCancellationPanel`、`IdentitySessionsPanel`、`IdentityUsersPanel`。
- 退选、比赛报名取消、教室申请撤销：`AcademicCoursesPanel`、`CampusCompetitionsPanel`、`CampusClassroomsPanel`。
- 图书归还、自习室预约取消、借阅台账 CSV 覆盖：`LibraryBorrowingsPanel`、`LibraryRoomsPanel`、`LibraryBorrowingLedgerPanel`。
- 订单支付、危险状态处理、账户充值：`StoreOrdersPanel`、`StoreAccountPanel`。
- 退宿、请假撤回/审批、报修取消、水电缴费：`DormAccommodationPanel`、`DormStudentLeavePanel`、`DormManagerLeavePanel`、`DormUtilityBillsTable`。

## 公告兼容边界

教务和图书馆使用 `CampusAnnouncementsPanel`；宿舍公告写入 `announcements` 的 DORM 模块行，并由 `DormExtNoticePanel` 读取 `dorm_notice_extras` 中的类型、楼栋/房间范围和置顶信息。宿舍页面不并列显示旧公告面板。
