package edu.seu.vcampus.server.dorm.repository.mysql;

import edu.seu.vcampus.server.dorm.repository.DormPaymentPort;
import edu.seu.vcampus.server.dorm.repository.DormRepositoryComposite;

/** MySQL 宿舍仓储组合；每个子仓储按业务职责独立实现。 */
public final class MySqlDormRepository extends DormRepositoryComposite {
    public MySqlDormRepository() { this(new MySqlDormFacilityRepository(), new MySqlDormPaymentAdapter()); }

    public MySqlDormRepository(DormPaymentPort payments) { this(new MySqlDormFacilityRepository(), payments); }

    private MySqlDormRepository(MySqlDormFacilityRepository facilities, DormPaymentPort payments) {
        super(facilities, new MySqlDormSpaceRepository(), new MySqlDormLeaveRepository(),
                new MySqlDormAccommodationRepository(facilities), new MySqlDormGovernanceRepository(),
                new MySqlDormBillingRepository(payments), new MySqlDormAnnouncementRepository());
    }

}
