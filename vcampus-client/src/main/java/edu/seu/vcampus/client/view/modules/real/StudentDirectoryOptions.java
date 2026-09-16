package edu.seu.vcampus.client.view.modules.real;

import javax.swing.JComboBox;

/** 学院与专业来自校方目录，班级名称用于本项目的模拟档案。 */
final class StudentDirectoryOptions {
    private StudentDirectoryOptions() { }
    static JComboBox<String> collegeBox() { return new JComboBox<>(colleges()); }
    static JComboBox<String> majorBox() { return new JComboBox<>(majors()); }
    static JComboBox<String> classBox() { return new JComboBox<>(classes()); }
    static String[] colleges() { return choices(SeuUndergraduateCatalog.colleges()); }
    static String[] majors() { return choices(SeuUndergraduateCatalog.allMajors()); }
    static String[] classes() {
        java.util.List<String> result = new java.util.ArrayList<>(); result.add("请选择");
        for(String major:SeuUndergraduateCatalog.allMajors())
            result.addAll(java.util.Arrays.asList(classesFor(major)).subList(1,5));
        return result.toArray(new String[0]);
    }
    static void majorsFor(JComboBox<String> box,String college) { replace(box,majorsFor(college)); }
    static void classesFor(JComboBox<String> box,String major) { replace(box,classesFor(major)); }
    static String majorForClass(String className) {
        if (className == null) return null;
        String found = null;
        for (String major : SeuUndergraduateCatalog.allMajors()) {
            if (className.startsWith(major) && (found == null || major.length() > found.length())) {
                found = major;
            }
        }
        return found;
    }
    static String collegeForMajor(String major) {
        if (major == null) return null;
        for (String college : SeuUndergraduateCatalog.colleges()) {
            for (String candidate : SeuUndergraduateCatalog.majors(college)) {
                if (major.equals(candidate)) return college;
            }
        }
        return null;
    }
    static Integer enrollmentYearForClass(String className) {
        if (className == null) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(20[0-9]{2})级(?:[0-9]+)班$").matcher(className);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
    static String[] majorsFor(String college) { return choices(SeuUndergraduateCatalog.majors(college)); }
    static String[] classesFor(String major) {
        if (major==null || !java.util.Arrays.asList(SeuUndergraduateCatalog.allMajors()).contains(major)) return new String[]{"请选择"};
        return new String[]{"请选择",major+"2026级1班",major+"2026级2班",major+"2024级1班",major+"2024级2班"};
    }
    private static String[] choices(String[] items) {
        String[] values=new String[items.length+1];values[0]="请选择";System.arraycopy(items,0,values,1,items.length);return values;
    }
    private static void replace(JComboBox<String> box,String[] values) {
        String current=selected(box);box.removeAllItems();
        for(String value:values)box.addItem(value);
        for(int i=1;i<values.length;i++) if(values[i].equals(current)) {box.setSelectedIndex(i);return;}
        box.setSelectedIndex(0);
    }
    static String selected(JComboBox<String> box) { return box.getSelectedIndex()<=0?null:(String)box.getSelectedItem(); }
}
