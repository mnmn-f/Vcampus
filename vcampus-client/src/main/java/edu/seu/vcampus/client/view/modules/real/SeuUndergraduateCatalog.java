package edu.seu.vcampus.client.view.modules.real;

import java.util.LinkedHashMap;
import java.util.Map;

/** 院系和本科专业来源与口径见 docs/SEU_DATA_SOURCES.md；不包含研究生院或成贤学院。 */
final class SeuUndergraduateCatalog {
    private static final Map<String,String[]> ENTRIES = new LinkedHashMap<>();
    static {
        add("建筑学院", "建筑学", "城乡规划", "风景园林");
        add("机械工程学院", "机械工程", "工业工程", "智能车辆工程");
        add("能源与环境学院", "能源与动力工程", "建筑环境与能源应用工程", "环境工程", "核工程与核技术", "新能源科学与工程");
        add("信息科学与工程学院", "信息工程", "海洋信息工程");
        add("土木工程学院", "土木工程", "工程管理", "工程力学", "给排水科学与工程", "智能建造");
        add("电子科学与工程学院", "电子科学与技术");
        add("数学学院", "数学与应用数学", "信息与计算科学", "统计学");
        add("自动化学院", "自动化", "机器人工程");
        add("计算机科学与工程学院", "计算机科学与技术");
        add("软件学院", "软件工程");
        add("物理学院", "物理学", "应用物理学");
        add("生物科学与医学工程学院", "生物医学工程", "生物信息学", "智能医学工程");
        add("材料科学与工程学院", "材料科学与工程");
        add("人文学院", "政治学与行政学", "旅游管理", "社会学", "汉语言文学", "哲学");
        add("经济管理学院", "工商管理", "国际经济与贸易", "信息管理与信息系统", "会计学", "金融学", "经济学", "电子商务", "物流管理", "金融工程");
        add("电气工程学院", "电气工程及其自动化", "电动载运工程");
        add("外国语学院", "英语", "日语", "俄语");
        add("化学化工学院", "化学工程与工艺", "制药工程", "化学");
        add("交通学院", "交通工程", "交通运输", "测绘工程", "港口航道与海岸工程", "城市地下空间工程", "道路桥梁与渡河工程", "智慧交通");
        add("仪器科学与工程学院", "测控技术与仪器", "智能感知工程");
        add("艺术学院", "动画", "美术学", "产品设计", "艺术史论");
        add("法学院", "法学");
        add("医学院", "临床医学", "医学影像学", "医学检验技术", "基础医学");
        add("公共卫生学院", "预防医学", "劳动与社会保障");
        add("网络空间安全学院", "网络空间安全", "密码科学与技术");
        add("生命科学与技术学院", "生物科学", "生物工程");
        add("人工智能学院", "人工智能");
        add("未来技术学院", "未来机器人");
        add("集成电路学院", "集成电路设计与集成系统");
    }
    private SeuUndergraduateCatalog() { }
    private static void add(String college,String... majors) { ENTRIES.put(college,majors); }
    static String[] colleges() { return ENTRIES.keySet().toArray(new String[0]); }
    static String[] majors(String college) { String[] values=ENTRIES.get(college);return values==null?new String[0]:values.clone(); }
    static String[] allMajors() { return ENTRIES.values().stream().flatMap(java.util.Arrays::stream).distinct().toArray(String[]::new); }
}
