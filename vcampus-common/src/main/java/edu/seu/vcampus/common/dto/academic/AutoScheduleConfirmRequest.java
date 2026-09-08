package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 教务管理员确认保存此前预览的候选方案。 */
public final class AutoScheduleConfirmRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<AutoScheduleEntryDto> entries;

    public AutoScheduleConfirmRequest(List<AutoScheduleEntryDto> entries) {
        this.entries = entries == null ? Collections.<AutoScheduleEntryDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<AutoScheduleEntryDto>(entries));
    }

    public List<AutoScheduleEntryDto> getEntries() { return entries; }
}
