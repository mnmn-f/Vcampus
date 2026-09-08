package edu.seu.vcampus.common.protocol.command;

/** 图书馆业务命令的唯一登记处。 */
public final class LibraryCommands {
    public static final String BOOK_SEARCH = "library.book.search";
    public static final String BOOK_DETAIL = "library.book.detail";
    public static final String BOOK_BORROW = "library.book.borrow";
    public static final String BOOK_RETURN = "library.book.return";
    public static final String BORROW_MINE = "library.borrow.mine";
    public static final String BORROW_ADMIN_LIST = "library.borrow.admin-list";
    public static final String BOOK_SAVE = "library.book.save";

    public static final String STUDY_ROOM_SEARCH = "library.study-room.search";
    public static final String STUDY_ROOM_RESERVE = "library.study-room.reserve";
    public static final String STUDY_ROOM_CANCEL = "library.study-room.cancel";
    public static final String STUDY_ROOM_RESERVATIONS = "library.study-room.reservations";
    public static final String STUDY_ROOM_SAVE = "library.study-room.save";

    public static final String RESOURCE_SEARCH = "library.resource.search";
    public static final String RESOURCE_SAVE = "library.resource.save";
    public static final String RESOURCE_ACCESS = "library.resource.access";
    public static final String RESOURCE_ACCESS_LOGS = "library.resource.access-logs";

    public static final String BOOK_UNAVAILABLE = "LIBRARY.BOOK_UNAVAILABLE";
    public static final String ALREADY_BORROWED = "LIBRARY.ALREADY_BORROWED";
    public static final String BORROW_NOT_ACTIVE = "LIBRARY.BORROW_NOT_ACTIVE";
    public static final String RESERVATION_CONFLICT = "LIBRARY.RESERVATION_CONFLICT";
    public static final String RESERVATION_DUPLICATE = "LIBRARY.RESERVATION_DUPLICATE";
    public static final String ROOM_CLOSED = "LIBRARY.ROOM_CLOSED";
    public static final String INVALID_TIME = "LIBRARY.INVALID_TIME";
    public static final String INVALID_INVENTORY = "LIBRARY.INVALID_INVENTORY";

    public static final String BOOK_QUERY = BOOK_SEARCH;
    public static final String BOOK_GET = BOOK_DETAIL;
    public static final String BORROW_BOOK = BOOK_BORROW;
    public static final String RETURN_BOOK = BOOK_RETURN;
    public static final String STUDY_ROOM_QUERY = STUDY_ROOM_SEARCH;
    public static final String RESOURCE_QUERY = RESOURCE_SEARCH;

    private LibraryCommands() {
    }
}
