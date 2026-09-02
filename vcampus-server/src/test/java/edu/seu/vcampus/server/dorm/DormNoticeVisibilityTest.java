package edu.seu.vcampus.server.dorm;

import edu.seu.vcampus.common.dto.dorm.DormPage;
import edu.seu.vcampus.common.dto.dorm.DormPageQuery;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraDto;
import edu.seu.vcampus.common.dto.dorm.ext.NoticeExtraRequest;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.dorm.repository.InMemoryDormExtRepository;
import edu.seu.vcampus.server.dorm.service.DormExtService;
import edu.seu.vcampus.server.security.SessionContext;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 宿舍公告的可见范围、置顶排序与默认兼容值。 */
public final class DormNoticeVisibilityTest {
    private InMemoryDormExtRepository repository;
    private DormExtService service;
    private SessionContext manager;
    private SessionContext student;
    private SessionContext outsider;

    @Before public void setUp() {
        repository = new InMemoryDormExtRepository(); repository.addBuilding(1L, "D1"); repository.addBuilding(2L, "D2");
        repository.addRoom(10L, "D1", "101"); repository.addRoom(20L, "D2", "201");
        repository.setRoomBuilding(10L, 1L); repository.setRoomBuilding(20L, 2L);
        repository.addAccommodation(11L, 10L); repository.addAccommodation(12L, 20L);
        repository.addNotice(1L, "全体公告", "PUBLISHED"); repository.addNotice(2L, "D1 停水", "PUBLISHED");
        repository.addNotice(3L, "101 整改", "PUBLISHED"); repository.addNotice(4L, "草稿公告", "DRAFT");
        service = new DormExtService(repository); manager = DormExtTestSupport.session(90L, Role.DORM_MANAGER);
        student = DormExtTestSupport.session(11L, Role.STUDENT); outsider = DormExtTestSupport.session(12L, Role.STUDENT);
    }

    @Test public void announcementsWithoutExtrasReadAsDefaults() {
        NoticeExtraDto row = repository.findNotice(null, 1L);
        assertEquals(NoticeExtraDto.TYPE_GENERAL, row.getNoticeType()); assertEquals(NoticeExtraDto.SCOPE_ALL, row.getScopeType());
        assertFalse(row.isPinned());
    }
    @Test public void managerSeesDraftsAndStudentsDoNot() {
        assertEquals(4, service.notices(manager, DormPageQuery.all()).getItems().size());
        assertEquals(3, service.myNotices(student, DormPageQuery.all()).getItems().size());
    }
    @Test public void buildingAndRoomScopesReachOnlyMatchingResidents() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L, NoticeExtraDto.TYPE_MAINTENANCE,
                NoticeExtraDto.SCOPE_BUILDING, 1L, null, false));
        assertTrue(titles(service.myNotices(student, DormPageQuery.all())).contains("D1 停水"));
        assertFalse(titles(service.myNotices(outsider, DormPageQuery.all())).contains("D1 停水"));
        service.saveNoticeExtra(manager, new NoticeExtraRequest(3L, NoticeExtraDto.TYPE_HYGIENE,
                NoticeExtraDto.SCOPE_ROOM, null, 10L, false));
        assertTrue(titles(service.myNotices(student, DormPageQuery.all())).contains("101 整改"));
        assertFalse(titles(service.myNotices(outsider, DormPageQuery.all())).contains("101 整改"));
    }
    @Test public void residentWithoutAccommodationSeesOnlyGlobalNotices() {
        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L, NoticeExtraDto.TYPE_MAINTENANCE,
                NoticeExtraDto.SCOPE_BUILDING, 1L, null, false));
        List<String> visible = titles(service.myNotices(DormExtTestSupport.session(13L, Role.STUDENT), DormPageQuery.all()));
        assertTrue(visible.contains("全体公告")); assertFalse(visible.contains("D1 停水"));
    }
    @Test public void scopeChangeTakesEffectImmediately() {
        NoticeExtraRequest d1 = new NoticeExtraRequest(2L, NoticeExtraDto.TYPE_MAINTENANCE,
                NoticeExtraDto.SCOPE_BUILDING, 1L, null, false);
        service.saveNoticeExtra(manager, d1); assertFalse(titles(service.myNotices(outsider, DormPageQuery.all())).contains("D1 停水"));
        service.saveNoticeExtra(manager, new NoticeExtraRequest(2L, NoticeExtraDto.TYPE_MAINTENANCE,
                NoticeExtraDto.SCOPE_ALL, null, null, false));
        assertTrue(titles(service.myNotices(outsider, DormPageQuery.all())).contains("D1 停水"));
    }
    @Test public void pinnedNoticeSortsFirstAndUnpinClearsTime() {
        assertEquals("101 整改", titles(service.myNotices(student, DormPageQuery.all()).getItems()).get(0));
        service.saveNoticeExtra(manager, new NoticeExtraRequest(1L, NoticeExtraDto.TYPE_URGENT,
                NoticeExtraDto.SCOPE_ALL, null, null, true));
        assertEquals("全体公告", titles(service.myNotices(student, DormPageQuery.all()).getItems()).get(0));
        NoticeExtraDto after = service.saveNoticeExtra(manager, new NoticeExtraRequest(1L,
                NoticeExtraDto.TYPE_GENERAL, NoticeExtraDto.SCOPE_ALL, null, null, false));
        assertFalse(after.isPinned()); assertEquals(null, after.getPinnedAt());
    }
    @Test public void repeatedPinKeepsOriginalTime() {
        NoticeExtraDto first = service.saveNoticeExtra(manager, new NoticeExtraRequest(1L, NoticeExtraDto.TYPE_GENERAL,
                NoticeExtraDto.SCOPE_ALL, null, null, true));
        NoticeExtraDto second = service.saveNoticeExtra(manager, new NoticeExtraRequest(1L, NoticeExtraDto.TYPE_URGENT,
                NoticeExtraDto.SCOPE_ALL, null, null, true));
        assertEquals(first.getPinnedAt(), second.getPinnedAt()); assertEquals(NoticeExtraDto.TYPE_URGENT, second.getNoticeType());
    }

    private static List<String> titles(DormPage<NoticeExtraDto> page) { return titles(page.getItems()); }
    private static List<String> titles(List<NoticeExtraDto> rows) {
        List<String> values = new ArrayList<String>(); for (NoticeExtraDto item : rows) values.add(item.getTitle()); return values;
    }
}
