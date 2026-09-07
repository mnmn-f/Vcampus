package edu.seu.vcampus.server.ai.repository;

import edu.seu.vcampus.common.ai.AiCompetitionRegistrationView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** AI 专用只读联合查询；不写入或复制业务数据。 */
public final class AiLiveDataRepository {
    public List<AiCompetitionRegistrationView> myCompetitionRegistrations(
            Connection connection, long userId) throws Exception {
        PreparedStatement statement = connection.prepareStatement(
                "SELECT c.id,c.title,cr.status,cr.registered_at,c.start_at "
                        + "FROM competition_registrations cr JOIN competitions c "
                        + "ON c.id=cr.competition_id WHERE cr.student_user_id=? "
                        + "AND cr.status='REGISTERED' ORDER BY c.start_at, c.id LIMIT 100");
        try {
            statement.setLong(1, userId); ResultSet rows = statement.executeQuery();
            try {
                List<AiCompetitionRegistrationView> result =
                        new ArrayList<AiCompetitionRegistrationView>();
                while (rows.next()) result.add(new AiCompetitionRegistrationView(
                        rows.getLong(1), rows.getString(2), rows.getString(3),
                        text(rows.getTimestamp(4)), text(rows.getTimestamp(5))));
                return result;
            } finally { rows.close(); }
        } finally { statement.close(); }
    }

    private String text(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime().toString().replace('T', ' ');
    }
}
