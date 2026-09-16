-- Optional presentation upgrade of the original V2/V18 demonstration rows.
-- User-created rows, credentials, relationships, money, order states and uploaded images are preserved.
-- Apply after V20, not as a production data import. Source details: docs/SEU_DATA_SOURCES.md.
SET NAMES utf8mb4;
USE vcampus;
START TRANSACTION;
CREATE TEMPORARY TABLE _vcampus_people (account VARCHAR(80) PRIMARY KEY, old_name VARCHAR(120), new_name VARCHAR(120)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO _vcampus_people VALUES
('demo_student','演示学生','王晨茜'),
('demo_teacher','演示教师兼教务员','程知远'),
('demo_registrar','演示学籍管理员','周敏'),
('demo_academic','演示教务老师','陈静'),
('demo_librarian','演示图书管理员','许文清'),
('demo_store','演示商店管理员','林晓岚'),
('demo_dorm','演示宿管员','杨慧'),
('demo_ai','演示AI知识管理员','陆知行'),
('demo_system','演示系统管理员','陈宇'),
('demo_repair','演示维修员','赵建国'),
('test_student01','测试学生01','龚巧璇'),
('test_student02','测试学生02','孙嘉妍'),
('test_student03','测试学生03','张毓琛'),
('test_student04','测试学生04','孙瀚晨'),
('test_student05','测试学生05','陈思远'),
('test_student06','测试学生06','李若彤'),
('test_student07','测试学生07','周子涵'),
('test_student08','测试学生08','林书瑶'),
('test_student09','测试学生09','赵奕辰'),
('test_student10','测试学生10','徐知夏'),
('test_student11','测试学生11','吴承泽'),
('test_student12','测试学生12','许清禾'),
('test_student13','测试学生13','何予安'),
('test_student14','测试学生14','蒋星澜'),
('test_student15','测试学生15','沈嘉宁'),
('test_student16','测试学生16','陆景行'),
('test_student17','测试学生17','顾明轩'),
('test_student18','测试学生18','苏雨桐'),
('test_student19','测试学生19','程以宁'),
('test_student20','测试学生20','唐书言'),
('test_teacher01','测试教师01','陈明远'),
('test_teacher02','测试教师02','李文博'),
('test_teacher03','测试教师03','赵清扬'),
('test_teacher04','测试教师04','周若宁'),
('test_teacher05','测试教师05','许知行'),
('test_teacher06','测试教师06','陆景和');
UPDATE users u JOIN _vcampus_people p ON u.username=p.account
SET u.display_name=p.new_name WHERE u.display_name=p.old_name;

-- Correct only the original generated student directory fields.
UPDATE student_profiles sp JOIN users u ON u.id=sp.user_id
SET sp.major=CASE WHEN sp.college='电子科学与工程学院' AND sp.major='信息工程' THEN '电子科学与技术'
                 WHEN sp.college='计算机科学与工程学院' AND sp.major='软件工程' THEN '计算机科学与技术' ELSE sp.major END
WHERE (u.username REGEXP '^test_student[0-9]{2}$' AND sp.student_no=CONCAT('TEST2026',RIGHT(u.username,2)))
   OR (u.username='demo_student' AND sp.student_no='DEMO2026001');
UPDATE student_profiles sp JOIN users u ON u.id=sp.user_id
SET sp.class_name=CONCAT(sp.major,sp.enrollment_year,'级',1+MOD(CAST(RIGHT(u.username,2) AS UNSIGNED),2),'班')
WHERE u.username REGEXP '^test_student[0-9]{2}$' AND sp.student_no=CONCAT('TEST2026',RIGHT(u.username,2)) AND sp.class_name REGEXP '^测试班[1-4]$';
UPDATE student_profiles sp JOIN users u ON u.id=sp.user_id SET sp.class_name='计算机科学与技术2026级1班'
WHERE u.username='demo_student' AND sp.student_no='DEMO2026001' AND sp.class_name='软件工程2601';
UPDATE student_profiles sp JOIN users u ON u.id=sp.user_id
LEFT JOIN student_profiles other ON other.student_no=CONCAT('0902601',RIGHT(u.username,2)) AND other.user_id<>sp.user_id
SET sp.student_no=CONCAT('0902601',RIGHT(u.username,2))
WHERE u.username REGEXP '^test_student[0-9]{2}$' AND sp.student_no=CONCAT('TEST2026',RIGHT(u.username,2)) AND other.user_id IS NULL;
UPDATE student_profiles sp JOIN users u ON u.id=sp.user_id
LEFT JOIN student_profiles other ON other.student_no='090260100' AND other.user_id<>sp.user_id
SET sp.student_no='090260100' WHERE u.username='demo_student' AND sp.student_no='DEMO2026001' AND other.user_id IS NULL;
UPDATE student_profiles sp JOIN users u ON u.id=sp.user_id SET sp.address='南京市江宁区东南大学路2号'
WHERE u.username REGEXP '^test_student[0-9]{2}$' AND sp.address='南京市江宁区测试地址';

CREATE TEMPORARY TABLE _vcampus_courses (old_code VARCHAR(40) PRIMARY KEY,new_code VARCHAR(40),new_name VARCHAR(160),credits DECIMAL(5,2),hours INT,description TEXT) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO _vcampus_courses VALUES
('TEST-C-001','B07M1051','工科数学分析I',5,96,'函数、极限、微分与积分及其工程应用。'),
('TEST-C-002','B07M2041','线性代数',3.5,64,'矩阵、线性方程组、向量空间与线性变换。'),
('TEST-C-003','BJSL0021','程序设计基础及语言I',4,80,'程序设计基础、控制结构、函数与数组。'),
('TEST-C-004','BJSL0011','计算机大类新生研讨',1,24,'计算机学科概览、学习方法与专业发展。'),
('TEST-C-005','B07M1061','工科数学分析II',5,96,'多元微积分、级数与微分方程。'),
('TEST-C-006','BJSL0040','离散数学',4,64,'数理逻辑、集合、关系、图论与组合基础。'),
('TEST-C-007','BJSL0031','程序设计基础及语言II',2.5,48,'数据抽象、程序组织与综合编程实践。'),
('TEST-C-008','BJSL0051','数字逻辑电路',3,48,'组合逻辑、时序逻辑与数字系统设计。'),
('TEST-C-009','BJSL0061','数据结构',4,72,'线性表、树、图、查找和排序算法。'),
('TEST-C-010','BJSL0071','计算机组成原理',4,72,'数据表示、运算器、存储层次与处理器设计。'),
('TEST-C-011','B07M3010','概率论与数理统计',3,48,'随机变量、概率分布、参数估计与假设检验。'),
('TEST-C-012','B10M0241','大学物理BⅠ',3,64,'力学、热学与物理问题的数学建模。'),
('TEST-C-013','B09A0010','人工智能概论',3,56,'搜索、知识表示、推理与机器学习基础。'),
('TEST-C-014','B09T0011','算法设计与分析',3,64,'分治、动态规划、贪心策略与复杂度分析。'),
('TEST-C-015','BJSL0082','操作系统',4,72,'进程管理、内存管理、文件系统与设备管理。'),
('TEST-C-016','B09S1031','Java程序设计',2,40,'面向对象设计、集合、异常处理与图形界面编程。'),
('TEST-C-017','B09D0012','数据库原理',3,56,'关系模型、SQL、规范化、事务与并发控制。'),
('TEST-C-018','B09N0014','计算机网络',3,56,'网络体系结构、传输协议、路由与应用层协议。'),
('TEST-C-019','B71S0032','编译原理',4,72,'词法分析、语法分析、中间代码与代码生成。'),
('TEST-C-020','B09G0011','数字图像处理',3,56,'图像增强、变换、分割与特征提取。'),
('TEST-C-021','B09S0061','软件工程',3,56,'需求分析、软件设计、质量保证与项目协作。'),
('TEST-C-022','B71S1170','软件建模与UML',2,40,'用例、类图、交互图与软件模型设计。'),
('TEST-C-023','B09A1111','机器学习(研讨)',2,40,'监督学习、模型评价与学习算法研讨。'),
('TEST-C-024','B71S1021','Python编程(研讨)',2,40,'Python语言、数据处理与程序实践。'),
('DEMO-SE-001','B09P0091','软件开发综合课程设计',2,48,'围绕需求分析、系统设计、开发实现与测试开展团队项目。');
UPDATE courses c JOIN _vcampus_courses m ON m.old_code=c.course_code
LEFT JOIN courses other ON other.course_code=m.new_code AND other.id<>c.id
SET c.course_name=m.new_name,c.credits=m.credits,c.total_hours=m.hours,c.description=m.description,
    c.course_type=CASE WHEN m.new_code='B09P0091' THEN 'PRACTICE' WHEN m.new_code IN ('B09S1031','B71S1170','B09A1111','B71S1021') THEN 'ELECTIVE' ELSE 'REQUIRED' END,
    c.course_code=IF(other.id IS NULL,m.new_code,c.course_code)
WHERE c.course_name=CONCAT('测试课程',RIGHT(m.old_code,2)) OR (m.old_code='DEMO-SE-001' AND c.course_name='软件工程实践');
UPDATE course_grades SET remark=NULL WHERE remark IN ('扩展测试成绩','demo_student 多学期测试成绩');
UPDATE classrooms SET building_name='教学楼',equipment_description='多媒体投影、空调'
WHERE building_name='测试教学楼';
UPDATE classrooms SET equipment_description='多媒体投影、空调' WHERE equipment_description REGEXP '^投影、空调、测试设备组[0-9]+$';

CREATE TEMPORARY TABLE _vcampus_products (n INT PRIMARY KEY,new_sku VARCHAR(80),new_name VARCHAR(160),category_code VARCHAR(40),description TEXT) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO _vcampus_products VALUES
(1,'SEU-FOOD-001','全麦面包','FOOD','全麦面包，适用于日常学习与校园生活。请按包装说明使用。'),
(2,'SEU-DAILY-001','抽取式纸巾3包装','DAILY','抽取式纸巾3包装，适用于日常学习与校园生活。请按包装说明使用。'),
(3,'SEU-STATIONERY-001','黑色中性笔0.5mm','STATIONERY','黑色中性笔0.5mm，适用于日常学习与校园生活。请按包装说明使用。'),
(4,'SEU-OTHER-001','USB-C数据线','OTHER','USB-C数据线，适用于日常学习与校园生活。请按包装说明使用。'),
(5,'SEU-CULTURE-001','东南大学校徽书签','CULTURE','东南大学校徽书签，适用于日常学习与校园生活。请按包装说明使用。'),
(6,'SEU-FOOD-002','原味酸奶200g','FOOD','原味酸奶200g，适用于日常学习与校园生活。请按包装说明使用。'),
(7,'SEU-DAILY-002','洗衣液1kg','DAILY','洗衣液1kg，适用于日常学习与校园生活。请按包装说明使用。'),
(8,'SEU-STATIONERY-002','A5横线笔记本','STATIONERY','A5横线笔记本，适用于日常学习与校园生活。请按包装说明使用。'),
(9,'SEU-OTHER-002','无线鼠标','OTHER','无线鼠标，适用于日常学习与校园生活。请按包装说明使用。'),
(10,'SEU-CULTURE-002','九龙湖明信片套装','CULTURE','九龙湖明信片套装，适用于日常学习与校园生活。请按包装说明使用。'),
(11,'SEU-FOOD-003','纯牛奶250mL','FOOD','纯牛奶250mL，适用于日常学习与校园生活。请按包装说明使用。'),
(12,'SEU-DAILY-003','软毛牙刷','DAILY','软毛牙刷，适用于日常学习与校园生活。请按包装说明使用。'),
(13,'SEU-STATIONERY-003','便利贴组合装','STATIONERY','便利贴组合装，适用于日常学习与校园生活。请按包装说明使用。'),
(14,'SEU-OTHER-003','电脑内胆包14英寸','OTHER','电脑内胆包14英寸，适用于日常学习与校园生活。请按包装说明使用。'),
(15,'SEU-CULTURE-003','东南大学帆布袋','CULTURE','东南大学帆布袋，适用于日常学习与校园生活。请按包装说明使用。'),
(16,'SEU-FOOD-004','即食燕麦片500g','FOOD','即食燕麦片500g，适用于日常学习与校园生活。请按包装说明使用。'),
(17,'SEU-DAILY-004','便携洗漱包','DAILY','便携洗漱包，适用于日常学习与校园生活。请按包装说明使用。'),
(18,'SEU-STATIONERY-004','A4打印纸500张','STATIONERY','A4打印纸500张，适用于日常学习与校园生活。请按包装说明使用。'),
(19,'SEU-OTHER-004','有线耳机','OTHER','有线耳机，适用于日常学习与校园生活。请按包装说明使用。'),
(20,'SEU-CULTURE-004','四牌楼建筑徽章','CULTURE','四牌楼建筑徽章，适用于日常学习与校园生活。请按包装说明使用。'),
(21,'SEU-FOOD-005','苏打饼干300g','FOOD','苏打饼干300g，适用于日常学习与校园生活。请按包装说明使用。'),
(22,'SEU-DAILY-005','收纳盒','DAILY','收纳盒，适用于日常学习与校园生活。请按包装说明使用。'),
(23,'SEU-STATIONERY-005','透明文件袋','STATIONERY','透明文件袋，适用于日常学习与校园生活。请按包装说明使用。'),
(24,'SEU-OTHER-005','桌面手机支架','OTHER','桌面手机支架，适用于日常学习与校园生活。请按包装说明使用。'),
(25,'SEU-CULTURE-005','东南大学纪念笔','CULTURE','东南大学纪念笔，适用于日常学习与校园生活。请按包装说明使用。'),
(26,'SEU-FOOD-006','混合坚果100g','FOOD','混合坚果100g，适用于日常学习与校园生活。请按包装说明使用。'),
(27,'SEU-DAILY-006','纯棉毛巾','DAILY','纯棉毛巾，适用于日常学习与校园生活。请按包装说明使用。'),
(28,'SEU-STATIONERY-006','自动铅笔0.5mm','STATIONERY','自动铅笔0.5mm，适用于日常学习与校园生活。请按包装说明使用。'),
(29,'SEU-OTHER-006','网线2米','OTHER','网线2米，适用于日常学习与校园生活。请按包装说明使用。'),
(30,'SEU-CULTURE-006','校园建筑贴纸套装','CULTURE','校园建筑贴纸套装，适用于日常学习与校园生活。请按包装说明使用。'),
(31,'SEU-FOOD-007','茉莉花茶500mL','FOOD','茉莉花茶500mL，适用于日常学习与校园生活。请按包装说明使用。'),
(32,'SEU-DAILY-007','折叠雨伞','DAILY','折叠雨伞，适用于日常学习与校园生活。请按包装说明使用。'),
(33,'SEU-STATIONERY-007','绘图尺套装','STATIONERY','绘图尺套装，适用于日常学习与校园生活。请按包装说明使用。'),
(34,'SEU-OTHER-007','移动硬盘收纳包','OTHER','移动硬盘收纳包，适用于日常学习与校园生活。请按包装说明使用。'),
(35,'SEU-CULTURE-007','东南大学钥匙扣','CULTURE','东南大学钥匙扣，适用于日常学习与校园生活。请按包装说明使用。'),
(36,'SEU-FOOD-008','矿泉水550mL','FOOD','矿泉水550mL，适用于日常学习与校园生活。请按包装说明使用。'),
(37,'SEU-DAILY-008','不锈钢保温杯','DAILY','不锈钢保温杯，适用于日常学习与校园生活。请按包装说明使用。'),
(38,'SEU-STATIONERY-008','错题整理本','STATIONERY','错题整理本，适用于日常学习与校园生活。请按包装说明使用。'),
(39,'SEU-OTHER-008','笔记本散热支架','OTHER','笔记本散热支架，适用于日常学习与校园生活。请按包装说明使用。'),
(40,'SEU-CULTURE-008','校园风景台历','CULTURE','校园风景台历，适用于日常学习与校园生活。请按包装说明使用。');
UPDATE store_order_items i JOIN products p ON p.id=i.product_id JOIN _vcampus_products m ON p.sku=CONCAT('TEST-SKU-',LPAD(m.n,3,'0'))
SET i.product_name_snapshot=m.new_name WHERE i.product_name_snapshot=CONCAT('扩展测试商品 ',LPAD(m.n,2,'0'));
UPDATE products p JOIN _vcampus_products m ON p.sku=CONCAT('TEST-SKU-',LPAD(m.n,3,'0'))
LEFT JOIN products other ON other.sku=m.new_sku AND other.id<>p.id
SET p.name=m.new_name,p.description=m.description,p.sku=IF(other.id IS NULL,m.new_sku,p.sku)
WHERE p.name=CONCAT('扩展测试商品 ',LPAD(m.n,2,'0'));
UPDATE products SET name='东南大学纪念马克杯' WHERE sku='DEMO-CUP-001' AND name='VCampus纪念马克杯';
UPDATE store_order_items SET product_name_snapshot='东南大学纪念马克杯' WHERE product_name_snapshot='VCampus纪念马克杯';
UPDATE store_coupons SET name=CONCAT('校园购物满',CAST(threshold_amount AS UNSIGNED),'减',CAST(discount_amount AS UNSIGNED),'券') WHERE code REGEXP '^TEST-COUPON-[0-9]{2}$' AND name REGEXP '^测试优惠券 [0-9]+$';
UPDATE store_promotions SET name=CASE promotion_type WHEN 'PERCENT' THEN '开学季九折优惠' WHEN 'THRESHOLD' THEN '开学季满减活动' ELSE '校园生活立减活动' END
WHERE code REGEXP '^TEST-PROMO-[0-9]{2}$' AND name REGEXP '^测试促销 [0-9]+$';
UPDATE store_orders SET shipping_remark='已送达九龙湖校区校园驿站'
WHERE (order_no REGEXP '^TEST-ORDER-[0-9]{4}$' OR order_no REGEXP '^DEMO-ORDER-[0-9]{3}$') AND shipping_remark IN ('已送达测试收货点','已送达九龙湖校区测试点');
UPDATE store_product_reviews r JOIN store_orders o ON o.id=r.order_id
SET r.content=CASE WHEN r.score>=4 THEN '包装完好，使用方便，取货也很及时。' WHEN r.score=3 THEN '整体符合预期，希望包装能再改进一些。' ELSE '使用体验还有改进空间，已向商店反馈。' END
WHERE r.content=CONCAT('扩展测试商品评价，订单 ',o.order_no) OR r.content=CONCAT('demo_student 已购商品评价，订单 ',o.order_no);
UPDATE account_transactions SET remark='校园账户开户充值' WHERE reference_type='EXPANDED_SEED' AND remark='扩展测试账户初始化';

CREATE TEMPORARY TABLE _vcampus_notices (n INT PRIMARY KEY,title VARCHAR(200),content TEXT) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO _vcampus_notices VALUES
(1,'秋季学期选课安排','请在选课开放期间查看课程容量与上课时间，完成选课后及时核对个人课表。如遇时间冲突，请联系开课院系确认。'),
(2,'图书馆读者证办理通知','新生可持有效校园身份证明办理读者服务。借阅前请核对个人联系方式，妥善保管校园卡。'),
(3,'宿舍用电安全提醒','请勿在宿舍使用大功率违章电器，不得私拉电线。离开宿舍前请关闭照明及无需运行的电器。'),
(4,'校园网络维护通知','校园网络维护期间部分网络服务可能短暂中断。请提前保存在线资料，恢复后重新连接。'),
(5,'课程退补选时间安排','需要调整课程的同学，请在退补选开放期间办理。退选前请核对培养计划要求，确认后重新查看课表。'),
(6,'图书借阅与续借须知','请按借阅记录中的到期时间归还图书。需要续借时，请先确认图书状态及是否满足续借条件。'),
(7,'宿舍报修受理安排','宿舍设施出现故障，请提交故障位置、现象和预约时间。涉及漏水或用电安全的情况，请及时联系值班人员。'),
(8,'校园账户使用提醒','请妥善保管校园账户及登录凭据。发现异常交易时，请保留交易记录并联系校园服务人员。'),
(9,'期末考试安排查询通知','请关注课程考试安排，提前核对考试时间、地点与课程名称，按要求携带有效证件参加考试。'),
(10,'四牌楼图书馆开放安排','请以图书馆当日开放安排为准，进入阅览区域保持安静，离馆前带走个人物品。'),
(11,'宿舍卫生检查安排','请各寝室整理个人物品，保持地面、书桌和卫生间清洁，检查结束后及时处理反馈的问题。'),
(12,'个人信息核对通知','请核对姓名、联系方式及紧急联系人信息。学籍档案有误时，请向所属院系提交更正申请。'),
(13,'课程成绩复核申请通知','对课程成绩有疑问的同学，请准备课程名称和具体复核事项，向开课院系申请复核。'),
(14,'九龙湖图书馆阅读推广活动','欢迎参加主题阅读与书目推荐活动。阅读结束后可提交读书笔记，分享学习体会。'),
(15,'水电费缴纳提醒','请及时查看本期水电账单，核对房间及分摊金额。如对费用有疑问，请联系宿舍管理人员。'),
(16,'校园服务维护通知','服务维护期间请避免重复提交同一业务请求。已提交的申请可在恢复后查看处理状态。'),
(17,'实验课程安全须知','进入实验室前请了解仪器使用要求，按指导教师安排开展实验，实验结束后关闭设备并整理场地。'),
(18,'电子资源校外访问指南','校外访问电子资源请通过学校提供的访问渠道认证，遵守数据库使用规则，勿批量下载或共享账号。'),
(19,'离校住宿登记通知','因实习、返乡等原因离校的同学，请如实登记离校时间和返校安排，保持联系方式畅通。'),
(20,'账号密码安全提醒','请勿向他人提供密码或短信验证码。更换密码后，请在自己的设备上重新登录。'),
(21,'学籍信息核对通知','请核对学号、学院、专业与班级等信息，发现差错后联系学籍管理人员核实更正。'),
(22,'自习室预约规则','预约前请核对日期和开放时间，按时到达。不能使用预约时段时，请及时取消，便于其他同学预约。'),
(23,'宿舍公共区域清洁安排','请勿在走廊、楼梯间等公共区域堆放杂物。垃圾请分类投放，保持消防通道畅通。'),
(24,'校园一卡通遗失处理','校园卡遗失后请及时办理挂失，核对近期账户交易。补卡与解挂请按校园卡服务流程办理。');
UPDATE announcements a JOIN _vcampus_notices n ON a.title=CONCAT('扩展测试公告 ',LPAD(n.n,2,'0'))
SET a.title=n.title,a.content=n.content
WHERE a.content=CONCAT('这是第 ',n.n,' 条完整公告正文，用于测试长文本、分页、弹窗和不同模块展示。');
UPDATE announcements SET title='校园服务开放通知',content='欢迎使用校园服务。请核对个人信息，并关注所属院系发布的教学安排。'
WHERE title='虚拟校园系统演示公告' AND content='本公告用于演示公告查询、发布状态和角色可见范围。';
UPDATE announcements SET title='本月宿舍水电费缴纳通知' WHERE title='宿舍水电缴费演示公告';

CREATE TEMPORARY TABLE _vcampus_topics (n INT PRIMARY KEY,topic VARCHAR(180),project VARCHAR(180)) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO _vcampus_topics VALUES
(1,'数据结构','校园二手书流转平台'),
(2,'Java程序设计','自习室使用情况分析'),
(3,'操作系统','宿舍能耗可视化'),
(4,'数据库原理','课程资料共享系统'),
(5,'计算机网络','校园失物招领平台'),
(6,'线性代数','智能垃圾分类识别'),
(7,'概率论与数理统计','图书借阅推荐方法研究'),
(8,'软件工程','校园活动信息聚合'),
(9,'数字逻辑电路','实验室预约系统'),
(10,'计算机组成原理','校园无障碍路线导航'),
(11,'算法设计与分析','学习计划管理工具'),
(12,'Python编程','食堂用餐反馈分析'),
(13,'机器学习','宿舍报修流程优化'),
(14,'数字图像处理','校园植物识别应用'),
(15,'编译原理','社团活动管理平台'),
(16,'离散数学','校园骑行安全提醒'),
(17,'信息检索','教学问答检索系统'),
(18,'软件建模','课堂笔记整理工具'),
(19,'计算机图形学','校园服务评价分析'),
(20,'人工智能','课程知识图谱构建');
UPDATE competitions SET title='大学生程序设计竞赛',description='面向在校学生开展大学生程序设计竞赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 01' AND description='竞赛完整介绍与报名要求 1';
UPDATE competitions SET title='大学生数学建模竞赛',description='面向在校学生开展大学生数学建模竞赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 02' AND description='竞赛完整介绍与报名要求 2';
UPDATE competitions SET title='电子设计竞赛',description='面向在校学生开展电子设计竞赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 03' AND description='竞赛完整介绍与报名要求 3';
UPDATE competitions SET title='创新创业项目路演',description='面向在校学生开展创新创业项目路演，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 04' AND description='竞赛完整介绍与报名要求 4';
UPDATE competitions SET title='校园网络安全挑战赛',description='面向在校学生开展校园网络安全挑战赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 05' AND description='竞赛完整介绍与报名要求 5';
UPDATE competitions SET title='智能车设计竞赛',description='面向在校学生开展智能车设计竞赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 06' AND description='竞赛完整介绍与报名要求 6';
UPDATE competitions SET title='计算机设计大赛',description='面向在校学生开展计算机设计大赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 07' AND description='竞赛完整介绍与报名要求 7';
UPDATE competitions SET title='英语演讲比赛',description='面向在校学生开展英语演讲比赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 08' AND description='竞赛完整介绍与报名要求 8';
UPDATE competitions SET title='数据分析应用竞赛',description='面向在校学生开展数据分析应用竞赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 09' AND description='竞赛完整介绍与报名要求 9';
UPDATE competitions SET title='机器人创意设计竞赛',description='面向在校学生开展机器人创意设计竞赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 10' AND description='竞赛完整介绍与报名要求 10';
UPDATE competitions SET title='绿色低碳科技作品赛',description='面向在校学生开展绿色低碳科技作品赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 11' AND description='竞赛完整介绍与报名要求 11';
UPDATE competitions SET title='校园软件应用设计赛',description='面向在校学生开展校园软件应用设计赛，请在报名截止前完成报名，并留意活动安排。' WHERE title='扩展测试竞赛 12' AND description='竞赛完整介绍与报名要求 12';
UPDATE srtp_records s JOIN _vcampus_topics t ON s.project_code=CONCAT('TEST-SRTP-',LPAD(t.n,3,'0'))
SET s.title=t.project,s.description=CONCAT('研究主题：',t.project,'。通过需求调研、方案设计与原型实现，验证校园服务中的具体应用。')
WHERE s.title=CONCAT('学生创新项目 ',LPAD(t.n,2,'0')) AND s.description='用于测试提交、修改和审核同步。';
UPDATE srtp_records SET review_remark=CASE WHEN status='APPROVED' THEN '研究目标明确，同意立项。' ELSE '请补充技术路线与工作计划。' END WHERE review_remark='扩展测试审核意见';
UPDATE books b JOIN _vcampus_topics t ON CAST(RIGHT(b.isbn,4) AS UNSIGNED)=t.n OR CAST(RIGHT(b.isbn,4) AS UNSIGNED)=t.n+20
SET b.title=CONCAT(t.topic,IF(CAST(RIGHT(b.isbn,4) AS UNSIGNED)<=20,'学习指南','实践案例')),
    b.author='课程资料编写组',b.publisher=NULL,b.description=CONCAT(t.topic,'的知识梳理与实践资料。'),
    b.location=CONCAT('自然科学阅览区',1+MOD(t.n,6),'架')
WHERE b.isbn REGEXP '^978-7-TEST-[0-9]{4}$' AND b.title=CONCAT('扩展测试图书 ',RIGHT(b.isbn,2));
UPDATE study_rooms SET building_name='九龙湖图书馆',description='安静学习区域，请保持座位整洁。' WHERE building_name='测试图书馆';
UPDATE online_resources r JOIN _vcampus_topics t ON r.title=CONCAT('扩展线上资源 ',LPAD(t.n,2,'0'))
SET r.title=CONCAT(t.topic,'资源导航'),r.description=CONCAT('通过图书馆资源导航检索',t.topic,'相关文献。'),
    r.url=CONCAT('https://lib.seu.edu.cn/#resource-',LPAD(t.n,2,'0'))
WHERE r.url=CONCAT('https://example.invalid/vcampus/resource/',t.n);
UPDATE dorm_buildings SET building_name=CONCAT('学生公寓',RIGHT(building_code,1),'号楼'),address='九龙湖校区学生生活区'
WHERE building_code REGEXP '^TEST-D[1-4]$' AND building_name=CONCAT('测试宿舍楼 ',RIGHT(building_code,1));
UPDATE dorm_rooms SET description='四人间，配备书桌、衣柜与独立储物空间。' WHERE description REGEXP '^扩展测试宿舍房间 [0-9]+$';
UPDATE leave_requests SET reason=CASE leave_type WHEN 'ILLNESS' THEN '身体不适，申请外出就诊。' WHEN 'OFF_CAMPUS' THEN '参加校外实践活动，按计划返校。' ELSE '因家庭事务申请短期离校。' END WHERE reason REGEXP '^EXPANDED-SEED-LEAVE-[0-9]{2}$';
UPDATE leave_requests SET review_remark=CASE status WHEN 'APPROVED' THEN '同意申请，请按时返校。' ELSE '请补充事由及相关材料。' END WHERE review_remark='扩展测试审核备注';
UPDATE repair_orders SET description=CASE category WHEN 'WATER' THEN '卫生间水龙头关闭后仍有滴水。' WHEN 'ELECTRICITY' THEN '书桌旁插座接触不良，请检修。' WHEN 'FURNITURE' THEN '衣柜门铰链松动，无法正常关闭。' ELSE '宿舍网络接口连接不稳定。' END WHERE description REGEXP '^EXPANDED-SEED-REPAIR-[0-9]{2}$';
UPDATE classroom_reservations SET purpose='课程学习交流与小组讨论' WHERE purpose REGEXP '^EXPANDED-SEED-CLASSROOM-[0-9]{2}$';
UPDATE classroom_reservations SET review_remark=CASE status WHEN 'APPROVED' THEN '同意使用，请按时归还场地。' ELSE '该时段暂不能安排，请调整申请。' END WHERE review_remark='扩展测试教室审核';
UPDATE late_return_alerts SET note='晚归记录已由值班人员核对。' WHERE note REGEXP '^EXPANDED-SEED-LATE-[0-9]{2}$';
UPDATE dorm_absence_warnings SET note='连续未归记录已通知学生本人核实。' WHERE note REGEXP '^EXPANDED-SEED-ABSENCE-[0-9]{2}$';
UPDATE hygiene_inspections SET issue_description=CASE result WHEN 'PASS' THEN '房间地面和桌面整洁。' ELSE '桌面物品较多，地面需要清理。' END
WHERE issue_description REGEXP '^EXPANDED-SEED-HYGIENE-[0-9]{2}$';
UPDATE dorm_hygiene_item_scores SET deduct_reason='物品未按要求分类摆放。' WHERE deduct_reason='扩展测试扣分原因';
UPDATE dorm_repair_entry_permits SET note=CASE allow_enter WHEN 1 THEN '学生已确认可在约定时间入内检修。' ELSE '请维修人员到达前联系学生。' END
WHERE note LIKE '扩展测试入户许可 %';
UPDATE account_cancellation_requests SET reason='近期仍需使用校园服务，暂时撤回注销申请。'
WHERE reason REGEXP '^EXPANDED-SEED-CANCEL-[0-9]{2}$';
UPDATE accommodation_requests SET reason=CASE request_type WHEN 'TRANSFER' THEN '因学习作息调整申请调换床位。' ELSE '因离校安排申请办理退宿。' END
WHERE reason REGEXP '^EXPANDED-SEED-DORM-REQUEST-[0-9]{2}$';
UPDATE accommodation_requests SET review_remark='请补充相关材料后重新提交。' WHERE review_remark='扩展测试住宿审核';
UPDATE access_records SET door_name='学生公寓东门',note='校园卡通行记录。' WHERE note REGEXP '^EXPANDED-SEED-ACCESS-[0-9]{2}$';
UPDATE dorm_visitor_registrations SET visit_reason='亲友来校探访。' WHERE visit_reason REGEXP '^EXPANDED-SEED-VISITOR-[0-9]{2}$';
UPDATE dorm_visitor_registrations SET audit_remark=CASE audit_status WHEN 'APPROVED' THEN '信息核验通过，请按登记时间来访。' ELSE '访客信息不完整，请补充后重新登记。' END
WHERE audit_remark='扩展测试访客审核';
UPDATE library_pdf_resources p JOIN _vcampus_topics t ON p.title=CONCAT('扩展测试文献 ',LPAD(t.n,2,'0'))
SET p.title=CONCAT(t.topic,'课程参考资料'),p.description=CONCAT(t.topic,'课程的阅读材料与学习提要。'),
    p.file_name=CONCAT('course-reading-',LPAD(t.n,2,'0'),'.pdf'),p.reviewer_name='许文清',
    p.rejection_reason=IF(p.status='REJECTED','文档信息不完整，请补充来源说明。',NULL);
UPDATE library_pdf_downloads SET title=REPLACE(title,'扩展测试文献 ','课程参考资料 '),
    file_name=REPLACE(file_name,'test-paper-','course-reading-'),failure_reason='网络连接中断，请重新下载。'
WHERE failure_reason='扩展测试网络中断' OR title LIKE '扩展测试文献 %';
UPDATE ai_knowledge_chunks SET title=REPLACE(title,'扩展校园知识 ','校园服务指南 '),
    content=CASE MOD(id,6) WHEN 0 THEN '学籍信息由学籍管理人员维护，学生可在个人学籍页面核对档案。'
      WHEN 1 THEN '选课开放期间可在选课中心办理选课和退选，完成后应核对个人课表。'
      WHEN 2 THEN '图书借阅、续借和归还记录可在图书馆模块查询。'
      WHEN 3 THEN '校园商店支持商品查询、购物车、下单、支付和订单追踪。'
      WHEN 4 THEN '宿舍报修需填写故障位置、故障现象和可联系时间。'
      ELSE '账号资料、密码和注销申请可在个人中心办理。' END
WHERE title LIKE '扩展校园知识 %';
UPDATE ai_tool_call_logs SET result_summary=CASE status WHEN 'SUCCEEDED' THEN '请求已处理完成。'
  WHEN 'FAILED' THEN '请求处理失败，请核对条件后重试。' WHEN 'CANCELLED' THEN '请求已取消。' ELSE '请求等待处理。' END
WHERE result_summary LIKE '扩展测试工具调用结果 %';
DROP TEMPORARY TABLE _vcampus_people,_vcampus_courses,_vcampus_products,_vcampus_notices,_vcampus_topics;
COMMIT;
