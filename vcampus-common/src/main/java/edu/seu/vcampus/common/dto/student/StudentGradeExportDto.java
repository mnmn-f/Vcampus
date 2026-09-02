package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 成绩导出数据；行和指标均由服务端按会话范围产生。 */
public final class StudentGradeExportDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<StudentGradeDto> items;
    private final StudentGradeMetricsDto metrics;

    public StudentGradeExportDto(List<StudentGradeDto> items,
                                 StudentGradeMetricsDto metrics) {
        this.items = items == null || items.isEmpty()
                ? Collections.<StudentGradeDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<StudentGradeDto>(items));
        this.metrics = metrics;
    }

    public List<StudentGradeDto> getItems() { return items; }
    public StudentGradeMetricsDto getMetrics() { return metrics; }
}
