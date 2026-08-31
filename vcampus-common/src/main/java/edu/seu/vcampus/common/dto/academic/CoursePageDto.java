package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 分页课程结果。 */
public final class CoursePageDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final List<CourseDto> items;

    public CoursePageDto(int pageNumber, int pageSize, long totalElements,
                         List<CourseDto> items) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.items = items == null || items.isEmpty()
                ? Collections.<CourseDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<CourseDto>(items));
    }

    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public long getTotal() { return totalElements; }
    public List<CourseDto> getItems() { return items; }
    public List<CourseDto> getCourses() { return items; }

    public int getTotalPages() {
        return totalElements == 0 ? 0
                : (int) ((totalElements + pageSize - 1L) / pageSize);
    }
}
