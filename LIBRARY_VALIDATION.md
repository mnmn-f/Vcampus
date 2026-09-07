# 构建与验收记录

## 图书详情交互修复验证

- 已从“图书查阅”页面结构中移除底部固定详情卡片。
- 每本书的“详情”按钮现在直接打开“书籍详情”窗口，并按所选图书 ID 读取封面、书目、库存及个人借阅记录。
- 客户端与公共模块主源码已用 Java 17 全量编译通过；三份客户端 JAR 中的实现已同步。

## 本轮三项修复验证

- 服务端与公共模块全部 956 个主类使用 Java 17 编译通过；客户端与公共模块全部 1154 个主类编译通过。
- 独立运行时烟雾验证通过：9 张封面均可从服务端 JAR 读取，借书后库存减少、还书后库存恢复，PDF 分块写入并提交后状态为 `PENDING`。
- 服务端 JAR 已包含自动数据库升级类、V6 迁移和 9 张 150×220 PNG 封面。
- 当前验证环境无法解析 Maven Central 域名，因此未重新执行完整 Maven 测试套件；失败点是依赖下载，不是源码编译或测试断言。

## 完整构建

- 环境：Linux，OpenJDK 17，项目自带 Maven Wrapper。
- 原项目 JDK/依赖配置未改动。
- `mvnw package` 完成，生成本包中的客户端与服务端 JAR。
- 测试共 370 项：358 项通过，0 项失败，0 项错误，12 项跳过。
- 12 项跳过测试均依赖未配置的真实 MySQL 环境；未连接或修改你的电脑与数据库。
- 未在 Windows 桌面上运行本次程序；Swing 页面采用实际组件离屏渲染，已查看对应截图。

## 新增测试

| 测试类 | 测试数 | 结果 |
| --- | --- | --- |
| LibraryUpgradeLayoutTest | 3 | 通过 |
| PdfFileTransfersTest | 2 | 通过 |
| PdfConnectionRotationTest | 1 | 通过 |
| PdfWorkflowTest | 3 | 通过 |
| BookDetailsMetadataTest | 1 | 通过 |

验证包括：真实 TCP 下约 9 MB PDF 字节往返与 SHA-256；新连接保留会话身份；两个学生与管理员的未审核资源访问控制；拒绝理由、停用后下载拒绝；非法文件、路径和不完整上传；同名文件并行下载；Demo 重开记录保留；下载失败和旧会话失效；图书封面/年份在库存调整与归档后保留；390/560/1000 像素预约表单的控件边界。

网络业务测试使用真实 TCP 与内存仓储，MySQL DAO 和 V6 迁移仍需要在目标 MySQL 8 数据库执行后验证。`LIBRARY_UPGRADE_GUIDE.md` 列出了升级与人工联调步骤。

## 原项目排序问题

原始 ZIP 的商店退款测试单次可能通过；在未修改源码的独立副本中，同一 JVM 重复运行 8 轮共 40 项，出现 7 次同时间流水排序失败。已补上与原 MySQL `ORDER BY created_at DESC, id DESC` 一致的编号倒序规则，最终完整构建通过。

## 页面截图

截图来自真实 Swing 组件和测试样例数据，位于 `docs/library-previews/`：预约表单窄/中/宽窗口、书籍详情、学生 PDF 列表与管理员审核列表。

## JAR 校验值（SHA-256）

- `vcampus-client/target/vCampusClient.jar`：`a3a368c5ad4ada33e84e980f4ef850676e2d05ea604e1e4ce5be65b9fa5545ad`
- `vcampus-server/target/vCampusServer.jar`：`4ed0a8013f1ae54d5b60ea2e88ca75f65e56aae9f1e44876804464c42e399ab8`
