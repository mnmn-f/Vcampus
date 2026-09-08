package edu.seu.vcampus.server.campus.repository.mysql;

import edu.seu.vcampus.server.campus.repository.CampusRepositoryComposite;
import edu.seu.vcampus.server.db.JdbcConnectionFactory;

/** 生产组合仓储；每个业务表由一个专责 JDBC 仓储负责。 */
public final class MySqlCampusRepository extends CampusRepositoryComposite {
    public MySqlCampusRepository() { this(new JdbcConnectionFactory()); }

    public MySqlCampusRepository(JdbcConnectionFactory connections) {
        super(new MySqlCampusAnnouncementRepository(), new MySqlCampusCompetitionRepository(),
                new MySqlCampusSrtpRepository(), new MySqlCampusClassroomRepository());
        if (connections == null) throw new IllegalArgumentException("connections is required");
    }
}
