package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** AI 管理员的反馈筛选条件。 */
public final class AiFeedbackQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String rating;
    private final String category;
    private final String processStatus;
    private final Long fromTime;
    private final Long toTime;

    public AiFeedbackQuery(String rating, String category, String processStatus,
                           Long fromTime, Long toTime) {
        this.rating = rating; this.category = category; this.processStatus = processStatus;
        this.fromTime = fromTime; this.toTime = toTime;
    }
    public String getRating() { return rating; }
    public String getCategory() { return category; }
    public String getProcessStatus() { return processStatus; }
    public Long getFromTime() { return fromTime; }
    public Long getToTime() { return toTime; }
}
