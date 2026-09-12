package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;

/** 待建档学生账号检索条件。 */
public final class StudentAccountCandidateQuery implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_PAGE_SIZE = 100;
    public static final int MAX_PAGE_SIZE = 100;
    private final String keyword;
    private final int page;
    private final int pageSize;

    public StudentAccountCandidateQuery(String keyword, int page, int pageSize) {
        this.keyword = clean(keyword);
        this.page = page < 1 ? 1 : page;
        this.pageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
    }

    public static StudentAccountCandidateQuery firstPage() {
        return new StudentAccountCandidateQuery(null, 1, DEFAULT_PAGE_SIZE);
    }

    public String getKeyword() { return keyword; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getOffset() { return (page - 1) * pageSize; }

    private static String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}
