package edu.seu.vcampus.client.service.student;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.student.StudentGradeExportDto;
import edu.seu.vcampus.common.dto.student.StudentGradeExportQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeDto;
import edu.seu.vcampus.common.dto.student.StudentGradeMetricsDto;
import edu.seu.vcampus.common.dto.student.StudentGradePage;
import edu.seu.vcampus.common.dto.student.StudentGradeQuery;
import edu.seu.vcampus.common.dto.student.StudentGradeReportDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** 成绩指标与导出客户端使用当前会话和强类型响应。 */
public final class NetworkStudentGradeInsightsClientServiceTest {
    @Test
    public void reportAndExportUseStableCommandsAndTermPayloads() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkStudentRecordClientService service = new NetworkStudentRecordClientService(
                new NetworkClientService(gateway), session());
        StudentGradeMetricsDto metrics = new StudentGradeMetricsDto("2026-FALL", 1, 1,
                new BigDecimal("3.00"), new BigDecimal("4.50"), new BigDecimal("4.50"),
                new BigDecimal("95.00"), new BigDecimal("95.00"));
        StudentGradePage page = new StudentGradePage(
                Collections.<StudentGradeDto>emptyList(),
                0, 1, 20);
        StudentGradeReportDto report = new StudentGradeReportDto(page, metrics);
        gateway.payload = report;
        StudentGradeQuery query = new StudentGradeQuery("2026-FALL", null, 1, 20);
        assertSame(report, service.getOwnGradeReport(query));
        assertEquals(StudentCommands.SELF_GRADE_REPORT, gateway.request.getCommand());
        assertSame(query, gateway.request.getPayload());

        StudentGradeExportDto export = new StudentGradeExportDto(
                Collections.<StudentGradeDto>emptyList(), metrics);
        gateway.payload = export;
        StudentGradeExportQuery exportQuery = new StudentGradeExportQuery("2026-FALL", null);
        assertSame(export, service.exportOwnGrades(exportQuery));
        assertEquals(StudentCommands.SELF_GRADE_EXPORT, gateway.request.getCommand());
        assertSame(exportQuery, gateway.request.getPayload());
        assertEquals("token-7", gateway.request.getSessionToken());
    }

    private static ClientSession session() {
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        return session;
    }

    private static final class RecordingGateway implements ClientGateway {
        private Message request;
        private Object payload;

        @Override public Message send(Message value) {
            request = value; return Message.success(value, (java.io.Serializable) payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
