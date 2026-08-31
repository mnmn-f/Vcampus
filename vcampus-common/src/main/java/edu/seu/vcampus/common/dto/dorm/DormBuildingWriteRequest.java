package edu.seu.vcampus.common.dto.dorm;

import java.io.Serializable;

/** 楼栋新增或修改请求；删除由业务明确禁止。 */
public final class DormBuildingWriteRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long id;
    private final String buildingCode;
    private final String buildingName;
    private final String address;
    private final String genderPolicy;
    private final String status;

    public DormBuildingWriteRequest(long id, String buildingCode, String buildingName,
                                    String address, String genderPolicy, String status) {
        this.id = id; this.buildingCode = buildingCode; this.buildingName = buildingName;
        this.address = address; this.genderPolicy = genderPolicy; this.status = status;
    }

    public DormBuildingWriteRequest(String buildingCode, String buildingName, String address,
                                    String genderPolicy, String status) {
        this(0L, buildingCode, buildingName, address, genderPolicy, status);
    }

    public long getId() { return id; }
    public long getBuildingId() { return id; }
    public String getBuildingCode() { return buildingCode; }
    public String getCode() { return buildingCode; }
    public String getBuildingName() { return buildingName; }
    public String getName() { return buildingName; }
    public String getAddress() { return address; }
    public String getGenderPolicy() { return genderPolicy; }
    public String getStatus() { return status; }
}
