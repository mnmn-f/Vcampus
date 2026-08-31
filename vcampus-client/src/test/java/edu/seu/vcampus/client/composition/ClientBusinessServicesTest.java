package edu.seu.vcampus.client.composition;

import edu.seu.vcampus.client.network.ClientGateway;
import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.academic.StudentScheduleDto;
import edu.seu.vcampus.common.dto.academic.CourseDto;
import edu.seu.vcampus.common.dto.auth.LoginResult;
import edu.seu.vcampus.common.dto.library.BookDetail;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.dorm.DormBuildingDto;
import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.UtilityBillDto;
import edu.seu.vcampus.common.dto.dorm.UtilityBillQuery;
import edu.seu.vcampus.common.dto.student.StudentProfileDto;
import edu.seu.vcampus.common.dto.student.StudentStatus;
import edu.seu.vcampus.common.dto.store.ProductPage;
import edu.seu.vcampus.common.dto.store.ProductDto;
import edu.seu.vcampus.common.protocol.Message;
import edu.seu.vcampus.common.protocol.command.AcademicCommands;
import edu.seu.vcampus.common.protocol.command.DormCommands;
import edu.seu.vcampus.common.protocol.command.LibraryCommands;
import edu.seu.vcampus.common.protocol.command.StoreCommands;
import edu.seu.vcampus.common.protocol.command.StudentCommands;
import edu.seu.vcampus.common.security.Role;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** 三类网络业务服务共享同一请求边界和当前会话令牌。 */
public final class ClientBusinessServicesTest {
    @Test
    public void allBusinessServicesUseTheSameSessionToken() throws Exception {
        RecordingGateway gateway = new RecordingGateway();
        NetworkClientService network = new NetworkClientService(gateway);
        ClientSession session = new ClientSession();
        session.open(new LoginResult(7L, "student", "学生", Role.STUDENT, "token-7"));
        ClientBusinessServices services = new ClientBusinessServices(network, session);

        gateway.payload = new StudentProfileDto(7L, "学生", "student", "S007", "学院", "专业",
                "班级", 2026, 2030, null, null, null, null, null, null, StudentStatus.ENROLLED);
        services.student().getOwnProfile();
        gateway.payload = new StudentScheduleDto(7L, Collections.<CourseDto>emptyList());
        services.academic().studentSchedule();
        gateway.payload = new PageResult<BookDetail>(Collections.<BookDetail>emptyList(), 1, 20, 0);
        services.library().searchBooks(null);
        gateway.payload = new ProductPage(Collections.<ProductDto>emptyList(), 1, 20, 0);
        services.store().searchProducts(null);
        gateway.payload = new DormPage<DormBuildingDto>(1, 20, 0, Collections.<DormBuildingDto>emptyList());
        services.dorm().buildings(null);
        gateway.payload = new DormPage<UtilityBillDto>(1, 20, 0, Collections.<UtilityBillDto>emptyList());
        services.dorm().managerBills(UtilityBillQuery.all());

        assertEquals(6, gateway.commands.size());
        assertEquals(StudentCommands.SELF_PROFILE, gateway.commands.get(0));
        assertEquals(AcademicCommands.STUDENT_SCHEDULE, gateway.commands.get(1));
        assertEquals(LibraryCommands.BOOK_SEARCH, gateway.commands.get(2));
        assertEquals(StoreCommands.PRODUCT_SEARCH, gateway.commands.get(3));
        assertEquals(DormCommands.BUILDING_LIST, gateway.commands.get(4));
        assertEquals(DormCommands.UTILITY_MANAGER_LIST, gateway.commands.get(5));
        assertEquals("token-7", gateway.tokens.get(0));
        assertEquals("token-7", gateway.tokens.get(1));
        assertEquals("token-7", gateway.tokens.get(2));
        assertEquals("token-7", gateway.tokens.get(3));
        assertEquals("token-7", gateway.tokens.get(4));
        assertEquals("token-7", gateway.tokens.get(5));
    }

    private static final class RecordingGateway implements ClientGateway {
        private final List<String> commands = new ArrayList<String>();
        private final List<String> tokens = new ArrayList<String>();
        private Object payload;

        @Override public Message send(Message request) {
            commands.add(request.getCommand()); tokens.add(request.getSessionToken());
            return Message.success(request, (java.io.Serializable) payload);
        }
        @Override public boolean isConnected() { return true; }
        @Override public void close() { }
    }
}
