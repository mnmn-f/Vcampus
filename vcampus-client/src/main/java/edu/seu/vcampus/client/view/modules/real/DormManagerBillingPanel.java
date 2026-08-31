package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.view.BasePage;
/** 宿管员水电台账查询；缴费仅对学生开放。 */
public final class DormManagerBillingPanel extends javax.swing.JPanel {
    private final DormUtilityBillsTable bills;

    public DormManagerBillingPanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        bills = new DormUtilityBillsTable(page, service, false); add(bills);
    }

    public void reload() { bills.reload(); }
}
