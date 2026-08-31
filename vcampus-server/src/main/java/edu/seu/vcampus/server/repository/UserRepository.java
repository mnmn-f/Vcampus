package edu.seu.vcampus.server.repository;


/** 用户认证记录的持久化边界。 */
public interface UserRepository {
    /** 通过校园账号、学号或工号查找唯一登录用户；标识歧义时返回空结果。 */
    UserRecord findByAccount(String account);

    UserRecord findById(long userId);

    void save(UserRecord user);
}
