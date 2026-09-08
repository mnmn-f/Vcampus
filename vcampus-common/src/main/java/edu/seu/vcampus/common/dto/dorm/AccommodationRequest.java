package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 学生提交入住、调宿或退宿申请；身份由服务端会话提供。 */
public final class AccommodationRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String requestType;
    private final Long currentRecordId;
    private final Long requestedBedId;
    private final String reason;

    public AccommodationRequest(String requestType, Long currentRecordId,
                                 Long requestedBedId, String reason) {
        this.requestType = requestType;
        this.currentRecordId = currentRecordId;
        this.requestedBedId = requestedBedId;
        this.reason = reason;
    }

    public AccommodationRequest(DormRequestType type, Long currentRecordId,
                                 Long requestedBedId, String reason) {
        this(type == null ? null : type.name(), currentRecordId, requestedBedId, reason);
    }

    public String getRequestType() { return requestType; }
    public Long getCurrentRecordId() { return currentRecordId; }
    public Long getRequestedBedId() { return requestedBedId; }
    public String getReason() { return reason; }
}
