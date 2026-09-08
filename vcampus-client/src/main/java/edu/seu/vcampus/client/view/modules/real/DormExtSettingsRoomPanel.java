package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.RoomDeleteRequest;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

/** 删除空置且无历史引用的房间。 */
final class DormExtSettingsRoomPanel extends JPanel {
    private final BasePage page;
    private final DormExtClientService service;
    private final JTextField roomId = UiFactory.textField(8);

    DormExtSettingsRoomPanel(BasePage page, DormExtClientService service) {
        this.page = page; this.service = service; setOpaque(false);
        SectionCard card = new SectionCard("删除房间", "仅可删除无住宿和历史记录的房间，操作不可撤销。");
        JPanel line = UiFactory.horizontal(8);
        line.add(UiFactory.body("房间编号")); line.add(roomId);
        line.add(DormExtSettingsSupport.button("删除房间", false, this::delete));
        card.setContent(line); setLayout(new BorderLayout()); add(card, BorderLayout.CENTER);
    }

    private void delete() {
        Long id = RealUi.number(roomId.getText());
        if (id == null || id.longValue() <= 0L) { page.showWarning("房间编号必须是正整数。"); return; }
        if (!RealUi.confirm(this, "删除房间 " + id + " 后不可恢复，确定继续吗？")) return;
        AsyncTask.run(() -> service.deleteRoom(new RoomDeleteRequest(id.longValue())), new AsyncTask.Callback<Long>() {
            @Override public void onSuccess(Long value) { page.showSuccess("房间 " + value + " 已删除。"); roomId.setText(""); }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
}
