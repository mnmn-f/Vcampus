package edu.seu.vcampus.common.dto.student;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 学籍管理员查看的档案详情；成绩由教务模块单独提供。 */
public final class StudentDetailDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private final StudentProfileDto profile;
    private final List<StudentGradeDto> grades;

    public StudentDetailDto(StudentProfileDto profile, List<StudentGradeDto> grades) {
        this.profile = profile;
        this.grades = Collections.unmodifiableList(new ArrayList<StudentGradeDto>(grades));
    }

    public StudentProfileDto getProfile() { return profile; }
    public List<StudentGradeDto> getGrades() { return grades; }
}
