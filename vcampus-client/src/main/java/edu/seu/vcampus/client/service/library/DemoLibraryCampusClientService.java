package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientException;
import edu.seu.vcampus.client.service.campus.CampusClientService;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementQuery;
import edu.seu.vcampus.common.dto.campus.CampusAnnouncementSaveRequest;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.CampusCompetitionQuery;
import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationRequest;
import edu.seu.vcampus.common.dto.campus.ClassroomReviewRequest;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;
import edu.seu.vcampus.common.dto.campus.SrtpSaveRequest;
import edu.seu.vcampus.common.dto.campus.SrtpStatusRequest;
import edu.seu.vcampus.common.security.Role;
import org.threeten.bp.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 仅为 Demo 图书馆提供可操作公告，其他校园业务仍保持未连接状态。 */
public final class DemoLibraryCampusClientService implements CampusClientService {
    private static final List<CampusAnnouncementDto> ANNOUNCEMENTS = seed();
    private static long nextId = 3L;
    private final boolean librarian;

    public DemoLibraryCampusClientService(Role role) {
        this.librarian = role == Role.LIBRARIAN;
    }

    @Override
    public CampusPage<CampusAnnouncementDto> announcements(CampusAnnouncementQuery request) {
        CampusAnnouncementQuery query = request == null
                ? new CampusAnnouncementQuery() : request;
        CampusPageQuery page = query.getPage();
        List<CampusAnnouncementDto> found = new ArrayList<CampusAnnouncementDto>();
        synchronized (ANNOUNCEMENTS) {
            for (CampusAnnouncementDto value : ANNOUNCEMENTS) {
                if (query.getModuleCode() != null
                        && !query.getModuleCode().equalsIgnoreCase(value.getModuleCode())) continue;
                if (!librarian && !visibleToStudent(value)) continue;
                if (page.getStatus() != null
                        && !page.getStatus().equalsIgnoreCase(value.getStatus())) continue;
                if (!matches(value.getTitle() + " " + value.getContent(),
                        page.getKeyword())) continue;
                found.add(value);
            }
        }
        int from = Math.min((page.getPage() - 1) * page.getPageSize(), found.size());
        int to = Math.min(from + page.getPageSize(), found.size());
        return new CampusPage<CampusAnnouncementDto>(page.getPage(), page.getPageSize(),
                found.size(), new ArrayList<CampusAnnouncementDto>(found.subList(from, to)));
    }

    @Override
    public CampusAnnouncementDto saveAnnouncement(CampusAnnouncementSaveRequest request)
            throws NetworkClientException {
        if (!librarian) throw error("只有图书管理员可以维护公告");
        if (request == null || blank(request.getTitle()) || blank(request.getContent())) {
            throw error("公告标题和内容不能为空");
        }
        synchronized (ANNOUNCEMENTS) {
            long id = request.isUpdate() ? request.getId().longValue() : nextId++;
            CampusAnnouncementDto old = find(id);
            LocalDateTime publishAt = request.getPublishAt();
            String status = request.getStatus() == null ? "DRAFT" : request.getStatus();
            if ("PUBLISHED".equals(status) && publishAt == null) publishAt = LocalDateTime.now();
            CampusAnnouncementDto value = new CampusAnnouncementDto(id, "LIBRARY",
                    request.getTitle().trim(), request.getContent().trim(),
                    request.getVisibleScope() == null ? "ALL" : request.getVisibleScope(),
                    request.getTargetRoleId(), request.getTargetRoleCode(), status, publishAt,
                    request.getExpireAt(), old == null ? 5L : old.getPublisherId());
            replace(value);
            return value;
        }
    }

    @Override
    public CampusAnnouncementDto revokeAnnouncement(long id) throws NetworkClientException {
        if (!librarian) throw error("只有图书管理员可以撤回公告");
        synchronized (ANNOUNCEMENTS) {
            CampusAnnouncementDto old = find(id);
            if (old == null) throw error("未找到公告");
            CampusAnnouncementDto value = new CampusAnnouncementDto(old.getId(),
                    old.getModuleCode(), old.getTitle(), old.getContent(),
                    old.getVisibleScope(), old.getTargetRoleId(), old.getTargetRoleCode(),
                    "REVOKED", old.getPublishAt(), old.getExpireAt(), old.getPublisherId());
            replace(value);
            return value;
        }
    }

    @Override public CampusPage<CompetitionDto> competitions(CampusCompetitionQuery q)
            throws NetworkClientException { throw unsupported(); }
    @Override public CompetitionDto saveCompetition(CompetitionSaveRequest r)
            throws NetworkClientException { throw unsupported(); }
    @Override public CompetitionRegistrationDto registerCompetition(long id)
            throws NetworkClientException { throw unsupported(); }
    @Override public void cancelCompetition(long id) throws NetworkClientException {
        throw unsupported();
    }
    @Override public CampusPage<CompetitionRegistrationDto> competitionRoster(
            long id, CampusPageQuery q) throws NetworkClientException { throw unsupported(); }
    @Override public CampusPage<CompetitionRegistrationDto> myCompetitionRegistrations(
            CampusPageQuery q) throws NetworkClientException { throw unsupported(); }
    @Override public CampusPage<SrtpRecordDto> mySrtp(CampusPageQuery q)
            throws NetworkClientException { throw unsupported(); }
    @Override public CampusPage<SrtpRecordDto> listSrtp(CampusPageQuery q)
            throws NetworkClientException { throw unsupported(); }
    @Override public SrtpRecordDto saveSrtp(SrtpSaveRequest r)
            throws NetworkClientException { throw unsupported(); }
    @Override public SrtpRecordDto reviewSrtp(SrtpStatusRequest r)
            throws NetworkClientException { throw unsupported(); }
    @Override public CampusPage<CampusClassroomDto> classrooms(CampusPageQuery q)
            throws NetworkClientException { throw unsupported(); }
    @Override public ClassroomReservationDto applyClassroom(ClassroomReservationRequest r)
            throws NetworkClientException { throw unsupported(); }
    @Override public CampusPage<ClassroomReservationDto> myClassroomReservations(CampusPageQuery q)
            throws NetworkClientException { throw unsupported(); }
    @Override public CampusPage<ClassroomReservationDto> classroomReservations(CampusPageQuery q)
            throws NetworkClientException { throw unsupported(); }
    @Override public ClassroomReservationDto reviewClassroom(ClassroomReviewRequest r)
            throws NetworkClientException { throw unsupported(); }
    @Override public ClassroomReservationDto cancelClassroom(long id)
            throws NetworkClientException { throw unsupported(); }

    private static CampusAnnouncementDto find(long id) {
        for (CampusAnnouncementDto value : ANNOUNCEMENTS) if (value.getId() == id) return value;
        return null;
    }

    private static void replace(CampusAnnouncementDto value) {
        for (int i = 0; i < ANNOUNCEMENTS.size(); i++) {
            if (ANNOUNCEMENTS.get(i).getId() == value.getId()) {
                ANNOUNCEMENTS.set(i, value);
                return;
            }
        }
        ANNOUNCEMENTS.add(0, value);
    }

    private static boolean visibleToStudent(CampusAnnouncementDto value) {
        if (!"PUBLISHED".equals(value.getStatus())) return false;
        LocalDateTime now = LocalDateTime.now();
        return (value.getPublishAt() == null || !value.getPublishAt().isAfter(now))
                && (value.getExpireAt() == null || value.getExpireAt().isAfter(now));
    }

    private static boolean matches(String source, String keyword) {
        return blank(keyword) || source.toLowerCase(Locale.ROOT).contains(
                keyword.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static NetworkClientException unsupported() {
        return error("Demo 模式仅接入图书馆公告");
    }

    private static NetworkClientException error(String message) {
        return new NetworkClientException("DEMO.LIBRARY", message);
    }

    private static List<CampusAnnouncementDto> seed() {
        LocalDateTime now = LocalDateTime.now();
        List<CampusAnnouncementDto> values = new ArrayList<CampusAnnouncementDto>();
        values.add(new CampusAnnouncementDto(1L, "LIBRARY", "图书馆开放时间通知",
                "李文正图书馆开放时间为每日 08:00—22:00。", "ALL", null,
                "PUBLISHED", now.minusDays(5), null, 5L));
        values.add(new CampusAnnouncementDto(2L, "LIBRARY", "电子资源使用说明",
                "校内用户可从线上资源页面访问已启用的学术数据库。", "ALL", null,
                "PUBLISHED", now.minusDays(2), null, 5L));
        return values;
    }
}
