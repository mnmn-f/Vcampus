package edu.seu.vcampus.server.db;

/** 只接收仓储中固定 SQL 表达式；人员显示不依赖客户端填写数据库主键。 */
public final class PersonDisplaySql {
    private PersonDisplaySql() { }
    public static String label(String userIdExpression) {
        return "(SELECT CONCAT(COALESCE(NULLIF(u.display_name,''),u.username),'（',COALESCE(sp.student_no,u.username),'）')"
                + " FROM users u LEFT JOIN student_profiles sp ON sp.user_id=u.id WHERE u.id=" + userIdExpression + ")";
    }
}
