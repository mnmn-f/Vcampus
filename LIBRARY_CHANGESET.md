> 本文保留上一轮交付记录。本次 PDF 审核与自适应布局升级以
> [LIBRARY_UPGRADE_GUIDE.md](LIBRARY_UPGRADE_GUIDE.md) 和 [LIBRARY_VALIDATION.md](LIBRARY_VALIDATION.md) 为准。
> PDF 演示数据已增加本机持久化，其余原有 Demo 数据规则见新说明。

# 图书馆模块交付说明

## 本次实现结果

- 保留原项目 Swing 界面主题，未修改 `DesignTokens.java`。
- 保留原项目编译配置，未修改任何 `pom.xml`、JDK、Maven 依赖版本或 `.vscode` 配置。
- 学生端可在界面完成：公告查看、图书检索与详情、借阅、归还、自习室检索、预约、取消预约、线上资源检索与访问。
- 图书管理员端可在界面完成：公告新增/编辑/发布/撤回、图书新增/编辑/归档、借阅记录筛选与 CSV 导出、自习室新增/编辑/关闭、全部预约查询与取消、线上资源新增/编辑/停用、资源访问日志查询。
- Demo 模式不再为图书馆显示“需要连接服务端”，所有上述操作会即时更新本地内存数据；关闭客户端后 Demo 数据重置。
- Network 模式仍使用原项目 TCP 服务端和 MySQL，借还书库存、重复借阅、预约时间冲突、权限和状态规则继续由服务端事务校验。

> 为保留借阅、预约和访问历史，界面中的“删除”采用业务软删除：图书归档、自习室关闭、资源停用、公告撤回。它们不会破坏外键和历史记录。

## 页面入口

学生端：

1. 首页：图书馆公告栏。
2. 图书查阅：图书查询、详情、借阅、我的借阅与归还。
3. 自习室预约：房间查询、预约、取消和我的预约记录。
4. 线上资源：查询并访问已启用资源。

图书管理员端：

1. 公告管理。
2. 图书管理。
3. 借阅管理。
4. 自习室管理和预约记录。
5. 线上资源管理和访问日志。

## 新增文件

- `scripts/start-library-demo.ps1`：Windows 一键启动图书馆 Demo。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/service/library/DemoLibraryClientService.java`：图书馆 Demo 服务门面。
- `DemoBookService.java`、`DemoStudyRoomService.java`、`DemoOnlineResourceService.java`：分别承载借阅、预约和线上资源流程。
- `DemoLibrarySupport.java`：分页、检索和错误处理的共享辅助逻辑。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/service/library/DemoLibraryCampusClientService.java`：可操作的图书馆 Demo 公告服务。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/service/library/DemoLibraryData.java`：图书馆 Demo 初始数据。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/LibraryTaskTabs.java`：复用现有主题令牌的图书馆角色导航。
- `vcampus-client/src/test/java/edu/seu/vcampus/client/service/library/DemoLibraryClientServiceTest.java`：本地操作与数据更新测试。
- `LIBRARY_CHANGESET.md`：本交付和文件清单。

## 修改的已有文件

- `README.md`：说明图书馆 Demo 已可操作及其非持久化边界。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/ModulePages.java`：Demo 模式将图书馆路由到真实可操作页面。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/RealLibraryPage.java`：按学生/图书管理员组合文档中的功能入口，并让 Demo/Network 共用界面。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/LibraryBooksPanel.java`：分类搜索提示、借阅和管理员归档操作。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/LibraryBorrowingsPanel.java`：学生借阅记录与归还入口文案。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/LibraryBorrowingLedgerPanel.java`：管理员借阅管理入口文案。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/LibraryRoomsPanel.java`：学生预约/取消，管理员全部预约查询/取消和关闭房间。
- `vcampus-client/src/main/java/edu/seu/vcampus/client/view/modules/real/LibraryResourcesPanel.java`：资源访问、管理员停用和访问日志入口。
- `vcampus-server/src/main/java/edu/seu/vcampus/server/library/repository/mysql/MySqlBookRepository.java`：书名/作者/ISBN 搜索扩展为同时搜索分类。
- `vcampus-server/src/main/java/edu/seu/vcampus/server/library/repository/mysql/MySqlOnlineResourceRepository.java`：资源关键字搜索同时覆盖资源类型。
- `vcampus-server/src/main/java/edu/seu/vcampus/server/library/repository/InMemoryBookRepository.java`：与 MySQL 图书搜索行为保持一致。
- `vcampus-server/src/main/java/edu/seu/vcampus/server/library/repository/InMemoryOnlineResourceRepository.java`：与 MySQL 资源搜索行为保持一致。
- `vcampus-client/src/test/java/edu/seu/vcampus/client/view/modules/ModulePagesTest.java`：增加 Demo 图书馆页面路由测试。
- `vcampus-client/src/test/java/edu/seu/vcampus/client/view/modules/real/ClientRoleCompositionTest.java`：增加学生和图书管理员页面组合测试。

## Windows 上运行可操作 Demo

在项目根目录打开 PowerShell：

```powershell
.\mvnw.cmd -pl vcampus-client -am -DskipTests package
.\scripts\start-library-demo.ps1
```

学生账号：`demo_student`，密码：`student123`。

图书管理员账号：`demo_librarian`，密码：`library123`。

也可以不用脚本，直接执行：

```powershell
java "-Dvcampus.client.mode=demo" -jar ".\vcampus-client\target\vCampusClient.jar"
```

请始终运行 `AppLauncher` 生成的客户端 JAR，不要直接运行 `LibraryTaskTabs.java` 或某个 Panel；这些界面组件没有 `main` 方法。

## 使用 MySQL 持久化运行

1. 按原项目说明依次导入 `V1__baseline.sql`、`V2__demo_data.sql`。
2. 在项目根目录执行 `.\mvnw.cmd package`。
3. 使用原项目数据库参数启动 `vCampusServer.jar`。
4. 执行 `.\scripts\start-client.ps1` 启动网络客户端。

Network 模式的数据库账号、端口和脚本没有在本次修改中改变。

## 验证记录

- `git diff --check`：通过。
- 图书馆 Demo 服务、角色页面组合和 JDBC 时间转换定向测试：通过。
- 完整 Maven 测试与质量门禁以合并提交的 CI/本地验收结果为准。
