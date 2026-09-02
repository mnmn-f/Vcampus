package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** 宿舍公告扩展字段校验与管理权限。 */
public final class DormNoticeValidationTest {
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;

    @Before public void setUp() {
        InMemoryDormExtRepository r = new InMemoryDormExtRepository(); r.addNotice(1L, "公告", "PUBLISHED");
        service = new DormExtService(r); manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
        student = DormExtTestSupport.session(11L, Role.STUDENT);
    }

    @Test public void scopeRulesRejectMissingOrConflictingTargets() {
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, req(NoticeExtraDto.SCOPE_BUILDING, null, null));
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, req(NoticeExtraDto.SCOPE_ROOM, null, null));
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, req(NoticeExtraDto.SCOPE_ALL, 1L, null));
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, req(NoticeExtraDto.SCOPE_BUILDING, 1L, 10L));
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, req("CAMPUS", null, null));
    }
    @Test public void typeAndNoticeMustExist() {
        expect(DormExtCommands.INVALID_INPUT, new NoticeExtraRequest(1L, "GOSSIP", NoticeExtraDto.SCOPE_ALL, null, null, false));
        expect(DormExtCommands.NOTICE_NOT_FOUND, new NoticeExtraRequest(999L, NoticeExtraDto.TYPE_GENERAL,
                NoticeExtraDto.SCOPE_ALL, null, null, false));
    }
    @Test public void studentsCannotChangeOrUseManagementListing() {
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.saveNoticeExtra(student, req(NoticeExtraDto.SCOPE_ALL, null, null)); }
        });
        DormExtTestSupport.assertCode(ResultCodes.FORBIDDEN, new DormExtTestSupport.Action() {
            public void run() { service.notices(student, DormPageQuery.all()); }
        });
    }

    private NoticeExtraRequest req(String scope, Long building, Long room) {
        return new NoticeExtraRequest(1L, NoticeExtraDto.TYPE_GENERAL, scope, building, room, false);
    }
    private void expect(String code, NoticeExtraRequest request) {
        DormExtTestSupport.assertCode(code, new DormExtTestSupport.Action() {
            public void run() { service.saveNoticeExtra(manager, request); }
        });
    }
}
