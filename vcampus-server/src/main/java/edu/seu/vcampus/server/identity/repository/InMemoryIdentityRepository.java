package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.server.repository.UserRepository;
import edu.seu.vcampus.server.security.SessionManager;

/** 兼容简短命名的身份内存仓储。 */
public final class InMemoryIdentityRepository extends InMemoryIdentityRecordRepository {
    public InMemoryIdentityRepository() { super(); }

    public InMemoryIdentityRepository(UserRepository users, SessionManager sessions) {
        super(users, sessions);
    }
}
