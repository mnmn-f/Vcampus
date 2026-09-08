package edu.seu.vcampus.server.security;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt 密码哈希适配器；服务端任何时候都不保存或比较明文密码。 */
public final class PasswordHasher {
    private static final int DEFAULT_LOG_ROUNDS = 12;
    private final int logRounds;

    public PasswordHasher() {
        this(DEFAULT_LOG_ROUNDS);
    }

    public PasswordHasher(int logRounds) {
        if (logRounds < 4 || logRounds > 31) {
            throw new IllegalArgumentException("BCrypt log rounds must be between 4 and 31");
        }
        this.logRounds = logRounds;
    }

    public String hash(String rawPassword) {
        requirePassword(rawPassword);
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(logRounds));
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isEmpty()
                || passwordHash == null || passwordHash.trim().isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** 便于业务层按自然语言调用。 */
    public boolean verify(String rawPassword, String passwordHash) {
        return matches(rawPassword, passwordHash);
    }

    private static void requirePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
    }
}
