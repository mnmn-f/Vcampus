package edu.seu.vcampus.server.academic.handler;

import edu.seu.vcampus.common.dto.academic.CoursePageDto;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.academic.CourseSaveRequest;
import edu.seu.vcampus.common.dto.academic.CourseRosterRequest;
import edu.seu.vcampus.common.dto.academic.EnrollmentDto;
import edu.seu.vcampus.common.dto.academic.EnrollmentRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleIdRequest;
import edu.seu.vcampus.common.dto.academic.ScheduleSaveRequest;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.StudentScheduleQuery;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.academic.CourseScheduleDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.server.academic.service.AcademicException;
import edu.seu.vcampus.server.academic.service.AcademicService;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.security.SessionContext;

/** 将教务命令适配到同一个服务层，避免每个命令复制鉴权和异常处理。 */
public final class AcademicCommandHandler implements CommandHandler {
    private final String command;
    private final AcademicService service;

    public AcademicCommandHandler(String command, AcademicService service) {
        if (command == null || service == null) {
            throw new IllegalArgumentException("command and service are required");
        }
        this.command = command;
        this.service = service;
    }

    @Override
    public Message handle(Message request, SessionContext session) {
        try {
            Object payload = request == null ? null : request.getPayload();
            if (AcademicCommands.COURSE_LIST.equals(command)) {
                return Message.success(request, service.queryCourses(session,
                        payload == null ? null : require(payload, CourseQuery.class)));
            }
            if (AcademicCommands.COURSE_CREATE.equals(command)) {
                CourseDto result = service.createCourse(session,
                        require(payload, CourseSaveRequest.class));
                return Message.success(request, result);
            }
            if (AcademicCommands.COURSE_UPDATE.equals(command)) {
                CourseDto result = service.updateCourse(session,
                        require(payload, CourseSaveRequest.class));
                return Message.success(request, result);
            }
            if (AcademicCommands.SCHEDULE_CREATE.equals(command)) {
                CourseScheduleDto result = service.createSchedule(session,
                        require(payload, ScheduleSaveRequest.class));
                return Message.success(request, result);
            }
            if (AcademicCommands.SCHEDULE_UPDATE.equals(command)) {
                CourseScheduleDto result = service.updateSchedule(session,
                        require(payload, ScheduleSaveRequest.class));
                return Message.success(request, result);
            }
            if (AcademicCommands.SCHEDULE_DELETE.equals(command)) {
                ScheduleIdRequest value = require(payload, ScheduleIdRequest.class);
                service.deleteSchedule(session, value.getScheduleId());
                return Message.success(request, null);
            }
            if (AcademicCommands.STUDENT_ENROLL.equals(command)) {
                EnrollmentRequest value = require(payload, EnrollmentRequest.class);
                EnrollmentDto result = service.enroll(session, value.getCourseId());
                return Message.success(request, result);
            }
            if (AcademicCommands.STUDENT_DROP.equals(command)) {
                EnrollmentRequest value = require(payload, EnrollmentRequest.class);
                service.drop(session, value.getCourseId());
                return Message.success(request, null);
            }
            if (AcademicCommands.STUDENT_SCHEDULE.equals(command)) {
                StudentScheduleDto result = service.studentSchedule(session,
                        payload == null ? StudentScheduleQuery.all()
                                : require(payload, StudentScheduleQuery.class));
                return Message.success(request, result);
            }
            if (AcademicCommands.TEACHER_COURSES.equals(command)) {
                CoursePageDto result = service.teacherCourses(session,
                        payload == null ? null : require(payload, CourseQuery.class));
                return Message.success(request, result);
            }
            if (AcademicCommands.COURSE_ROSTER.equals(command)) {
                return Message.success(request, service.courseRoster(session,
                        require(payload, CourseRosterRequest.class)));
            }
            return Message.failure(request, ResultCodes.INVALID_INPUT, "不支持的教务操作");
        } catch (AcademicException ex) {
            return Message.failure(request, ex.getResultCode(), ex.getUserMessage());
        } catch (IllegalArgumentException ex) {
            return Message.failure(request, ResultCodes.INVALID_INPUT, "请求参数格式不正确");
        }
    }

    @Override
    public Permission requiredPermission() {
        if (AcademicCommands.COURSE_LIST.equals(command)) {
            return Permission.COURSE_READ;
        }
        if (AcademicCommands.COURSE_CREATE.equals(command)
                || AcademicCommands.COURSE_UPDATE.equals(command)
                || AcademicCommands.SCHEDULE_CREATE.equals(command)
                || AcademicCommands.SCHEDULE_UPDATE.equals(command)
                || AcademicCommands.SCHEDULE_DELETE.equals(command)) {
            return Permission.COURSE_MANAGE;
        }
        if (AcademicCommands.TEACHER_COURSES.equals(command)
                || AcademicCommands.COURSE_ROSTER.equals(command)) {
            return Permission.COURSE_TEACH;
        }
        return Permission.COURSE_ENROLL;
    }

    @Override
    public boolean requiresAuthentication() { return true; }

    private static <T> T require(Object value, Class<T> type) {
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("payload must be " + type.getSimpleName());
        }
        return type.cast(value);
    }
}
