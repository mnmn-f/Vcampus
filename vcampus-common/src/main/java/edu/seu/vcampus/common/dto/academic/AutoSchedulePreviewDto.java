package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 未写数据库的自动排课候选方案或可读失败说明。 */
public final class AutoSchedulePreviewDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final boolean success;
    private final int totalPenalty;
    private final List<AutoScheduleEntryDto> entries;
    private final List<String> explanations;

    public AutoSchedulePreviewDto(boolean success, int totalPenalty,
            List<AutoScheduleEntryDto> entries, List<String> explanations) {
        this.success = success;
        this.totalPenalty = totalPenalty;
        this.entries = immutable(entries);
        this.explanations = immutable(explanations);
    }

    public boolean isSuccess() { return success; }
    public int getTotalPenalty() { return totalPenalty; }
    public List<AutoScheduleEntryDto> getEntries() { return entries; }
    public List<String> getExplanations() { return explanations; }

    private static <T> List<T> immutable(List<T> values) {
        return values == null || values.isEmpty() ? Collections.<T>emptyList()
                : Collections.unmodifiableList(new ArrayList<T>(values));
    }
}
