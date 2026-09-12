# VCampus 局域网部署

## 1. 构建

在项目根目录执行：

```powershell
.\mvnw.cmd -q -Pquality verify
```

验证通过后会生成两个可执行 shaded jar：

- `vcampus-server/target/vCampusServer.jar`，入口 `edu.seu.vcampus.server.ServerMain`
- `vcampus-client/target/vCampusClient.jar`，入口 `edu.seu.vcampus.client.AppLauncher`

服务端使用现有 MySQL 配置边界。部署时通过 `VCAMPUS_DB_URL`、`VCAMPUS_DB_USER`、
`VCAMPUS_DB_PASSWORD`（或对应 JVM 属性）提供连接信息；不要把密码写入脚本、命令历史或仓库。
项目按 Java 17 构建，并使用支持 MySQL 8.0 默认认证方式的 Connector/J 9.5.0。数据库账号认证插件
属于部署配置；不要通过降低业务用户密码哈希或把数据库密码写入代码来规避连接问题。

聊天模型默认使用 DeepSeek Responses API。仅在服务端设置 `DEEPSEEK_API_KEY`（或优先级更高的 `VCAMPUS_AI_API_KEY`）；不要把密钥放入客户端或仓库。可选覆盖项为 `VCAMPUS_AI_ENDPOINT`、`VCAMPUS_AI_MODEL` 和 `VCAMPUS_AI_VISION_MODEL`。未配置密钥时服务端仍可启动，校园实时工具和本地知识库降级仍可使用，但通用聊天和图片理解不可用。

## 2. 两台电脑启动

在电脑 A（安装 MySQL、保存唯一业务数据库的电脑）执行：

```powershell
.\scripts\start-lan-server.ps1
```

脚本会隐藏读取 MySQL 密码、启动服务端，并打印当前电脑可用的局域网 IPv4 和队友命令。服务端监听
所有本机网络接口；所有客户端请求最终读写电脑 A 上同一个 `vcampus` 数据库，因此不是每个人各自维护
一份数据。服务端状态变更提交后，其他客户端可刷新当前列表或重新进入页面读取最新结果。
通用分页列表以及已接入的购物车、账户、图书页面会在可见且未编辑时约每 8 秒刷新；
隐藏页面停止轮询，正在输入或部分选中编辑场景暂停自动更新，避免覆盖未保存内容。这不是服务器推送。

在电脑 B 执行：

```powershell
.\scripts\start-lan-client.ps1 -ServerHost 192.168.1.20 -ServerPort 8888
```

拥有校园助手权限的账号登录后会启用小松鼠桌宠。桌宠动画只在可见时运行；远程桌面、自动化测试或低性能终端可直接启动客户端 JAR 并增加 `-Dvcampus.pet.animation=false`：

```powershell
java -Dvcampus.pet.animation=false -Dvcampus.server.host=192.168.1.20 `
  -Dvcampus.server.port=8888 -jar .\vcampus-client\target\vCampusClient.jar
```

该属性只关闭呼吸、摇摆和状态动画，不影响点击入口、最小化收束、拖动恢复、AI 对话或业务功能。

把 `192.168.1.20` 换成电脑 A 的脚本打印出的 IPv4 地址，不要使用客户端自己的 `127.0.0.1`。
客户端可用 `VCAMPUS_SERVER_HOST`、`VCAMPUS_SERVER_PORT`、`VCAMPUS_CLIENT_CONNECT_TIMEOUT`、
`VCAMPUS_CLIENT_READ_TIMEOUT` 环境变量；脚本会先检查 jar 文件存在。

两台电脑应在同一网段。电脑 B 可先检查：

```powershell
Test-NetConnection 192.168.1.20 -Port 8888
```

电脑 B 不需要安装或开放 MySQL，也不要把 MySQL 3306 暴露给队友；客户端只连接服务端 TCP 8888。

如果队友不在同一局域网，需先通过可信 VPN/组网服务建立可达的私有网络，再使用电脑 A 的 VPN 地址。
当前 Socket 协议未提供 TLS，不建议直接将 8888 端口暴露到公网。连接到不同电脑各自的数据库不会同步。

## 3. Windows 防火墙

只在电脑 A 的“专用网络”开放服务端端口，并按实际网段限制来源：

```powershell
New-NetFirewallRule -DisplayName 'VCampus TCP 8888' -Direction Inbound `
  -Protocol TCP -LocalPort 8888 -Action Allow -Profile Private
```

不需要远程访问时不要在公用网络放行；测试结束可删除规则：

```powershell
Remove-NetFirewallRule -DisplayName 'VCampus TCP 8888'
```

## 4. 网络行为和故障排查

- 客户端连接或读取超时会关闭坏连接；下一次显式请求才会重新连接，不会自动重放请求。因此下单、支付等非幂等操作由业务层决定是否重新发起。
- 服务端默认每个连接读超时 30 秒，空闲握手、半关闭和断帧连接会被释放；连接池有界，超出容量的连接会被拒绝。
- 普通 EOF、半关闭、读超时或网络异常只关闭对应 socket，不注销已登录 token；客户端重连后仍可使用有效 token。登出、管理员强制下线和服务端 `stop()` 才会让 token 失效。
- 客户端与服务端均先发送并 flush `ObjectOutputStream`，再建立输入流，避免 ObjectStream 握手死锁。
- 反序列化只允许 common DTO/协议、必要的数值、时间、集合和枚举类型，拒绝代理类及非协议容器。8 MiB 限额是单条持久 TCP 连接所有对象读取的累计预算，不是单个请求上限；正常小消息可以复用连接，超过预算的连接会关闭。
- 非 `Message` 对象、非法类、截断帧和读写异常不会向客户端返回堆栈；服务端按协议处理或关闭对应 socket，不因普通断连注销账号会话。

若连接失败，依次确认 jar 与 Java 可执行文件、电脑 A 的局域网 IP、MySQL 可达性、`Test-NetConnection` 结果和防火墙规则；修改超时或端口后需重启对应脚本。
