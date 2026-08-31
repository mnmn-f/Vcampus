package edu.seu.vcampus.server.student.repository.mysql;

import edu.seu.vcampus.server.db.JdbcConnectionFactory;
import edu.seu.vcampus.server.student.repository.DelegatingStudentRecordRepository;

/** 学籍模块 MySQL DAO 总入口；按档案和成绩职责委托，避免超大类。 */
public final class MySqlStudentRecordRepository
        extends DelegatingStudentRecordRepository {
    public MySqlStudentRecordRepository() {
        this(new MySqlStudentProfileRepository(), new MySqlStudentGradeRepository());
    }

    /** 兼容按连接工厂构造仓储的模块接线；事务连接仍由 TransactionManager 提供。 */
    public MySqlStudentRecordRepository(JdbcConnectionFactory ignored) {
        this();
    }

    private MySqlStudentRecordRepository(MySqlStudentProfileRepository profiles,
                                         MySqlStudentGradeRepository grades) {
        super(profiles, grades);
    }
}
