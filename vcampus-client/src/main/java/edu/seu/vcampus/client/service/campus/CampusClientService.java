package edu.seu.vcampus.client.service.campus;

import edu.seu.vcampus.client.network.NetworkClientException;
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

/** 教务扩展客户端网络边界；页面不接触命令信封和仓储。 */
public interface CampusClientService {
    CampusPage<CampusAnnouncementDto> announcements(CampusAnnouncementQuery query) throws NetworkClientException;
    CampusAnnouncementDto saveAnnouncement(CampusAnnouncementSaveRequest request) throws NetworkClientException;
    CampusAnnouncementDto revokeAnnouncement(long id) throws NetworkClientException;
    CampusPage<CompetitionDto> competitions(CampusCompetitionQuery query) throws NetworkClientException;
    CompetitionDto saveCompetition(CompetitionSaveRequest request) throws NetworkClientException;
    CompetitionRegistrationDto registerCompetition(long id) throws NetworkClientException;
    void cancelCompetition(long id) throws NetworkClientException;
    CampusPage<CompetitionRegistrationDto> competitionRoster(long id, CampusPageQuery query) throws NetworkClientException;
    CampusPage<CompetitionRegistrationDto> myCompetitionRegistrations(CampusPageQuery query) throws NetworkClientException;
    CampusPage<SrtpRecordDto> mySrtp(CampusPageQuery query) throws NetworkClientException;
    CampusPage<SrtpRecordDto> listSrtp(CampusPageQuery query) throws NetworkClientException;
    SrtpRecordDto saveSrtp(SrtpSaveRequest request) throws NetworkClientException;
    SrtpRecordDto reviewSrtp(SrtpStatusRequest request) throws NetworkClientException;
    CampusPage<CampusClassroomDto> classrooms(CampusPageQuery query) throws NetworkClientException;
    ClassroomReservationDto applyClassroom(ClassroomReservationRequest request) throws NetworkClientException;
    CampusPage<ClassroomReservationDto> myClassroomReservations(CampusPageQuery query) throws NetworkClientException;
    CampusPage<ClassroomReservationDto> classroomReservations(CampusPageQuery query) throws NetworkClientException;
    ClassroomReservationDto reviewClassroom(ClassroomReviewRequest request) throws NetworkClientException;
    ClassroomReservationDto cancelClassroom(long id) throws NetworkClientException;
}
