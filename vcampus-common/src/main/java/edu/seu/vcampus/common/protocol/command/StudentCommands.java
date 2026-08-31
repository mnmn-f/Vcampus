package edu.seu.vcampus.common.protocol.command;

/** 学籍与成绩模块的命令字唯一登记处。 */
public final class StudentCommands {
    public static final String SELF_PROFILE = "student.self-profile";
    public static final String SELF_GRADES = "student.self-grades";
    public static final String PROFILE_SEARCH = "student.profile.search";
    public static final String PROFILE_DETAIL = "student.profile.detail";
    public static final String PROFILE_CREATE = "student.profile.create";
    public static final String PROFILE_UPDATE = "student.profile.update";
    public static final String GRADE_RECORD = "student.grade.record";
    public static final String GRADE_REVIEW = "student.grade.review";

    private StudentCommands() {
    }
}
