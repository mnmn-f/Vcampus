-- AI 助手知识扩充：仅写入 AI 自有知识表，不复制业务实时数据。
-- 所有业务事实仍通过原模块命令实时查询。

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'校园助手三种模式说明',
       '问答模式用于查询校园实时信息和回答系统指南，包括课表、成绩、学籍、馆藏、本人借阅、商品、订单、竞赛、住宿、报修和公告；只读，不修改数据。聊天模式连接通用大模型，可连续讨论校园内外的一般问题，但不执行校园业务。代办模式用于选课、退课、借书、还书、竞赛报名或取消报名、加入购物车和创建订单；所有写操作都会先展示确认信息，确认后仍由原业务模块校验权限、状态、容量、库存和冲突。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园助手三种模式说明');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'校园实时数据查询范围',
       '校园助手不会把课程、图书、商品、订单、竞赛或个人资料复制到知识库。问答模式会使用当前登录身份调用原业务模块，并读取数据库中的最新状态。可询问：我的学院、专业、班级和学号；我的课表和成绩；图书馆有什么书、某书是否可借、我借了什么书；商店有什么商品、我的购物车和订单；目前有哪些竞赛、我的 SRTP；我的宿舍、水电、报修、请假和教室申请。结果以原业务模块实时返回为准。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园实时数据查询范围');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'学籍资料查询说明',
       '学生可在问答模式直接询问“我的学院”“我的专业”“我的班级”“我的学号”“我的入学年份”等。助手通过学籍服务实时返回姓名、账号、学号、学院、专业、班级、培养层次、入学年份、预计毕业年份和学籍状态等本人可见字段。若资料缺失，应联系学籍管理人员维护，助手不会根据其他信息猜测。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学籍资料查询说明');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'图书馆馆藏与借还书指南',
       '问答模式可用“图书馆有什么书”“查一下 Java 相关书籍”“我借了哪些书”等自然表达查询实时馆藏和本人借阅。馆藏结果包含图书编号、书名、作者、分类、位置、总册数、可借册数和状态。代办模式可按编号或书名借书，也可先说“帮我还书”列出本人借阅记录，再按借阅记录编号或书名归还。借书和还书必须确认，最终由图书馆服务校验库存、借阅状态和权限。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='图书馆馆藏与借还书指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'校园商店查询与代办指南',
       '问答模式中“商品”“查看商品”“查找商品”“校园商店有什么”“我的订单”“购买记录”等说法都可查询实时商品或本人订单，不要求出现购物车字样。代办模式可以按商品编号或商品名称加入购物车，并可从购物车创建订单；这些操作需要确认。创建订单不等于支付，支付、优惠券选择和好友代付仍在校园商店页面完成，并由商店服务重新校验库存、价格、余额与订单状态。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园商店查询与代办指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'竞赛查询和报名指南',
       '问答模式可查询系统当前显示的竞赛及其编号、名称、简介、开始与结束时间、报名截止时间、容量、已报名人数和状态。代办模式可直接说竞赛名称，例如“帮我报名数学建模竞赛”；助手会用名称检索实时竞赛并解析编号，再展示确认信息。若名称无法唯一匹配，应先查看竞赛列表并补充更完整名称。报名和取消报名最终由竞赛服务检查登录身份、报名窗口、容量和重复报名。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='竞赛查询和报名指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'课程查询与选退课指南',
       '问答模式可查询本人课表、成绩和系统中可见课程。代办模式可按课程数据库编号、课程代码或课程名称发起选课和退课；名称会先通过实时课程列表解析。确认后，教务服务仍会校验课程是否发布、容量是否充足、是否重复选课、是否存在上课时间冲突以及当前用户是否具有选课权限。成功后可在本人课表或已选课程中核对。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='课程查询与选退课指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'宿舍服务查询指南',
       '问答模式可查询本人住宿信息、水电分摊、报修记录、请假记录和宿舍公告。可使用“我住在哪”“这个月水电怎么样”“我的报修到哪一步了”等自然表达。结果来自宿舍模块实时数据。当前助手只提供这些已登记的只读工具；新建报修、提交请假等尚未进入代办白名单时，应在宿舍服务页面办理。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='宿舍服务查询指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'连续对话和指代说明',
       '同一会话会保留最近对话上下文，可继续追问“它什么时候截止”“那我报名这个”“第三个呢”等。为避免误操作，涉及写入时助手会结合上下文解析对象，但仍展示对象名称和编号供用户确认。新建会话会开始独立上下文；清空会话会归档原历史。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='连续对话和指代说明');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_RULE',NULL,'校园助手权限、确认与隐私边界',
       '助手只能调用登记在白名单中的既有业务命令，不能直接修改其他模块数据库。当前登录会话是身份唯一来源，用户话语和模型输出都不能覆盖用户编号或角色。所有写操作先生成五分钟有效的确认请求，确认后由原业务模块再次鉴权并执行事务。助手不得输出密码、会话令牌或密钥，也不得把工具执行失败描述为成功。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园助手权限、确认与隐私边界');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`)
SELECT 'SYSTEM_GUIDE',NULL,'校园助手常见问题排查',
       '按 Enter 发送消息，Shift+Enter 换行。若聊天模式提示模型未配置，需要在服务端配置兼容 Responses API 的密钥和地址；问答模式的实时校园查询以及确定性代办仍可使用。若知识问答提示未收录，应由知识管理员补充经核验的正式材料；若实时查询无结果，应检查当前账号权限、筛选词和业务数据状态。写操作未确认、确认超时或原模块校验失败时都不会修改数据。',
       'ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园助手常见问题排查');
