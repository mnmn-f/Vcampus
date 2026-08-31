package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 成绩分页结果。 */
public final class StudentGradePage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<StudentGradeDto> items;
    private final long total;
    private final int page;
    private final int pageSize;

    public StudentGradePage(List<StudentGradeDto> items, long total,
                            int page, int pageSize) {
        this.items = Collections.unmodifiableList(new ArrayList<StudentGradeDto>(items));
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<StudentGradeDto> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public boolean hasNext() { return ((long) page * pageSize) < total; }
}
