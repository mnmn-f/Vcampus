package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.common.dto.dorm.DormBedDto;

/** 平面图与分配表单共用的一句床位描述，避免两处各拼一遍拼得不一样。 */
final class RealUiPlanText {
    private RealUiPlanText() {
    }

    static String bedLabel(DormBedDto bed) {
        if (bed == null) return "—";
        String no = bed.getBedNo() == null ? "?" : bed.getBedNo().trim();
        return no + " 号床（编号 " + bed.getId() + "）";
    }
}
