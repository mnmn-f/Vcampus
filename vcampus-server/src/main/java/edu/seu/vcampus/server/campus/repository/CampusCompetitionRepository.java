package edu.seu.vcampus.server.campus.repository;

import edu.seu.vcampus.common.dto.campus.CampusPage;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.campus.CompetitionDto;
import edu.seu.vcampus.common.dto.campus.CompetitionRegistrationDto;
import edu.seu.vcampus.common.dto.campus.CompetitionSaveRequest;

import java.sql.Connection;
import java.sql.SQLException;

/** 比赛与报名持久化边界。 */
public interface CampusCompetitionRepository {
    CampusPage<CompetitionDto> list(Connection connection, CampusPageQuery query,
                                     boolean includeDrafts) throws SQLException;
    CompetitionDto findCompetition(Connection connection, long id) throws SQLException;
    CompetitionDto lockCompetition(Connection connection, long id) throws SQLException;
    CompetitionDto save(Connection connection, CompetitionSaveRequest request,
                        long organizerId) throws SQLException;
    CompetitionRegistrationDto findRegistration(Connection connection, long competitionId,
                                                long studentId, boolean forUpdate)
            throws SQLException;
    long countRegistered(Connection connection, long competitionId) throws SQLException;
    CompetitionRegistrationDto register(Connection connection, long competitionId,
                                         long studentId) throws SQLException;
    void cancelRegistration(Connection connection, long competitionId, long studentId) throws SQLException;
    CampusPage<CompetitionRegistrationDto> roster(Connection connection, long competitionId,
                                                  CampusPageQuery query) throws SQLException;
    CampusPage<CompetitionRegistrationDto> registrationsForStudent(Connection connection,
            long studentId, CampusPageQuery query) throws SQLException;
}
