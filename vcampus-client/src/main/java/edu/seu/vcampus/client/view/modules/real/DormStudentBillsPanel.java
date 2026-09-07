package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.DormClientService;
import edu.seu.vcampus.client.view.BasePage;

import javax.swing.JPanel;

/** 学生本人水电分摊账单查询和幂等缴费。 */
public final class DormStudentBillsPanel extends JPanel {
    private final DormUtilityBillsTable bills;

    public DormStudentBillsPanel(BasePage page, DormClientService service) {
        super(); setOpaque(false); setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
        bills = new DormUtilityBillsTable(page, service, true); add(bills);
    }

    public void reload() { bills.reload(); }
}
