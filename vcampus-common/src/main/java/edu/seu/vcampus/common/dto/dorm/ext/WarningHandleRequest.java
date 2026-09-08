package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;

/**
 * 处理一条未归预警。
 *
 * <p>通知辅导员时必须显式给出接收人：V1 的 student_profiles 没有辅导员字段，
 * 系统里不存在学生到辅导员的映射，因此由宿管在处理时指定，而不是自动推导。</p>
 */
public final class WarningHandleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long warningId;
    private final Long teacherUserId;
    private final String note;

    public WarningHandleRequest(long warningId, Long teacherUserId, String note) {
        this.warningId = warningId;
        this.teacherUserId = teacherUserId;
        this.note = note;
    }

    public long getWarningId() { return warningId; }
    /** 通知操作必填；核实操作可为空。 */
    public Long getTeacherUserId() { return teacherUserId; }
    public String getNote() { return note; }
}
