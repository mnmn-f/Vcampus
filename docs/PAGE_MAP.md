# 页面与人员职责地图

页面先按登录后的当前职责分流，再进入该职责可用的校园业务。本文区分“页面入口”和“业务闭环”：入口由 Swing 客户端控制，数据范围、权限、状态机和事务仍由服务端校验。无数据库的 Demo 预览只用于查看布局和角色差异，不代表真实业务数据。

## 统一外壳

- 登录与匿名注册：[LoginFrame.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/LoginFrame.java)、[RegistrationPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/RegistrationPanel.java)。登录页只采集校园账号、学号或工号和密码；服务端查询账号、学号和工号，匹配唯一用户后返回姓名、用户编号和有效职责。
- 登录后外壳：[AppShell.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/AppShell.java)、[WorkspaceController.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/controller/WorkspaceController.java)。外壳包含品牌侧栏、顶部身份栏和内容区。
- 角色文案与快捷任务：[RoleWorkspace.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/RoleWorkspace.java)。主页标题、侧栏名称和快捷卡片随 `activeRole` 改变。
- 侧栏与顶部身份：[SidebarPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/SidebarPanel.java)、[TopBarPanel.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/TopBarPanel.java)。侧栏只保留当前职责有权进入的模块；多角色账号才显示身份切换器。
- 个人中心：[PersonalCenterPage.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/view/PersonalCenterPage.java)。所有登录人员可查看本人资料和账号安全；本人注销申请在非系统管理员的个人中心内办理。
- 网络组合根：[ClientBusinessServices.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/composition/ClientBusinessServices.java)。登录成功后的业务页共用同一 `NetworkClientService`、`ClientSession` 和当前身份。

主导航分为“我的工作台”“校园业务”“系统管理”三组。模块内的查询、维护、审批、日志等任务使用 [TaskTabs.java](../vcampus-client/src/main/java/edu/seu/vcampus/client/ui/components/TaskTabs.java) 切换，避免把互不相同的人员任务堆在同一长页面中。

## 按职责的真实页面入口

| 当前职责 | 工作台标题 | 主导航可见入口 | 该职责的重点任务 |
|---|---|---|---|
| 学生 | 学生主页 | 我的学籍、选课与课表、图书馆、校园商店、宿舍生活 | 本人学籍/分学期成绩指标与导出、选退课与课程详情、图书借阅/自习室/资源、商品/购物车/优惠券/评价/好友代付、住宿/请假/报修/账单 |
| 任课教师 | 教师主页 | 我的教学、图书馆 | 本人课程与成绩登记、教室申请/进度，以及图书馆只读能力 |
| 学籍管理员 | 学籍工作台 | 学籍管理 | 学生档案查询、新建与修改，成绩档案核对 |
| 教务老师 | 教务工作台 | 教务管理 | 课程维护、选中课程后的排课时段维护、教务公告/竞赛/SRTP、教室申请审批 |
| 图书管理员 | 图书馆工作台 | 馆务管理 | 书目与库存、借还、自习室、线上资源与访问日志、借阅台账筛选和 CSV、图书馆公告 |
| 商店管理员 | 商店工作台 | 商店运营 | 商品图片、分类和库存、促销、订单处理、销售统计与趋势图 |
| 宿管员 | 宿管工作台 | 宿舍管理 | 楼栋/房间/床位、住宿关系、住宿/请假审批、门禁/未归/卫生、报修、账单、水电和宿舍公告 |
| AI 知识管理员 | 知识服务工作台 | 安全与运行 | 查看系统运行状态；校园助手未接入真实服务，当前不显示入口 |
| 系统管理员 | 系统管理工作台 | 账号与角色、安全与运行 | 用户/状态/角色/会话/注销申请、登录与业务审计、系统监控；不显示其他业务模块写入口 |

`ModuleId.isVisibleTo(role)` 控制主导航入口；`WorkspaceController` 在打开页面前再次检查。上表描述的是人员分流，不替代服务端的 `activeRole`、`userId`、细粒度权限和数据范围校验。

## 页面与服务边界

| 页面组件 | 真实客户端服务 | 主要范围 |
|---|---|---|
| `AcademicCoursesPanel`、`CourseEditorPanel`、`CourseScheduleEditorPanel`、`StudentSchedulePanel`、`TeacherGradePanel` | `AcademicClientService`、`StudentRecordClientService` | 课程、排课和选退课；排课时段在课程页内行编辑，冲突与权限由服务端处理 |
| `CampusAnnouncementsPanel`、`CampusCompetitionsPanel`、`CampusSrtpPanel`、`CampusClassroomsPanel` | `CampusClientService` | 公告、竞赛、SRTP 和教室申请/审批 |
| `LibraryBooksPanel`、`LibraryBorrowingsPanel`、`LibraryBorrowingLedgerPanel`、`LibraryRoomsPanel`、`LibraryResourcesPanel`、`LibraryResourceAccessLogsPanel` | `LibraryClientService` | 图书、本人借还、预约、线上资源访问/日志、管理员借阅台账和 CSV；单次导出最多 5000 条 |
| `StoreProductsPanel`、`StoreCartPanel`、`StoreOrdersPanel`、`StoreCouponPanel`、`StoreReviewPanel`、`StoreFriendPaymentPanel`、`StoreSalesPanel`、`StoreSalesTrendPanel` | `StoreClientService` | 商品图片/分类、购物车、服务端结算、订单、优惠券、评价、好友代付和销售趋势；价格、库存、余额、身份和幂等由服务端裁决 |
| `DormAccommodationPanel`、`DormStudentLeavePanel`、`DormStudentRepairsPanel`、`DormStudentRepairEvaluationPanel`、`DormStudentBillsPanel` | `DormClientService` | 学生本人住宿、请假、报修评价和账单；请求中的用户身份由会话确定 |
| `DormManagerSpacePanel`、`DormManagerLeavePanel`、`DormManagerRequestsPanel`、`DormManagerGovernancePanel`、`DormManagerRepairsPanel`、`DormManagerBillingPanel`、`DormAnnouncementsPanel` | `DormClientService` | 宿管空间维护、住宿/请假审批、巡查、报修、账单和公告；筛选条件不扩大授权范围 |
| `IdentityUsersPanel`、`IdentitySessionsPanel`、`IdentityCancellationPanel`、`IdentityAuditPanel`、`IdentityMonitorPanel` | `IdentityClientService` | 用户、状态、角色、会话、注销申请、审计与系统快照；会话展示不返回 raw token |

## 高风险操作确认

真实页面在提交会改变状态、余额、库存或账号的操作前使用 `RealUi.confirm`；查询、新增、普通编辑和成功提示不弹模态框。最终确认不是安全边界，服务端仍负责权限、状态机、事务和并发校验。

- 账号注销提交/撤回/审批、强制会话下线、密码重置和角色撤销：`IdentityCancellationPanel`、`IdentitySessionsPanel`、`IdentityUsersPanel`。
- 退选、比赛报名取消、教室申请撤销：`AcademicCoursesPanel`、`CampusCompetitionsPanel`、`CampusClassroomsPanel`。
- 图书归还、自习室预约取消、借阅台账 CSV 覆盖：`LibraryBorrowingsPanel`、`LibraryRoomsPanel`、`LibraryBorrowingLedgerPanel`。
- 订单支付、危险状态处理、账户充值：`StoreOrdersPanel`、`StoreAccountPanel`。
- 退宿、请假撤回/审批、报修取消、水电缴费：`DormAccommodationPanel`、`DormStudentLeavePanel`、`DormManagerLeavePanel`、`DormUtilityBillsTable`。

## 公告兼容边界

教务和图书馆使用 `CampusAnnouncementsPanel` + `CampusAnnouncementService`，按 `module_code`、角色可见范围和生效时间读取 `announcements`。宿舍使用 `DormAnnouncementsPanel` + `DormAnnouncementService`，同样写入 DORM 模块行，以保留宿舍专用 DTO/API；两者暂未合并为一个客户端类，后续可增加适配层而不改表结构。
