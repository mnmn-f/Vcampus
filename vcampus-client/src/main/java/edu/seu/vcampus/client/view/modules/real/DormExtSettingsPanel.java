package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.view.BasePage;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/** 宿舍扩展设置入口：状态、门禁、预警阈值和房间维护。 */
public final class DormExtSettingsPanel extends JPanel {
    private final DormExtSettingsStatusPanel status;
    private final DormExtSettingsAccessPanel access;
    private final DormExtSettingsWarningPanel warning;
    private final DormExtSettingsRoomPanel room;

    public DormExtSettingsPanel(BasePage page, DormExtClientService service) {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        status = new DormExtSettingsStatusPanel(page, service);
        access = new DormExtSettingsAccessPanel(page, service);
        warning = new DormExtSettingsWarningPanel(page, service);
        room = new DormExtSettingsRoomPanel(page, service);
        add(status); add(access); add(warning); add(room);
    }

    public void reload() {
        status.reload(); access.reload(); warning.reload();
    }
}
