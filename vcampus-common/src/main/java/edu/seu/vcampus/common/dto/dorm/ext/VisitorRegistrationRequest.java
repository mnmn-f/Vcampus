package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import org.threeten.bp.LocalDateTime;

/**
 * 学生提交的外来人员登记。
 *
 * <p>刻意不含房间号：房间由服务端按提交人的在住记录解析，客户端指定不了，
 * 因此学生无法把来访人登记到别人的宿舍。</p>
 */
public final class VisitorRegistrationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String visitorName;
    private final String visitorIdCard;
    private final String visitorPhone;
    private final String visitReason;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;

    public VisitorRegistrationRequest(String visitorName, String visitorIdCard,
                                      String visitorPhone, String visitReason,
                                      LocalDateTime startAt, LocalDateTime endAt) {
        this.visitorName = visitorName;
        this.visitorIdCard = visitorIdCard;
        this.visitorPhone = visitorPhone;
        this.visitReason = visitReason;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public String getVisitorName() { return visitorName; }
    public String getVisitorIdCard() { return visitorIdCard; }
    public String getVisitorPhone() { return visitorPhone; }
    public String getVisitReason() { return visitReason; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
}
