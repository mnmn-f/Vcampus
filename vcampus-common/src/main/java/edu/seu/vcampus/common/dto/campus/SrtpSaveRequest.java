package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Locale;

/** SRTP 项目记录维护请求；服务端按 activeRole 决定是否采用 studentUserId。 */
public final class SrtpSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Long id;
    private final String projectCode;
    private final Long studentUserId;
    private final String title;
    private final String description;
    private final BigDecimal credits;
    private final String status;

    public SrtpSaveRequest(Long id, String projectCode, String title, String description,
                           BigDecimal credits, String status) {
        this(id, projectCode, null, title, description, credits, status);
    }

    public SrtpSaveRequest(Long id, String projectCode, Long studentUserId, String title,
                           String description, BigDecimal credits, String status) {
        this.id = id;
        this.projectCode = projectCode;
        this.studentUserId = studentUserId;
        this.title = title;
        this.description = description;
        this.credits = credits;
        this.status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    public static SrtpSaveRequest selfCreate(String code, String title, String description,
            BigDecimal credits) {
        return new SrtpSaveRequest(null, code, title, description, credits, "SUBMITTED");
    }

    public static SrtpSaveRequest adminCreate(String code, long studentId, String title,
            String description, BigDecimal credits, String status) {
        return new SrtpSaveRequest(null, code, Long.valueOf(studentId), title, description,
                credits, status);
    }

    public Long getId() { return id; }
    public Long getRecordId() { return id; }
    public String getProjectCode() { return projectCode; }
    public Long getStudentUserId() { return studentUserId; }
    public Long getStudentId() { return studentUserId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getCredits() { return credits; }
    public String getStatus() { return status; }
    public boolean isUpdate() { return id != null && id.longValue() > 0L; }
}
