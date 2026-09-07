package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;

/** 原子保存自动排课方案后的结果。 */
public final class AutoScheduleSaveResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int savedCount;

    public AutoScheduleSaveResult(int savedCount) { this.savedCount = savedCount; }
    public int getSavedCount() { return savedCount; }
}
