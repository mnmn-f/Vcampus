package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 提交一次卫生检查：只报分项，总分与等级由服务端算，避免客户端算错或造假。 */
public final class HygieneScoreSubmitRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long roomId;
    private final List<HygieneItemScoreDto> items;
    private final String issueDescription;

    public HygieneScoreSubmitRequest(long roomId, List<HygieneItemScoreDto> items,
                                     String issueDescription) {
        this.roomId = roomId;
        this.items = items == null
                ? Collections.<HygieneItemScoreDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<HygieneItemScoreDto>(items));
        this.issueDescription = issueDescription;
    }

    public long getRoomId() { return roomId; }
    public List<HygieneItemScoreDto> getItems() { return items; }
    public String getIssueDescription() { return issueDescription; }
}
