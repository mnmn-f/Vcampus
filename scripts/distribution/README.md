# Windows 双击启动包

在仓库根目录用 PowerShell 执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1
```

需要 JDK 17+。脚本通过 Maven Wrapper 构建项目，在 `target/windows-release-时间戳/` 生成服务端和客户端 ZIP。首次构建需要联网下载 Maven 依赖。

- 服务端：解压 `VCampus-Server.zip`，双击 `start-server.cmd`。需要已运行的 MySQL 和已初始化的项目数据库，启动时隐藏输入数据库密码。
- 本机客户端：解压 `VCampus-Client.zip`，双击 `start-local-client.cmd`。
- 其他电脑：解压客户端 ZIP，双击 `start-client.cmd`，填写服务端 IP。需要网络可达且服务端端口允许连接。

这些 CMD 是打包模板，需要和构建后的 JAR 一起使用，不能直接在源码目录双击运行。详细网络配置见包内 `使用说明.txt`。

已有构建产物时，可加 `-SkipBuild` 仅打包。启动包不包含 Java、数据库数据、数据库密码或 API 密钥，也不会自动初始化数据库、修改防火墙。
