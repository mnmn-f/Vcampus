package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusAnnouncementDto;
import edu.seu.vcampus.common.dto.campus.CampusClassroomDto;
import edu.seu.vcampus.common.dto.campus.ClassroomReservationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.SrtpRecordDto;

import java.util.LinkedHashMap;
import java.util.Map;

/** 内存仓储共享状态；仅用于测试和离线演示。 */
final class InMemoryCampusState {
    final Map<Long, CampusAnnouncementDto> announcements =
            new LinkedHashMap<Long, CampusAnnouncementDto>();
    final Map<Long, CompetitionDto> competitions = new LinkedHashMap<Long, CompetitionDto>();
    final Map<String, CompetitionRegistrationDto> registrations =
            new LinkedHashMap<String, CompetitionRegistrationDto>();
    final Map<Long, SrtpRecordDto> srtp = new LinkedHashMap<Long, SrtpRecordDto>();
    final Map<Long, CampusClassroomDto> classrooms =
            new LinkedHashMap<Long, CampusClassroomDto>();
    final Map<Long, ClassroomReservationDto> reservations =
            new LinkedHashMap<Long, ClassroomReservationDto>();
    long nextAnnouncement = 1L;
    long nextCompetition = 1L;
    long nextSrtp = 1L;
    long nextReservation = 1L;
}
