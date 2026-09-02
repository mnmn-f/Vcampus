package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.common.protocol.command.DormExtCommands;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormException;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** 宿舍公告的类型、可见范围与置顶：范围投放、置顶排序与越权拦截。 */
public final class DormNoticeExtraTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;
    private SessionContext outsider;

    @Before
    public void setUp() {
        repository = new InMemoryDormExtRepository();
        repository.addBuilding(1L, "D1");
        repository.addBuilding(2L, "D2");
        repository.addRoom(10L, "D1", "101");
        repository.addRoom(20L, "D2", "201");
        repository.setRoomBuilding(10L, 1L);
        repository.setRoomBuilding(20L, 2L);
        // 11 号住 D1-101，12 号住 D2-201，13 号没有住宿记录
        repository.addAccommodation(11L, 10L);
        repository.addAccommodation(12L, 20L);

        repository.addNotice(1L, "全体公告", "PUBLISHED");
        repository.addNotice(2L, "D1 停水", "PUBLISHED");
        repository.addNotice(3L, "101 整改", "PUBLISHED");
        repository.addNotice(4L, "草稿公告", "DRAFT");

        service = new DormExtService(repository);
        manager = session(90L, Role.DORM_MANAGER);
        student = session(11L, Role.STUDENT);
        outsider = session(12L, Role.STUDENT);
    }

    // ---------- 默认值与迁移友好 ----------

    @Test
    public void announcementsWithoutExtrasReadAsDefaults() {
        // main 已发布、从未设置过扩展属性的公告必须照常可见，不需要数据迁移。
        NoticeExtraDto row = repository.findNotice(null, 1L);
        assertEquals(NoticeExtraDto.TYPE_GENERAL, row.getNoticeType());
        assertEquals(NoticeExtraDto.SCOPE_ALL, row.getScopeType());
        assertFalse(row.isPinned());
    }

    @Test
    public void managerSeesDraftsAndStudentsDoNot() {
        assertEquals(4, service.notices(manager, DormPageQuery.all()).getItems().size());
        assertEquals(3, service.myNotices(student, DormPageQuery.all()).getItems().size());
    }

    // ---------- 可见范围 ----------

    @Test
    public void buildingScopedNoticeReachesOnlyThatBuilding() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L,
                NoticeExtraDto.TYPE_MAINTENANCE, NoticeExtraDto.SCOPE_BUILDING,
                Long.valueOf(1L), null, false));
        assertTrue(titles(service.myNotices(student, DormPageQuery.all())).contains("D1 停水"));
        assertFalse(titles(service.myNotices(outsider, DormPageQuery.all())).contains("D1 停水"));
    }

    @Test
    public void roomScopedNoticeReachesOnlyThatRoom() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(3L,
                NoticeExtraDto.TYPE_HYGIENE, NoticeExtraDto.SCOPE_ROOM,
                null, Long.valueOf(10L), false));
        assertTrue(titles(service.myNotices(student, DormPageQuery.all())).contains("101 整改"));
        assertFalse(titles(service.myNotices(outsider, DormPageQuery.all())).contains("101 整改"));
    }

    @Test
    public void studentWithoutAccommodationStillSeesGlobalNotices() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L,
                NoticeExtraDto.TYPE_MAINTENANCE, NoticeExtraDto.SCOPE_BUILDING,
                Long.valueOf(1L), null, false));
        SessionContext homeless = session(13L, Role.STUDENT);
        List<String> visible = titles(service.myNotices(homeless, DormPageQuery.all()));
        assertTrue("没有住宿记录也应看得到全体公告", visible.contains("全体公告"));
        assertFalse("但看不到定向投放的公告", visible.contains("D1 停水"));
    }

    @Test
    public void scopeChangeTakesEffectImmediately() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L,
                NoticeExtraDto.TYPE_MAINTENANCE, NoticeExtraDto.SCOPE_BUILDING,
                Long.valueOf(1L), null, false));
        assertFalse(titles(service.myNotices(outsider, DormPageQuery.all())).contains("D1 停水"));

        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L,
                NoticeExtraDto.TYPE_MAINTENANCE, NoticeExtraDto.SCOPE_ALL, null, null, false));
        assertTrue("改回全体后所有人都应看得到",
                titles(service.myNotices(outsider, DormPageQuery.all())).contains("D1 停水"));
    }

    // ---------- 置顶 ----------

    @Test
    public void pinnedNoticeSortsFirst() {
        assertEquals("默认按编号倒序，最新的在最前", "101 整改",
                titles(service.myNotices(student, DormPageQuery.all())).get(0));
        service.saveNoticeExtra(manager, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_URGENT, NoticeExtraDto.SCOPE_ALL, null, null, true));
        assertEquals("置顶后排到最前", "全体公告",
                titles(service.myNotices(student, DormPageQuery.all())).get(0));
    }

    @Test
    public void unpinningClearsPinnedAt() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL, null, null, true));
        assertNotNull(repository.findNotice(null, 1L).getPinnedAt());

        NoticeExtraDto after = service.saveNoticeExtra(manager, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL, null, null, false));
        assertFalse(after.isPinned());
        assertEquals(null, after.getPinnedAt());
    }

    @Test
    public void repeatedSaveKeepsTheOriginalPinTime() {
        NoticeExtraDto first = service.saveNoticeExtra(manager, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL, null, null, true));
        NoticeExtraDto second = service.saveNoticeExtra(manager, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_URGENT, NoticeExtraDto.SCOPE_ALL, null, null, true));
        assertEquals("重复保存不应把已置顶的公告顶到其它置顶公告前面",
                first.getPinnedAt(), second.getPinnedAt());
        assertEquals(NoticeExtraDto.TYPE_URGENT, second.getNoticeType());
    }

    // ---------- 输入校验 ----------

    @Test
    public void buildingScopeRequiresABuilding() {
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_BUILDING, null, null, false));
    }

    @Test
    public void roomScopeRequiresARoom() {
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ROOM, null, null, false));
    }

    @Test
    public void globalScopeRejectsATarget() {
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL,
                Long.valueOf(1L), null, false));
    }

    @Test
    public void buildingScopeRejectsARoomAtTheSameTime() {
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_BUILDING,
                Long.valueOf(1L), Long.valueOf(10L), false));
    }

    @Test
    public void unknownScopeIsRejected() {
        expect(DormExtCommands.NOTICE_SCOPE_INVALID, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, "CAMPUS", null, null, false));
    }

    @Test
    public void unknownTypeIsRejected() {
        expect(DormExtCommands.INVALID_INPUT, new NoticeExtraRequest(1L,
                "GOSSIP", NoticeExtraDto.SCOPE_ALL, null, null, false));
    }

    @Test
    public void missingAnnouncementIsRejected() {
        expect(DormExtCommands.NOTICE_NOT_FOUND, new NoticeExtraRequest(999L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL, null, null, false));
    }

    // ---------- 鉴权 ----------

    @Test
    public void studentsCannotChangeNoticeSettings() {
        try {
            service.saveNoticeExtra(student, new NoticeExtraRequest(1L,
                    NoticeExtraDto.TYPE_URGENT, NoticeExtraDto.SCOPE_ALL, null, null, true));
            fail("学生不应能设置公告");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    @Test
    public void studentsCannotUseTheManagementListing() {
        try {
            service.notices(student, DormPageQuery.all());
            fail("学生不应看得到含草稿的管理视图");
        } catch (DormException ex) {
            assertEquals(ResultCodes.FORBIDDEN, ex.getResultCode());
        }
    }

    private void expect(String code, NoticeExtraRequest request) {
        try {
            service.saveNoticeExtra(manager, request);
            fail("应当拒绝：" + code);
        } catch (DormException ex) {
            assertEquals(code, ex.getResultCode());
        }
    }

    private static List<String> titles(edu.seu.vcampus.common.dto.dorm.DormPage<NoticeExtraDto> page) {
        java.util.List<String> values = new java.util.ArrayList<String>();
        for (NoticeExtraDto item : page.getItems()) values.add(item.getTitle());
        return values;
    }

    private static SessionContext session(long userId, Role role) {
        return new SessionContext("token-" + userId, userId, "u" + userId,
                "用户" + userId, Collections.singleton(role), role);
    }
}
