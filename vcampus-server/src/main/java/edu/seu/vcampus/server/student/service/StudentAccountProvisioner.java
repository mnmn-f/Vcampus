package edu.seu.vcampus.server.student.service;

import edu.seu.vcampus.common.dto.student.StudentProfileCreateRequest;
import edu.seu.vcampus.server.security.SessionContext;
import java.sql.Connection;

/** 在学籍事务内创建可登录的学生账号。 */
public interface StudentAccountProvisioner {
    long create(Connection connection, SessionContext operator,
                StudentProfileCreateRequest request) throws StudentRecordException;
}
