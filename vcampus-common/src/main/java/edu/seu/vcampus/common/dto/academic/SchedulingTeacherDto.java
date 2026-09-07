package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 自动排课配置页使用的教师选项。 */
public final class SchedulingTeacherDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long userId;
    private final String displayName;
    private final String employeeNo;

    public SchedulingTeacherDto(long userId, String displayName, String employeeNo) {
        this.userId = userId;
        this.displayName = displayName;
        this.employeeNo = employeeNo;
    }

    public long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmployeeNo() { return employeeNo; }

    @Override public String toString() {
        String name = displayName == null ? "教师" : displayName;
        return employeeNo == null ? name : name + "（" + employeeNo + "）";
    }
}
