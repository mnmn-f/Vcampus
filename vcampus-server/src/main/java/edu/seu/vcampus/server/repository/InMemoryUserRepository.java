package edu.seu.vcampus.server.repository;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试和本地演示用用户仓储。
 *
 * <p>生产环境由 {@link MySqlUserRepository} 提供实现；本实现不写入磁盘。</p>
 */
public final class InMemoryUserRepository implements UserRepository {
    private final Map<Long, UserRecord> usersById = new ConcurrentHashMap<Long, UserRecord>();
    private final Map<String, Long> idsByAccount = new ConcurrentHashMap<String, Long>();

    @Override
    public UserRecord findByAccount(String account) {
        String key = normalize(account);
        if (key == null) {
            return null;
        }
        Long userId = idsByAccount.get(key);
        return userId == null ? null : findById(userId);
    }

    @Override
    public UserRecord findById(long userId) {
        return usersById.get(userId);
    }

    @Override
    public synchronized void save(UserRecord user) {
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
        String accountKey = normalize(user.getAccount());
        UserRecord previous = usersById.put(user.getUserId(), user);
        if (previous != null) {
            removeAccountMapping(normalize(previous.getAccount()), user.getUserId());
        }
        Long previousId = idsByAccount.put(accountKey, user.getUserId());
        if (previousId != null && previousId.longValue() != user.getUserId()) {
            UserRecord replaced = usersById.remove(previousId);
            if (replaced != null) {
                removeAccountMapping(normalize(replaced.getAccount()), previousId);
            }
        }
    }

    public int size() {
        return usersById.size();
    }

    private void removeAccountMapping(String account, Long expectedUserId) {
        Long currentUserId = idsByAccount.get(account);
        if (expectedUserId.equals(currentUserId)) {
            idsByAccount.remove(account);
        }
    }

    private static String normalize(String account) {
        if (account == null || account.trim().isEmpty()) {
            return null;
        }
        return account.trim().toLowerCase(Locale.ROOT);
    }
}
