package edu.seu.vcampus.client.service.academic;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseRosterDto;
import edu.seu.vcampus.common.dto.academic.CourseRosterRequest;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleIdRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.academic.StudentEnrollmentListDto;
import edu.seu.vcampus.common.dto.academic.AutoScheduleConfirmRequest;
import edu.seu.vcampus.common.dto.academic.AutoSchedulePreviewDto;
import edu.seu.vcampus.common.dto.academic.AutoScheduleRequest;
import edu.seu.vcampus.common.dto.academic.AutoScheduleSaveResult;
import edu.seu.vcampus.common.dto.academic.SchedulingOverviewDto;
import edu.seu.vcampus.common.dto.academic.TeacherTimePreferenceDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;

/** 教务客户端网络边界；不包含 Swing 页面或业务规则。 */
public final class AcademicClientService {
    private final NetworkClientService network;
    private final ClientSession session;

    public AcademicClientService(NetworkClientService network) {
        this(network, null);
    }

    public AcademicClientService(NetworkClientService network, ClientSession session) {
        if (network == null) {
            throw new IllegalArgumentException("network is required");
        }
        this.network = network;
        this.session = session;
    }

    public CoursePageDto queryCourses(CourseQuery query) throws NetworkClientException {
        return payload(AcademicCommands.COURSE_LIST, query, CoursePageDto.class);
    }

    public CoursePageDto listCourses(CourseQuery query) throws NetworkClientException {
        return queryCourses(query);
    }

    public CourseDto createCourse(CourseSaveRequest request) throws NetworkClientException {
        return payload(AcademicCommands.COURSE_CREATE, request, CourseDto.class);
    }

    public CourseDto updateCourse(CourseSaveRequest request) throws NetworkClientException {
        return payload(AcademicCommands.COURSE_UPDATE, request, CourseDto.class);
    }

    public CourseDto saveCourse(CourseSaveRequest request) throws NetworkClientException {
        return request != null && request.isUpdate()
                ? updateCourse(request) : createCourse(request);
    }

    public CourseScheduleDto createSchedule(ScheduleSaveRequest request)
            throws NetworkClientException {
        return payload(AcademicCommands.SCHEDULE_CREATE, request, CourseScheduleDto.class);
    }

    public CourseScheduleDto updateSchedule(ScheduleSaveRequest request)
            throws NetworkClientException {
        return payload(AcademicCommands.SCHEDULE_UPDATE, request, CourseScheduleDto.class);
    }

    public CourseScheduleDto saveSchedule(ScheduleSaveRequest request)
            throws NetworkClientException {
        return request != null && request.isUpdate()
                ? updateSchedule(request) : createSchedule(request);
    }

    public void deleteSchedule(long scheduleId) throws NetworkClientException {
        request(AcademicCommands.SCHEDULE_DELETE, new ScheduleIdRequest(scheduleId));
    }

    public EnrollmentDto enroll(long courseId) throws NetworkClientException {
        return payload(AcademicCommands.STUDENT_ENROLL,
                new EnrollmentRequest(courseId), EnrollmentDto.class);
    }

    public void drop(long courseId) throws NetworkClientException {
        request(AcademicCommands.STUDENT_DROP, new EnrollmentRequest(courseId));
    }

    public StudentScheduleDto studentSchedule() throws NetworkClientException {
        return studentSchedule(StudentScheduleQuery.all());
    }

    public StudentScheduleDto studentSchedule(StudentScheduleQuery query)
            throws NetworkClientException {
        return payload(AcademicCommands.STUDENT_SCHEDULE, query, StudentScheduleDto.class);
    }

    public StudentEnrollmentListDto studentEnrollments() throws NetworkClientException {
        return payload(AcademicCommands.STUDENT_ENROLLMENTS, null,
                StudentEnrollmentListDto.class);
    }

    public CoursePageDto teacherCourses(CourseQuery query) throws NetworkClientException {
        return payload(AcademicCommands.TEACHER_COURSES, query, CoursePageDto.class);
    }

    public CourseRosterDto courseRoster(long courseId) throws NetworkClientException {
        return payload(AcademicCommands.COURSE_ROSTER, new CourseRosterRequest(courseId),
                CourseRosterDto.class);
    }

    public SchedulingOverviewDto schedulingOverview() throws NetworkClientException {
        return payload(AcademicCommands.SCHEDULING_OVERVIEW, null, SchedulingOverviewDto.class);
    }
    public TeacherTimePreferenceDto saveTimePreference(TeacherTimePreferenceDto value) throws NetworkClientException {
        return payload(AcademicCommands.SCHEDULING_PREFERENCE_SAVE, value, TeacherTimePreferenceDto.class);
    }
    public void deleteTimePreference(long id) throws NetworkClientException {
        request(AcademicCommands.SCHEDULING_PREFERENCE_DELETE, new ScheduleIdRequest(id));
    }
    public AutoSchedulePreviewDto previewAutoSchedule(int timeLimitMillis) throws NetworkClientException {
        return payload(AcademicCommands.AUTO_SCHEDULE_PREVIEW, new AutoScheduleRequest(timeLimitMillis), AutoSchedulePreviewDto.class);
    }
    public AutoScheduleSaveResult confirmAutoSchedule(AutoScheduleConfirmRequest request) throws NetworkClientException {
        return payload(AcademicCommands.AUTO_SCHEDULE_CONFIRM, request, AutoScheduleSaveResult.class);
    }

    private <T> T payload(String command, java.io.Serializable body, Class<T> type)
            throws NetworkClientException {
        Message response = request(command, body);
        Object result = response.getPayload();
        if (!type.isInstance(result)) {
            throw new NetworkClientException(ResultCodes.INTERNAL_ERROR,
                    "教务响应格式不正确");
        }
        return type.cast(result);
    }

    private Message request(String command, java.io.Serializable body)
            throws NetworkClientException {
        syncSessionToken();
        return network.request(command, body);
    }

    private void syncSessionToken() {
        if (session != null) {
            network.setSessionToken(session.isAuthenticated()
                    ? session.getSessionToken() : null);
        }
    }
}
