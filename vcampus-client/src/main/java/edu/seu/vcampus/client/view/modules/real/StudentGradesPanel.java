package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.student.StudentRecordClientService;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Academic-module entry for the student's existing read-only grade service. */
public final class StudentGradesPanel extends JPanel {
    public StudentGradesPanel(final StudentRecordClientService service) {
        super(new BorderLayout());
        if (service == null) throw new IllegalArgumentException("grade service is required");
        setOpaque(false);
        add(new AsyncPagedTable<StudentGradeDto>("我的成绩",
                "按学期查看课程成绩与绩点。", "输入学期编号", null,
                new String[]{"学期", "课程编号", "课程名称", "学分", "成绩", "绩点", "状态"},
                new AsyncPagedTable.Loader<StudentGradeDto>() {
                    @Override public PageSlice<StudentGradeDto> load(int page, String semester,
                            String filter) throws Exception {
                        StudentGradeQuery query = new StudentGradeQuery(semester, null, page,
                                StudentGradeQuery.firstPage().getPageSize());
                        StudentGradePage result = service.getOwnGrades(query);
                        return StudentGradeTableSupport.slice(result);
                    }
                }, new AsyncPagedTable.RowMapper<StudentGradeDto>() {
                    @Override public Object[] values(StudentGradeDto row) {
                        return StudentGradeTableSupport.row(row);
                    }
                }, null), BorderLayout.CENTER);
    }
}
