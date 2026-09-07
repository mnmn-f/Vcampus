package edu.seu.vcampus.common.ai;

import java.io.Serializable;

/** AI 问答中展示的本人竞赛报名与竞赛名称联合视图。 */
public final class AiCompetitionRegistrationView implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String title;
    private final String status;
    private final String registeredAt;
    private final String startAt;

    public AiCompetitionRegistrationView(long id, String title, String status,
            String registeredAt, String startAt) {
        this.id = id; this.title = title; this.status = status;
        this.registeredAt = registeredAt; this.startAt = startAt;
    }

    public long getId() { return id; }
    public long getCompetitionId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getRegisteredAt() { return registeredAt; }
    public String getStartAt() { return startAt; }
}
