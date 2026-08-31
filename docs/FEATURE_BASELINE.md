# 功能基线与验收清单

本文件是答辩验收清单，不是愿望列表。只有“Common 命令/DTO → 服务端权限与业务规则 → InMemory/MySQL 持久化 → 网络客户端 → 真实页面 → 测试/证据”全部存在，才标记为完整闭环。逐项路径见 [REQUIREMENT_TRACEABILITY.md](REQUIREMENT_TRACEABILITY.md)。

状态标记：

- [x] **完整闭环**：可从真实页面走到服务端和数据库，并有测试或真实集成证据。
- [~] **服务/API完成等待UI**：接口和服务已存在，但真实页面仍缺少完整写入入口。
- [ ] **接口阶段/明确不实现**：当前只保留接口、模型或降级提示。

## 用户、身份与系统

| 验收项 | 状态 | 证据 |
|---|---|---|
| 注册、登录、登出、账号停用/恢复、修改密码 | [x] | LoginFrame、RegistrationPanel、AuthService、IdentityProfilePanel、IdentityServiceTest |
| 多角色切换、按 activeRole 导航、细粒度权限和越权拒绝 | [x] | RolePolicy、SessionContext、CommandRouter、WorkspaceController、RolePolicyTest、CrossModuleSecurityIntegrationTest |
| 用户分页查询、角色分配/撤销、会话查询/强制下线、登录/业务审计和系统快照 | [x] | RealIdentityAdminPage、IdentityUsersPanel、IdentitySessionsPanel、IdentityAuditPanel、IdentityMonitorPanel、MySqlIdentityIntegrationTest |
| 账号注销申请：本人提交/历史/撤回，SYSTEM_ADMIN 审批/驳回；批准禁用账号且提交后失效全部会话 | [x] | account_cancellation_requests、IdentityCancellationService、IdentityCancellationPanel、AccountCancellationServiceTest、MySqlAccountCancellationIntegrationTest |

## 学籍与成绩

| 验收项 | 状态 | 证据 |
|---|---|---|
| 学生查看本人学籍和成绩，身份不由 payload 的 userId 决定 | [x] | StudentOwnPanel、StudentRecordService、StudentProfileServiceTest、StudentCommandHandlerTest |
| 学籍管理员按条件查询、新建、修改和状态变更；学号唯一 | [x] | StudentRegistrarPanel、StudentProfileEditorPanel、MySqlStudentProfileRepository、StudentProfileServiceTest |
| 任课教师仅登记本人课程成绩；管理员核对；成绩范围、重复成绩和选课状态校验 | [x] | TeacherGradePanel、StudentGradeService、StudentGradeServiceTest |

## 教务与校园

| 验收项 | 状态 | 证据 |
|---|---|---|
| 课程分页查询、课程新增/修改、授课教师和教室关联 | [x] | AcademicCoursesPanel、CourseEditorPanel、AcademicCourseService、MySqlCourseWriteRepository、AcademicServiceTest |
| 课程时段新增/修改/删除 | [x] | AcademicScheduleService、MySqlScheduleRuleRepository、AcademicClientService、AcademicCoursesPanel、CourseScheduleEditorPanel；AcademicScheduleClientServiceTest、AcademicScheduleEditorPanelTest、ClientRoleCompositionTest |
| 学生选课/退课/个人课表、教师本人课程；容量、重复和时段冲突 | [x] | AcademicCoursesPanel、StudentSchedulePanel、AcademicEnrollmentService、AcademicServiceTest、CrossModuleCapacityIntegrationTest |
| 公告按模块、角色可见范围、生效/失效时间读取；教务发布/撤回 | [x] | CampusAnnouncementsPanel、CampusAnnouncementEditorPanel、CampusAnnouncementService、CampusServiceTest |
| 比赛发布、报名/取消、截止时间、容量和名单 | [x] | CampusCompetitionsPanel、CompetitionEditorPanel、CampusCompetitionService、CampusServiceTest |
| SRTP 学生本人查询、项目维护、参与人和状态管理 | [x] | CampusSrtpPanel、CampusSrtpEditorPanel、CampusSrtpService、CampusServiceTest |
| 教室查询、学生/教师申请、本人进度、教务审批/驳回/取消和占用冲突 | [x] | CampusClassroomsPanel、CampusClassroomApplyPanel、CampusClassroomService、CampusClassroomLockIntegrationTest |

## 图书馆

| 验收项 | 状态 | 证据 |
|---|---|---|
| 图书检索、详情、库存维护、借还和本人历史 | [x] | LibraryBooksPanel、LibraryBookEditorPanel、LibraryBorrowingsPanel、BookService、BorrowService、LibraryServiceTest |
| 图书管理员借阅台账：按状态/学生/关键字分页筛选并导出 CSV | [x] | BORROW_ADMIN_LIST、BorrowAdminSearchRequest、LibraryBorrowingLedgerPanel、LibraryClientService.adminBorrowings、CsvEncoder；单次导出最多 5000 条，覆盖已有文件须确认；LibraryBorrowLedgerCommandTest、NetworkLibraryBorrowLedgerTest、LibraryBorrowingLedgerPanelTest、CsvEncoderTest |
| 借还事务锁定图书/借阅记录，库存和状态一起提交 | [x] | MySqlBookRepository、MySqlBorrowRepository、LibraryServiceTest、MySqlDaoIntegrationTest |
| 自习室开放时段、预约、取消、重复和时间冲突 | [x] | LibraryRoomsPanel、LibraryRoomEditorPanel、StudyRoomService、LibraryServiceTest |
| 线上资源检索、启用/停用 | [x] | LibraryResourcesPanel、ResourceEditorPanel、OnlineResourceService、LibraryServiceTest |
| 线上资源访问记录和图书管理员分页日志 | [x] | RESOURCE_ACCESS / RESOURCE_ACCESS_LOGS；LibraryResourcesPanel 的“访问选中资源”写入服务端日志，LibraryResourceAccessLogsPanel 按资源/用户/时间分页查询；OnlineResourceAccessLogRepository、MySqlOnlineResourceAccessLogRepository、NetworkLibraryResourceAccessTest、LibraryResourceAccessPanelTest、LibraryResourceCommandTest |
| 图书馆公告使用校园通用公告模型 | [x] | RealLibraryPage、CampusAnnouncementsPanel（LIBRARY 模块）、CampusLibraryAnnouncementsTest |

## 校园商店

| 验收项 | 状态 | 证据 |
|---|---|---|
| 商品检索、上下架、价格/库存维护、购物车 | [x] | StoreProductsPanel、StoreProductEditorPanel、StoreCartPanel、StoreProductService、StoreCartService |
| 下单、订单历史/详情、取消和管理员状态处理 | [x] | StoreOrdersPanel、StoreOrderService、StoreOrderStatusService、StoreServiceTest |
| 账户余额、充值和流水；订单支付原子扣减余额/库存并写流水 | [x] | StoreAccountPanel、StorePaymentService、MySqlStoreAccountRepository、StoreServiceTest |
| 支付幂等、余额/库存不足和订单状态机 | [x] | StorePaymentService、StoreCommandHandlerTest、MySqlDaoIntegrationTest |
| 销售统计按日期/商品读取 PAID 或 COMPLETED 订单，不统计 REFUNDED | [x] | StoreSalesPanel、StoreSalesService、MySqlStoreSalesRepository、StoreSalesServiceTest、DatabaseCompatibilityTest |

## 宿舍服务

| 验收项 | 状态 | 证据 |
|---|---|---|
| 楼栋、房间、床位查询和空间维护；床位状态/房间容量校验 | [x] | DormManagerSpacePanel、DormSpaceEditorPanel、DormSpaceService、DormSpaceServiceTest |
| 学生本人住宿信息；宿管直接分配、调宿、退宿；床位唯一占用 | [x] | DormAccommodationPanel、DormManagerSpacePanel、DormAccommodationService、DormServiceTest、MySqlDaoIntegrationTest |
| 入住/调宿/退宿申请、本人进度和宿管审批 | [x] | DormAccommodationPanel、DormManagerRequestsPanel、DormServiceTest |
| 请假提交、本人历史/撤回、宿管审批和非法状态防护 | [x] | DormStudentLeavePanel、DormManagerLeavePanel、DormLeaveService、DormLeaveServiceTest |
| 门禁记录、未归判断、预警列表和处理备注 | [x] | DormManagerGovernancePanel、DormGovernanceService、DormServiceTest |
| 卫生检查、评分、问题记录和整改状态 | [x] | DormManagerGovernancePanel、DormGovernanceService、DormServiceTest |
| 报修申请、受理、处理中、完成、取消和本人一次性评价 | [x] | DormStudentRepairsPanel、DormStudentRepairEvaluationPanel、DormManagerRepairsPanel、DormRepairEvaluationTest |
| 宿舍公告、房间水电账单、学生分摊和本人缴费 | [x] | DormAnnouncementsPanel、DormStudentBillsPanel、DormManagerBillingPanel、DormBillingService、DormServiceTest |
| 门禁预警或账单的后台定时生成 | [ ] | 当前只有查询/处理和账单服务，未发现真实 scheduler；不把静态任务文字当成已完成 |

## 网络、通用界面与部署

| 验收项 | 状态 | 证据 |
|---|---|---|
| 统一 TCP 请求、结果码、token、Socket 断线/超时和服务端异常兜底 | [x] | TcpServer、CommandRouter、SocketClientGateway、TcpServerProtocolResilienceTest、SocketClientGatewayResilienceTest |
| SafeObjectInputStream 白名单和单条持久连接 8 MiB 累计预算 | [x] | SafeObjectInputStream、SafeObjectInputStreamTest、TcpProtocolSecurityIntegrationTest |
| 生产组合根使用同一 JdbcConnectionFactory、TransactionManager、SessionManager；真实登录→业务只读→登出→旧 token 拒绝 | [x] | ServerMain.createProductionRouter、ClientBusinessServices、ProductionRouterMySqlIntegrationTest |
| 列表页统一筛选、分页、加载、空状态、错误提示和内嵌编辑；危险操作确认 | [x] | AsyncPagedTable、RealUi、各 Real 页面及 ClientRoleCompositionTest |

## AI 助手（接口阶段）

| 验收项 | 状态 | 证据 |
|---|---|---|
| 会话、消息、流式片段、取消、只读工具/写操作确认的协议边界 | [ ] | AiAssistantGateway、AiQuery、AiStreamListener；尚无真实服务 |
| 未实现能力不出现在任何角色的导航、主页或业务按钮中 | [x] | RolePolicy、RoleWorkspace、ModulePagesTest、RoleAwareShellUiTest |
| 真实模型、RAG、外部知识库和写操作工具 | [ ] | 当前版本明确不实现；ai_* 表只是存储模型，不能作为模型接入证据 |

## 明确延期或不纳入本版本

- 后台预警/账单调度器。
- 真实 AI、RAG、外部知识库和自动执行写操作。
- 宿舍公告和校园通用公告当前共享 announcements 表，但保留两个客户端服务边界；后续做适配合并，不重复建表。
