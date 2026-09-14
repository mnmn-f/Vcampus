package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/**
 * 可被通知的辅导员/教师。
 *
 * <p>系统里没有单独的「辅导员」角色，也没有学生到辅导员的映射；通知未归预警时由宿管
 * 从启用中的教师账号里挑一个。和派单选维修员是同一种交互：从名单里选，而不是让人
 * 背用户编号。</p>
 */
public final class DormTeacherDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String displayName;
    private final String username;

    public DormTeacherDto(long userId, String displayName, String username) {
        this.userId = userId;
        this.displayName = displayName;
        this.username = username;
    }

    public long getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getUsername() { return username; }

    /** 下拉里显示成「王老师（wang）」。 */
    public String summary() {
        String name = displayName == null || displayName.trim().isEmpty() ? username : displayName;
        return username == null ? String.valueOf(name) : name + "（" + username + "）";
    }
}
