package edu.seu.vcampus.common.protocol;

/** 跨模块命令字的唯一登记处。 */
public final class Commands {
    public static final String AUTH_LOGIN = "auth.login";
    public static final String AUTH_LOGOUT = "auth.logout";
    public static final String AUTH_SWITCH_ROLE = "auth.switch-role";
    public static final String PROFILE_GET = "profile.get";
    public static final String PROFILE_UPDATE = "profile.update";
    public static final String AI_QUERY = "ai.query";
    public static final String AI_CONFIRM = "ai.confirm";

    private Commands() {
    }
}
