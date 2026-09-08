package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.dto.campus.CampusIdRequest;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.academic.CourseQuery;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
import edu.seu.vcampus.common.dto.dorm.LeaveSubmitRequest;
import edu.seu.vcampus.common.dto.library.StudyRoomReservationRequest;
import edu.seu.vcampus.common.dto.store.PaymentRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** AI 参数只转换为 common 中既有的业务 DTO。 */
public final class CampusCommandToolTest {
    @Test public void adaptsNewWritePayloads() {
        CartItemRequest cart = (CartItemRequest) tool(CampusCommandTool.Kind.CART_ITEM)
                .payload("{\"id\":31,\"quantity\":2}");
        assertEquals(31L, cart.getProductId());
        assertEquals(2, cart.getQuantity());
        ReturnBorrowRequest returned = (ReturnBorrowRequest) tool(
                CampusCommandTool.Kind.ID_RETURN_BORROW).payload("{\"id\":18}");
        assertEquals(18L, returned.getBorrowRecordId());
        CampusIdRequest id = (CampusIdRequest) tool(CampusCommandTool.Kind.CAMPUS_ID)
                .payload("{\"id\":9}");
        assertEquals(9L, id.getId());
    }

    @Test public void createsPagePayloadForCampusLists() {
        CampusPageQuery campus = (CampusPageQuery) tool(CampusCommandTool.Kind.CAMPUS_PAGE)
                .payload("{\"keyword\":\"建模\"}");
        assertEquals("建模", campus.getKeyword());
        CourseQuery course = (CourseQuery) tool(CampusCommandTool.Kind.COURSE_SEARCH)
                .payload("{\"keyword\":\"高数\"}");
        assertEquals("高数", course.getKeyword());
        assertTrue(tool(CampusCommandTool.Kind.CAMPUS_PAGE).payload("{}") instanceof CampusPageQuery);
    }

    @Test public void adaptsStructuredWritesAndClarifiesMissingFields() {
        StudyRoomReservationRequest room = (StudyRoomReservationRequest) tool(
                CampusCommandTool.Kind.STUDY_ROOM_RESERVE).payload(
                "{\"roomId\":7,\"startAt\":\"2026-09-06T14:00\",\"endAt\":\"2026-09-06T16:00\"}");
        assertEquals(7L, room.getRoomId());
        LeaveSubmitRequest leave = (LeaveSubmitRequest) tool(CampusCommandTool.Kind.LEAVE_SUBMIT)
                .payload("{\"leaveType\":\"PERSONAL\",\"startAt\":\"2026-09-06T08:00\","
                        + "\"endAt\":\"2026-09-07T20:00\",\"reason\":\"家庭事务\"}");
        assertEquals("PERSONAL", leave.getLeaveType());
        PaymentRequest payment = (PaymentRequest) tool(CampusCommandTool.Kind.ORDER_PAY)
                .payload("{\"orderId\":99}");
        assertEquals(99L, payment.getOrderId());
        assertTrue(payment.getIdempotencyKey().startsWith("ai-"));
        assertTrue(tool(CampusCommandTool.Kind.REPAIR_CREATE).clarificationFor("{}")
                .contains("房间编号"));
        String partial = tool(CampusCommandTool.Kind.STUDY_ROOM_RESERVE).clarificationFor(
                "{\"roomId\":7}");
        assertTrue(partial.contains("startAt,endAt"));
    }

    private CampusCommandTool tool(CampusCommandTool.Kind kind) {
        return new CampusCommandTool("test", "test", "test", false, kind);
    }
}
