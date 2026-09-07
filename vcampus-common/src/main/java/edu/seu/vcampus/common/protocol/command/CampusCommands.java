package edu.seu.vcampus.common.protocol.command;

/** 教务扩展与通用公告命令、稳定结果码的唯一登记处。 */
public final class CampusCommands {
    public static final String ANNOUNCEMENT_LIST = "campus.announcement.list";
    public static final String ANNOUNCEMENT_SAVE = "campus.announcement.save";
    public static final String ANNOUNCEMENT_REVOKE = "campus.announcement.revoke";
    public static final String COMPETITION_LIST = "campus.competition.list";
    public static final String COMPETITION_SAVE = "campus.competition.save";
    public static final String COMPETITION_REGISTER = "campus.competition.register";
    public static final String COMPETITION_CANCEL = "campus.competition.cancel";
    public static final String COMPETITION_ROSTER = "campus.competition.roster";
    public static final String COMPETITION_MINE = "campus.competition.mine";
    public static final String SRTP_MINE = "campus.srtp.mine";
    public static final String SRTP_LIST = "campus.srtp.list";
    public static final String SRTP_SAVE = "campus.srtp.save";
    public static final String SRTP_REVIEW = "campus.srtp.review";
    public static final String CLASSROOM_LIST = "campus.classroom.list";
    public static final String CLASSROOM_APPLY = "campus.classroom.apply";
    public static final String CLASSROOM_MINE = "campus.classroom.mine";
    public static final String CLASSROOM_REVIEW = "campus.classroom.review";
    public static final String CLASSROOM_CANCEL = "campus.classroom.cancel";
    public static final String CLASSROOM_REQUEST_LIST = "campus.classroom.request.list";

    public static final String INVALID_INPUT = "CAMPUS.INVALID_INPUT";
    public static final String NOT_FOUND = "CAMPUS.NOT_FOUND";
    public static final String FORBIDDEN = "CAMPUS.FORBIDDEN";
    public static final String INTERNAL_ERROR = "CAMPUS.INTERNAL_ERROR";
    public static final String ANNOUNCEMENT_NOT_FOUND = "CAMPUS.ANNOUNCEMENT_NOT_FOUND";
    public static final String ANNOUNCEMENT_INVALID_STATE = "CAMPUS.ANNOUNCEMENT_INVALID_STATE";
    public static final String ANNOUNCEMENT_VISIBILITY = "CAMPUS.ANNOUNCEMENT_VISIBILITY";
    public static final String COMPETITION_NOT_FOUND = "CAMPUS.COMPETITION_NOT_FOUND";
    public static final String COMPETITION_INVALID_STATE = "CAMPUS.COMPETITION_INVALID_STATE";
    public static final String COMPETITION_DUPLICATE = "CAMPUS.COMPETITION_DUPLICATE";
    public static final String COMPETITION_FULL = "CAMPUS.COMPETITION_FULL";
    public static final String COMPETITION_WINDOW_CLOSED = "CAMPUS.COMPETITION_WINDOW_CLOSED";
    public static final String SRTP_NOT_FOUND = "CAMPUS.SRTP_NOT_FOUND";
    public static final String SRTP_INVALID_STATE = "CAMPUS.SRTP_INVALID_STATE";
    public static final String SRTP_FORBIDDEN = "CAMPUS.SRTP_FORBIDDEN";
    public static final String CLASSROOM_NOT_FOUND = "CAMPUS.CLASSROOM_NOT_FOUND";
    public static final String CLASSROOM_CONFLICT = "CAMPUS.CLASSROOM_CONFLICT";
    public static final String CLASSROOM_INVALID_STATE = "CAMPUS.CLASSROOM_INVALID_STATE";
    public static final String CLASSROOM_FORBIDDEN = "CAMPUS.CLASSROOM_FORBIDDEN";

    /* Compatibility aliases make command names easy to discover by callers. */
    public static final String ANNOUNCEMENT_QUERY = ANNOUNCEMENT_LIST;
    public static final String COMPETITION_QUERY = COMPETITION_LIST;
    public static final String COMPETITION_ENROLL = COMPETITION_REGISTER;
    public static final String SRTP_QUERY_MINE = SRTP_MINE;
    public static final String CLASSROOM_RESERVATION_APPLY = CLASSROOM_APPLY;
    public static final String DUPLICATE_REGISTRATION = COMPETITION_DUPLICATE;
    public static final String REGISTRATION_FULL = COMPETITION_FULL;
    public static final String REGISTRATION_CLOSED = COMPETITION_WINDOW_CLOSED;
    public static final String RESERVATION_NOT_FOUND = CLASSROOM_NOT_FOUND;
    public static final String RESERVATION_CONFLICT = CLASSROOM_CONFLICT;
    public static final String SRTP_RECORD_NOT_FOUND = SRTP_NOT_FOUND;

    private CampusCommands() { }
}
