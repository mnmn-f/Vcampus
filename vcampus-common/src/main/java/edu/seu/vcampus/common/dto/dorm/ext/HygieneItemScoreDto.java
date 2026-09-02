package edu.seu.vcampus.common.dto.dorm.ext;

import java.io.Serializable;
import java.math.BigDecimal;

/** 卫生检查的单项得分。五项各 0~20 分，合计 100 分。 */
public final class HygieneItemScoreDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String FLOOR = "FLOOR";
    public static final String DESK = "DESK";
    public static final String BED = "BED";
    public static final String BATHROOM = "BATHROOM";
    public static final String BALCONY = "BALCONY";

    /** 五个检查项的固定顺序；服务端与界面都以此为准。 */
    public static final String[] ITEM_CODES = { FLOOR, DESK, BED, BATHROOM, BALCONY };
    /** 单项满分。 */
    public static final int MAX_ITEM_SCORE = 20;

    private final String itemCode;
    private final BigDecimal score;
    private final String deductReason;

    public HygieneItemScoreDto(String itemCode, BigDecimal score, String deductReason) {
        this.itemCode = itemCode;
        this.score = score;
        this.deductReason = deductReason;
    }

    public String getItemCode() { return itemCode; }
    public BigDecimal getScore() { return score; }
    public String getDeductReason() { return deductReason; }

    /** 检查项的中文名，界面和导出共用一份。 */
    public static String itemName(String code) {
        if (FLOOR.equals(code)) return "地面";
        if (DESK.equals(code)) return "桌面";
        if (BED.equals(code)) return "床铺";
        if (BATHROOM.equals(code)) return "卫生间";
        if (BALCONY.equals(code)) return "阳台";
        return code;
    }

    public String getItemName() { return itemName(itemCode); }
}
