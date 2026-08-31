package edu.seu.vcampus.server.campus.repository;

/** 由公告、比赛、SRTP、教室专责仓储组成的统一注入边界。 */
public interface CampusRepository extends CampusAnnouncementRepository,
        CampusCompetitionRepository, CampusSrtpRepository, CampusClassroomRepository {
}
