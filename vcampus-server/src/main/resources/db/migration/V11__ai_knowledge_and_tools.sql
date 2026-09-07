-- AI 助手知识扩充。政策内容按主题摘要，业务实时状态仍由原模块工具读取。

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：适用范围与管理原则','《东南大学学生公寓管理办法》（2025年6月修订版）适用于学校统一安排住宿的学生。学生公寓实行学校统筹、部门协同、学院参与、学生自我教育与自我管理相结合；住宿人应遵守国家法律法规、学校规章和公寓管理要求。用户提供的扫描件文件名标注“2026年”，知识条目按其中现行内容整理，遇版本差异应以学校最新正式发布文本为准。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：适用范围与管理原则');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：入住调宿与退宿','住宿由学校统一安排。学生应按分配的楼栋、房间和床位入住，不得私自调换、转让、出租床位或留宿他人；确需调宿应按学校流程申请并获批准。离校、休学、退学、毕业或其他需要退宿的情形，应在规定期限内办理退宿、交还钥匙、结清费用并带走个人物品。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：入住调宿与退宿');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：门禁访客与夜间秩序','住宿人应遵守门禁、会客和查验制度，维护公共秩序。访客应按要求登记并在规定时间、区域内活动，不得擅自留宿校外人员。晚归或夜不归宿应按要求说明或办理请假；不得以喧哗、起哄、酗酒等方式影响他人学习休息。具体开放和门禁时间以所在公寓最新通知为准。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：门禁访客与夜间秩序');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：消防与用电安全','学生应爱护消防设施，保持消防通道和安全出口畅通；不得挪用、遮挡或损坏消防器材，不得在室内使用明火、焚烧物品、吸烟或存放易燃易爆及其他危险物品。发现火情或安全隐患应立即报告并服从应急处置，不得虚报火警。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：消防与用电安全');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：禁用电器与电池充电','公寓内不得私拉乱接电线或违规改变供电设施。扫描件列举的禁止或限制情形包括：电动自行车电池、平衡车等在室内充电；户外便携储能设备和大容量蓄电池；无国家强制性产品认证标志的电器；无自动断电保护装置的相关设备；额定功率达到或超过800瓦的其他大功率电器；以及其他存在用电安全隐患的设备。因疾病确需使用特殊医疗电器，应凭疾病证明履行申请、审批和登记。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：禁用电器与电池充电');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：卫生设施与公共财物','住宿人应维护室内外清洁，按规定分类投放垃圾，配合卫生检查和传染病防控；不得向楼外抛物、倾倒污水或堆放妨碍通行的物品。家具、门窗、水电、网络和公共设施不得擅自拆改、搬移或损坏；发现故障可通过宿舍报修流程登记，因人为原因造成损失的按规定承担责任。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：卫生设施与公共财物');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生公寓管理：禁止经营饲养与危险行为','未经批准不得在学生公寓从事经营、推销、广告张贴、收费服务等活动；不得饲养影响卫生、安全或他人生活的动物；不得赌博、酗酒滋事、打架斗殴、传播违法信息，或携带管制器具和危险物品。学校可依据情节采取教育整改、依规处理并移交相关部门。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生公寓管理：禁止经营饲养与危险行为');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：原则与处分种类','学生违纪处分坚持教育与惩戒相结合，做到事实清楚、证据充分、依据明确、定性准确、程序正当、处分适当。处分一般包括警告、严重警告、记过、留校察看和开除学籍；具体处分应结合行为性质、情节、后果、本人态度及从轻、从重情形依法依规确定。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：原则与处分种类');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：从轻减轻与从重情形','主动承认错误、配合调查、及时消除影响或赔偿损失等情形，可按条例综合认定是否从轻或减轻；拒不承认、妨碍调查、串供伪造证据、打击报复、屡次违纪或造成严重后果等，可能从重处理。是否适用及处分幅度由有权部门依事实、证据和正式条款决定，助手不能代替处分认定。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：从轻减轻与从重情形');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：公共秩序与人身财产','扰乱学校教育教学、生活秩序或公共场所秩序，打架斗殴、寻衅滋事、侮辱诽谤、侵害他人人身权利，盗窃、诈骗、侵占、故意损坏公私财物等行为，可依据情节和后果给予相应处分；涉嫌违法犯罪的还可能移送有关机关。具体认定应查阅正式条例对应条款。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：公共秩序与人身财产');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：网络与信息行为','利用网络制作、复制、发布、传播违法有害信息，侵犯他人隐私、名誉或知识产权，冒用身份、攻击系统、窃取或篡改数据，以及其他危害网络与信息安全的行为，可按性质、影响和后果处理。正常批评建议与违法侵权行为应依据事实和正式规则区分。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：网络与信息行为');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：宿舍消防与危险物品','在宿舍违反消防安全规定、违规使用电器或明火、私拉电线、给电动车电池等危险设备充电、堵塞消防通道，或存放使用管制器具、易燃易爆和其他危险物品，可能依据情节、整改情况及是否造成事故给予处分。造成损害的还应承担相应责任。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：宿舍消防与危险物品');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：学习考试与学术诚信','无故旷课、扰乱课堂或考试秩序、考试作弊，以及抄袭、剽窃、伪造研究数据或其他学术不端行为，按照行为性质和情节处理。对考试与学术行为的判断应使用课程、考试和学术规范的正式证据，不应仅凭传闻；涉及学位和成果处理时还应适用相关专门规定。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：学习考试与学术诚信');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'POLICY',NULL,'学生违纪处分：调查告知决定与申诉','处分前应进行调查取证，并告知学生拟处分的事实、理由和依据，听取其陈述和申辩；处分决定应按程序作出并送达。学生对处理或处分决定有异议，可在正式规定的期限内向指定机构提出书面申诉。具体期限、材料和受理机构以决定书及学校最新学生申诉办法为准。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='学生违纪处分：调查告知决定与申诉');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'聊天模式附件使用指南','聊天模式输入框左侧“+”可一次选择最多3个附件。图片支持 PNG、JPEG、GIF、WebP，单张不超过2MB；文本和常见代码文件不超过256KB。图片会使用视觉模型，文本内容会连同文件名发给模型。问答和代办模式不接收附件；PDF、Word、压缩包等当前未直接解析，应转为图片或纯文本后上传。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='聊天模式附件使用指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'自习室预约操作指南','问答模式可查询可用自习室和本人预约。代办模式可说“预约自习室”，并提供自习室编号、开始和结束时间；时间格式示例为2026-09-06T14:00。参数不全时助手会逐项追问，确认后由图书馆模块检查开放状态和时段冲突。取消预约需要预约记录编号。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='自习室预约操作指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'校园商店完整代办指南','代办模式支持按商品名称加入购物车、修改数量、移除商品、从购物车创建订单、按订单编号余额支付，以及按代码领取优惠券。每个写操作均先确认，价格、库存、订单状态、余额、优惠券资格和幂等性仍由商店服务实时校验。问“我的订单”“买过什么”会直接查询订单，不要求出现“购物车”。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园商店完整代办指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'宿舍报修请假与水电代办指南','代办模式支持提交报修、提交或取消请假、支付水电分摊。报修需房间编号、故障类别和描述，可选优先级；请假需类型、起止时间和原因；缴费需待缴分摊编号。助手缺少字段时会先澄清，确认后宿舍模块按本人身份、记录状态和余额执行。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='宿舍报修请假与水电代办指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'教室查询申请与取消指南','问答模式可查询可申请教室和本人申请记录。代办模式提交申请时需教室编号、用途、开始时间和结束时间；取消需本人申请编号。助手先澄清缺少参数并展示确认，最终由校园服务校验教室状态、时段冲突、申请权限和记录状态。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='教室查询申请与取消指南');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'快捷问题与自然语言理解','聊天框上方按当前模式显示可横向滚动的常用示例，点击会立即发送。固定规则覆盖高频表达，大模型意图路由可把“买过什么”“找点能买的东西”“参加数学建模”等近义说法映射到实时工具。写操作只在意图明确时选择工具；缺少名称、编号、时间、数量、原因等参数时进入多轮澄清，不猜测业务数据。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='快捷问题与自然语言理解');

INSERT INTO `ai_knowledge_chunks` (`source_type`,`source_ref_id`,`title`,`content`,`status`,`updated_by`) SELECT
'SYSTEM_GUIDE',NULL,'校园助手结果阅读说明','实时工具结果以“实时数据”标题开头，并按记录分组展示关键字段、状态和编号；编号可在后续代办中引用。知识问答应区分正式规则摘要、系统操作指南与实时个人数据。查询无结果不等于系统故障，可能是当前账号没有对应记录、筛选条件过窄或业务记录尚未生成。','ACTIVE',NULL
WHERE NOT EXISTS (SELECT 1 FROM `ai_knowledge_chunks` WHERE `title`='校园助手结果阅读说明');

-- 替换 V6 中已过时的“宿舍只读”描述和“支付仍需手工完成”描述。
UPDATE `ai_knowledge_chunks` SET `content`='问答模式可查询本人住宿信息、水电分摊、报修记录、请假记录和宿舍公告。代办模式可提交报修、提交或取消请假、支付本人水电分摊；助手会先澄清必要参数并请求确认，最终由宿舍模块校验身份、状态和余额。'
WHERE `title`='宿舍服务查询指南';

UPDATE `ai_knowledge_chunks` SET `content`='问答模式中“商品”“查看商品”“查找商品”“校园商店有什么”“我的订单”“购买记录”等说法都可查询实时商品或本人订单，不要求出现购物车字样。代办模式支持加入、修改和移除购物车商品，创建订单、余额支付订单和领取优惠券；所有写操作需要确认，并由商店服务重新校验库存、价格、余额、资格与订单状态。'
WHERE `title`='校园商店查询与代办指南';
