package edu.seu.vcampus.server.ai.tool;

import edu.seu.vcampus.common.dto.campus.CampusIdRequest;
import edu.seu.vcampus.common.dto.campus.CampusPageQuery;
import edu.seu.vcampus.common.dto.library.ReturnBorrowRequest;
import edu.seu.vcampus.common.dto.store.CartItemRequest;
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
        assertTrue(tool(CampusCommandTool.Kind.CAMPUS_PAGE).payload("{}") instanceof CampusPageQuery);
    }

    private CampusCommandTool tool(CampusCommandTool.Kind kind) {
        return new CampusCommandTool("test", "test", "test", false, kind);
    }
}
