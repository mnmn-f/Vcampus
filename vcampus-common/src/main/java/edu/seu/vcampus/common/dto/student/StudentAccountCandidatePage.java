package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 待建档学生账号分页结果。 */
public final class StudentAccountCandidatePage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<StudentAccountCandidateDto> items;
    private final long total;
    private final int page;
    private final int pageSize;

    public StudentAccountCandidatePage(List<StudentAccountCandidateDto> items, long total,
                                       int page, int pageSize) {
        this.items = Collections.unmodifiableList(new ArrayList<StudentAccountCandidateDto>(items));
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<StudentAccountCandidateDto> getItems() { return items; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public boolean hasNext() { return ((long) page * pageSize) < total; }
}
