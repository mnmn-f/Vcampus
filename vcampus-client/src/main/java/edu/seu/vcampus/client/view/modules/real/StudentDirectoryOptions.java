package edu.seu.vcampus.client.view.modules.real;

import javax.swing.JComboBox;

/** 学籍表单使用的最小目录；显示名称与档案中的现有中文值保持一致。 */
final class StudentDirectoryOptions {
    private static final String[] COLLEGES = {"请选择", "计算机科学与工程学院", "电气工程学院",
            "电子科学与工程学院", "信息科学与工程学院", "经济管理学院", "机械工程学院", "土木工程学院"};
    private static final String[] ALL_MAJORS = {"请选择", "计算机科学与技术", "软件工程", "信息工程",
            "电子信息工程", "电气工程及其自动化", "工商管理"};
    private static final String[] ALL_CLASSES = {"请选择", "计科2601", "软工2601", "软件工程2601", "电气2601",
            "信工2601", "工商2601", "测试班1", "测试班2", "测试班3", "测试班4"};

    private StudentDirectoryOptions() { }

    static JComboBox<String> collegeBox() { return new JComboBox<String>(COLLEGES); }
    static JComboBox<String> majorBox() { return new JComboBox<String>(ALL_MAJORS); }
    static JComboBox<String> classBox() { return new JComboBox<String>(ALL_CLASSES); }
    static String[] colleges() { return COLLEGES.clone(); }
    static String[] majors() { return ALL_MAJORS.clone(); }
    static String[] classes() { return ALL_CLASSES.clone(); }

    static void majorsFor(JComboBox<String> box, String college) {
        replace(box, majorsFor(college));
    }

    static void classesFor(JComboBox<String> box, String major) {
        replace(box, classesFor(major));
    }

    static String[] majorsFor(String college) {
        if ("电气工程学院".equals(college)) return new String[]{"请选择", "电气工程及其自动化"};
        if ("电子科学与工程学院".equals(college)) return new String[]{"请选择", "电子信息工程"};
        if ("信息科学与工程学院".equals(college)) return new String[]{"请选择", "信息工程"};
        if ("经济管理学院".equals(college)) return new String[]{"请选择", "工商管理"};
        if ("计算机科学与工程学院".equals(college)) return new String[]{"请选择", "计算机科学与技术", "软件工程"};
        return new String[]{"请选择"};
    }

    static String[] classesFor(String major) {
        if ("计算机科学与技术".equals(major)) return new String[]{"请选择", "计科2601", "测试班1"};
        if ("软件工程".equals(major)) return new String[]{"请选择", "软工2601", "软件工程2601", "测试班2"};
        if ("电气工程及其自动化".equals(major)) return new String[]{"请选择", "电气2601", "测试班3"};
        if ("信息工程".equals(major) || "电子信息工程".equals(major)) return new String[]{"请选择", "信工2601", "测试班4"};
        if ("工商管理".equals(major)) return new String[]{"请选择", "工商2601"};
        return new String[]{"请选择"};
    }

    private static void replace(JComboBox<String> box, String[] values) {
        String selected = selected(box); box.removeAllItems();
        for (String value : values) box.addItem(value);
        if (selected != null) box.setSelectedItem(selected);
        if (box.getSelectedIndex() < 0) box.setSelectedIndex(0);
    }

    static String selected(JComboBox<String> box) {
        return box.getSelectedIndex() <= 0 ? null : (String) box.getSelectedItem();
    }
}
