package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentProfileWriteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import org.threeten.bp.LocalDate;

/** 学籍档案字段的服务端校验。 */
final class StudentProfileValidation {
    private StudentProfileValidation() { }

    static void validate(StudentProfileWriteRequest request) throws StudentRecordException {
        if (request == null || request.getUserId() <= 0) throw invalid("学生账号不存在");
        required(request.getStudentNo(), "学号"); max(request.getStudentNo(), 32, "学号");
        max(request.getCollege(), 120, "学院"); max(request.getMajor(), 120, "专业");
        max(request.getClassName(), 120, "班级"); max(request.getDegreeLevel(), 20, "学历层次");
        max(request.getGender(), 16, "性别"); max(request.getAddress(), 255, "地址");
        max(request.getEmergencyContact(), 100, "紧急联系人"); max(request.getEmergencyPhone(), 32, "紧急联系电话");
        year(request.getEnrollmentYear(), "入学年份"); year(request.getExpectedGraduationYear(), "预计毕业年份");
        if (request.getEnrollmentYear() != null && request.getExpectedGraduationYear() != null
                && request.getExpectedGraduationYear() < request.getEnrollmentYear()) throw invalid("预计毕业年份不能早于入学年份");
        if (request.getBirthDate() != null && request.getBirthDate().isAfter(LocalDate.now())) throw invalid("出生日期不能晚于今天");
        if (request.getStatus() == null) throw invalid("请选择学籍状态");
    }

    private static void year(Integer value, String name) throws StudentRecordException {
        if (value != null && (value < 1900 || value > 2200)) throw invalid(name + "不在有效范围内");
    }

    private static void max(String value, int length, String name) throws StudentRecordException {
        StudentServiceSupport.maxLength(value, length, name);
    }

    private static void required(String value, String name) throws StudentRecordException {
        StudentServiceSupport.required(value, name);
    }

    static StudentRecordException invalid(String message) {
        return new StudentRecordException(ResultCodes.INVALID_INPUT, message);
    }
}
