package edu.seu.vcampus.common.dto.academic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Current and historical enrollments for the authenticated student. */
public final class StudentEnrollmentListDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<StudentEnrollmentDto> items;

    public StudentEnrollmentListDto(List<StudentEnrollmentDto> items) {
        this.items = items == null || items.isEmpty()
                ? Collections.<StudentEnrollmentDto>emptyList()
                : Collections.unmodifiableList(new ArrayList<StudentEnrollmentDto>(items));
    }

    public List<StudentEnrollmentDto> getItems() { return items; }
}
