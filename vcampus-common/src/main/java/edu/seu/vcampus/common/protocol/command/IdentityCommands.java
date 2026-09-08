package edu.seu.vcampus.common.protocol.command;

/** 身份资料与系统管理命令字唯一登记处。 */
public final class IdentityCommands {
    public static final String REGISTER = "identity.register";
    public static final String PROFILE_SELF = "identity.profile.self";
    public static final String PROFILE_UPDATE = "identity.profile.update";
    public static final String PASSWORD_CHANGE = "identity.password.change";
    public static final String USER_SEARCH = "identity.user.search";
    public static final String USER_STATUS_UPDATE = "identity.user.status-update";
    public static final String USER_PASSWORD_RESET = "identity.user.password-reset";
    public static final String ROLE_LIST = "identity.role.list";
    public static final String ROLE_ASSIGN = "identity.role.assign";
    public static final String ROLE_REVOKE = "identity.role.revoke";
    public static final String SESSION_LIST = "identity.session.list";
    public static final String SESSION_REVOKE = "identity.session.revoke";
    public static final String LOGIN_AUDIT_PAGE = "identity.audit.login";
    public static final String BUSINESS_AUDIT_PAGE = "identity.audit.business";
    public static final String SYSTEM_MONITOR = "identity.system.monitor";
    public static final String ACCOUNT_CANCELLATION_SUBMIT = "identity.account-cancellation.submit";
    public static final String ACCOUNT_CANCELLATION_SELF_LIST = "identity.account-cancellation.self-list";
    public static final String ACCOUNT_CANCELLATION_WITHDRAW = "identity.account-cancellation.withdraw";
    public static final String ACCOUNT_CANCELLATION_ADMIN_LIST = "identity.account-cancellation.admin-list";
    public static final String ACCOUNT_CANCELLATION_APPROVE = "identity.account-cancellation.approve";
    public static final String ACCOUNT_CANCELLATION_REJECT = "identity.account-cancellation.reject";

    public static final String ACCOUNT_CANCELLATION_LIST = ACCOUNT_CANCELLATION_SELF_LIST;
    public static final String ACCOUNT_CANCELLATION_REVIEW_LIST = ACCOUNT_CANCELLATION_ADMIN_LIST;

    public static final String PROFILE_GET = PROFILE_SELF;
    public static final String USER_PAGE = USER_SEARCH;
    public static final String RESET_PASSWORD = USER_PASSWORD_RESET;
    public static final String AUDIT_LOGIN = LOGIN_AUDIT_PAGE;
    public static final String AUDIT_BUSINESS = BUSINESS_AUDIT_PAGE;
    public static final String MONITOR = SYSTEM_MONITOR;

    private IdentityCommands() { }
}
