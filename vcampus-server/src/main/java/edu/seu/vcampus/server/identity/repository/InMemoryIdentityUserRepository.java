package edu.seu.vcampus.server.identity.repository;

import edu.seu.vcampus.common.dto.identity.ProfileDto;
import edu.seu.vcampus.common.dto.identity.ProfileUpdateRequest;
import edu.seu.vcampus.common.dto.identity.RegistrationRequest;
import edu.seu.vcampus.common.dto.identity.UserPage;
import edu.seu.vcampus.common.dto.identity.UserQuery;
import edu.seu.vcampus.common.security.Role;
import edu.seu.vcampus.server.repository.UserRecord;
import edu.seu.vcampus.server.repository.UserRepository;

import java.sql.Connection;
import org.threeten.bp.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/** 用户内存仓储；可同步现有 UserRepository 让 AuthService 立即看见变更。 */
final class InMemoryIdentityUserRepository implements IdentityUserRepository {
    private final InMemoryIdentityState state;
    private final UserRepository authUsers;

    InMemoryIdentityUserRepository(InMemoryIdentityState state, UserRepository authUsers) {
        this.state = state;
        this.authUsers = authUsers;
    }
    @Override public IdentityUserRecord findById(Connection c, long id, boolean lock) {
        synchronized (state) { return state.users.get(id); }
    }
    @Override public IdentityUserRecord findByAccount(Connection c, String account,
                                                                 boolean lock) {
        synchronized (state) {
            Long id = state.idsByAccount.get(normalize(account));
            return id == null ? null : state.users.get(id);
        }
    }
    @Override public UserPage search(Connection c, UserQuery query) {
        UserQuery q = query == null ? new UserQuery() : query;
        synchronized (state) {
            List<ProfileDto> found = new ArrayList<ProfileDto>();
            for (IdentityUserRecord user : state.users.values()) {
                if (matches(user, q)) found.add(user.toProfile());
            }
            Collections.sort(found, new Comparator<ProfileDto>() {
                @Override public int compare(ProfileDto a, ProfileDto b) {
                    return Long.compare(a.getUserId(), b.getUserId());
                }
            });
            int from = Math.min(q.getOffset(), found.size());
            int to = Math.min(from + q.getPageSize(), found.size());
            return new UserPage(new ArrayList<ProfileDto>(found.subList(from, to)), q.getPage(),
                    q.getPageSize(), found.size());
        }
    }

    @Override public long insertStudent(Connection c, RegistrationRequest request,
                                         String passwordHash) {
        synchronized (state) {
            String key = normalize(request.getAccount());
            if (state.idsByAccount.containsKey(key)) {
                throw new IdentityRepositoryException("账号已存在");
            }
            long id = state.userSequence++;
            LocalDateTime now = LocalDateTime.now();
            IdentityUserRecord user = new IdentityUserRecord(id, request.getAccount().trim(),
                    passwordHash, request.getDisplayName().trim(), request.getEmail(),
                    request.getPhone(), null, "ACTIVE", EnumSet.of(Role.STUDENT), now, now, null);
            state.users.put(id, user);
            state.idsByAccount.put(key, id);
            sync(user);
            return id;
        }
    }

    @Override public boolean updateProfile(Connection c, long id, ProfileUpdateRequest request) {
        synchronized (state) {
            IdentityUserRecord old = state.users.get(id);
            if (old == null) return false;
            IdentityUserRecord next = new IdentityUserRecord(id, old.getAccount(),
                    old.getPasswordHash(), request.getDisplayName().trim(), request.getEmail(),
                    request.getPhone(), request.getAvatarUrl(), old.getStatus(), old.getRoles(),
                    old.getCreatedAt(), LocalDateTime.now(), old.getLastLoginAt());
            state.users.put(id, next);
            sync(next);
            return true;
        }
    }

    @Override public boolean updatePassword(Connection c, long id, String expectedHash,
                                            String newHash) {
        synchronized (state) {
            IdentityUserRecord old = state.users.get(id);
            if (old == null || !old.getPasswordHash().equals(expectedHash)) return false;
            return replace(old, new IdentityUserRecord(id, old.getAccount(), newHash,
                    old.getDisplayName(), old.getEmail(), old.getPhone(), old.getAvatarUrl(),
                    old.getStatus(), old.getRoles(), old.getCreatedAt(), LocalDateTime.now(),
                    old.getLastLoginAt()));
        }
    }

    @Override public boolean resetPassword(Connection c, long id, String newHash) {
        synchronized (state) {
            IdentityUserRecord old = state.users.get(id);
            return old != null && replace(old, new IdentityUserRecord(id, old.getAccount(), newHash,
                    old.getDisplayName(), old.getEmail(), old.getPhone(), old.getAvatarUrl(),
                    old.getStatus(), old.getRoles(), old.getCreatedAt(), LocalDateTime.now(),
                    old.getLastLoginAt()));
        }
    }

    @Override public boolean updateStatus(Connection c, long id, String status) {
        synchronized (state) {
            IdentityUserRecord old = state.users.get(id);
            return old != null && replace(old, new IdentityUserRecord(id, old.getAccount(),
                    old.getPasswordHash(), old.getDisplayName(), old.getEmail(), old.getPhone(),
                    old.getAvatarUrl(), status, old.getRoles(), old.getCreatedAt(),
                    LocalDateTime.now(), old.getLastLoginAt()));
        }
    }

    @Override public boolean assignRole(Connection c, long id, Role role, long operatorId) {
        synchronized (state) {
            IdentityUserRecord old = state.users.get(id);
            if (old == null || old.getRoles().contains(role)) return false;
            EnumSet<Role> roles = EnumSet.copyOf(old.getRoles());
            roles.add(role);
            return replace(old, copyRoles(old, roles));
        }
    }

    @Override public boolean revokeRole(Connection c, long id, Role role) {
        synchronized (state) {
            IdentityUserRecord old = state.users.get(id);
            if (old == null || !old.getRoles().contains(role) || old.getRoles().size() <= 1) {
                return false;
            }
            EnumSet<Role> roles = EnumSet.copyOf(old.getRoles());
            roles.remove(role);
            return replace(old, copyRoles(old, roles));
        }
    }

    @Override public int countRoles(Connection c, long id) {
        synchronized (state) {
            IdentityUserRecord user = state.users.get(id);
            return user == null ? 0 : user.getRoles().size();
        }
    }

    UserRepository authRepository() { return authUsers; }

    void add(IdentityUserRecord user) {
        synchronized (state) {
            state.users.put(user.getUserId(), user);
            state.idsByAccount.put(normalize(user.getAccount()), user.getUserId());
            state.userSequence = Math.max(state.userSequence, user.getUserId() + 1L);
            sync(user);
        }
    }

    private boolean replace(IdentityUserRecord old, IdentityUserRecord next) {
        state.users.put(old.getUserId(), next);
        sync(next);
        return true;
    }

    private static IdentityUserRecord copyRoles(IdentityUserRecord old, EnumSet<Role> roles) {
        return new IdentityUserRecord(old.getUserId(), old.getAccount(), old.getPasswordHash(),
                old.getDisplayName(), old.getEmail(), old.getPhone(), old.getAvatarUrl(),
                old.getStatus(), roles, old.getCreatedAt(), LocalDateTime.now(),
                old.getLastLoginAt());
    }

    private void sync(IdentityUserRecord user) {
        authUsers.save(new UserRecord(user.getUserId(), user.getAccount(), user.getPasswordHash(),
                user.getDisplayName(), user.getRoles(), "ACTIVE".equals(user.getStatus())));
    }

    private static boolean matches(IdentityUserRecord user, UserQuery q) {
        if (q.getStatus() != null && !q.getStatus().equalsIgnoreCase(user.getStatus())) return false;
        if (q.getKeyword() == null) return true;
        String key = q.getKeyword().toLowerCase(Locale.ROOT);
        return contains(user.getAccount(), key) || contains(user.getDisplayName(), key);
    }

    private static boolean contains(String value, String key) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(key);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
