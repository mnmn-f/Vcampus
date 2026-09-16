# VCampus JavaFX 客户端预览

该模块与现有 `vcampus-client` Swing 客户端并存，默认 Swing 启动入口不变。它使用 Java 17 语言级别、JavaFX 21、FXML、CSS 和 AtlantaFX Primer Light，当前用于验收登录页、学生主框架、学籍概览和校园商店的页面结构与视觉风格。

## 启动

在仓库根目录执行：

```powershell
.\mvnw.cmd -pl vcampus-client-fx javafx:run
```

首次运行需要能够访问 Maven Central 下载 JavaFX 与 AtlantaFX 依赖。测试命令：

```powershell
.\mvnw.cmd -pl vcampus-client-fx test
```

登录页不连接服务器，输入任意非空账号和密码即可进入固定学生身份页面，用于查看 UI；现有 Swing 客户端的数据库、Socket 和登录流程没有改动。
