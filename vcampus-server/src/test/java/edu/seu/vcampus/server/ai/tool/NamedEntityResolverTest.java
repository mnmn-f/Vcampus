package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.StudyRoomView;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.router.CommandHandler;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Test;
import org.threeten.bp.LocalTime;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.assertTrue;

public final class NamedEntityResolverTest {
    @Test public void fillsUniqueOpenStudyRoomWithoutLeakingSearchResult() {
        CommandRouter router = new CommandRouter();
        router.register(LibraryCommands.STUDY_ROOM_SEARCH, new CommandHandler() {
            public Message handle(Message request, SessionContext session) {
                StudyRoomView room = new StudyRoomView(7L, "图书馆", "研习室A", 8,
                        LocalTime.of(8, 0), LocalTime.of(22, 0), "OPEN", "测试");
                return Message.success(request, new PageResult<StudyRoomView>(
                        Arrays.asList(room), 1, 20, 1));
            }
            public Permission requiredPermission() { return null; }
            public boolean requiresAuthentication() { return false; }
        });
        AiToolRegistry tools = AiToolRegistry.campusDefaults();
        NamedEntityResolver resolver = new NamedEntityResolver(tools, new ToolBridge(router));
        SessionContext session = new SessionContext("token", 1L, "student", "学生",
                EnumSet.of(Role.STUDENT), Role.STUDENT);
        AiToolInvocation resolved = resolver.resolve(new AiToolInvocation(
                "library.study-room.reserve",
                "{\"startAt\":\"2026-11-11T09:00\",\"endAt\":\"2026-11-11T12:00\"}",
                "预约自习室"), session);
        assertTrue(resolved.getArgumentsJson().contains("\"roomId\":7"));
        assertTrue(resolved.getSummary().contains("图书馆 研习室A"));
    }
}
