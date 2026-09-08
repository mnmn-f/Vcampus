package edu.seu.vcampus.common.dto.campus;

import java.io.Serializable;

/** 统一 ID 命令载荷。 */
public final class CampusIdRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;

    public CampusIdRequest(long id) { this.id = id; }
    public long getId() { return id; }
    public long getRecordId() { return id; }
    public long getReservationId() { return id; }
    public long getCompetitionId() { return id; }
}
