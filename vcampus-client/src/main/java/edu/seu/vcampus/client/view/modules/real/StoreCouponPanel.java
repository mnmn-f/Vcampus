package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.client.ui.UiFactory;
import edu.seu.vcampus.client.ui.components.PrimaryButton;
import edu.seu.vcampus.client.ui.components.SectionCard;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** 学生领取和查看可用优惠券。 */
public final class StoreCouponPanel extends SectionCard {
    private final BasePage page; private final StoreClientService service;
    private final JTextField code=UiFactory.textField(12); private final JLabel state=UiFactory.muted(" ");
    private final JButton claim=new PrimaryButton("领取选中优惠券");
    private final AsyncPagedTable<CouponDto> table;
    private boolean submitting;

    public StoreCouponPanel(BasePage page, StoreClientService service) {
        super("优惠券", ""); this.page = page; this.service = service;
        code.setEditable(false); claim.setEnabled(false);
        table = new AsyncPagedTable<>("优惠券列表", "", "名称或编码", new String[0],
                new String[]{"编码", "名称", "门槛", "减免", "过期时间", "状态"},
                (p, k, f) -> PageSlice.filter(service.listCoupons().getItems(), k, p, 20,
                        value -> value.getCode() + " " + value.getName()),
                value -> new Object[]{value.getCode(), value.getName(), value.getThreshold(),
                        value.getDiscountAmount(), RealUi.dateTime(value.getExpiresAt()), status(value)}, this::select);
        table.setItemKey(CouponDto::getId); table.setLiveSelectionUpdates(true);
        claim.addActionListener(e -> claim());
        JPanel fields = UiFactory.horizontal(8);
        fields.add(UiFactory.labelledField("当前选择", code)); fields.add(claim); fields.add(state);
        JPanel body = UiFactory.vertical(10); body.add(fields); body.add(table); setContent(body);
    }

    private static boolean expired(CouponDto value) {
        return value.getExpiresAt() != null && !value.getExpiresAt().isAfter(org.threeten.bp.LocalDateTime.now());
    }
    private static String status(CouponDto value) {
        return value.isUsed() ? "已使用" : expired(value) ? "已过期" : value.isClaimed() ? "已领取" : "可领取";
    }
    private void select(CouponDto value) {
        code.setText(value == null ? "" : value.getCode());
        claim.setEnabled(!submitting && value != null && !value.isClaimed() && !value.isUsed() && !expired(value));
        if (!submitting) state.setText(value == null ? "请选择优惠券" : status(value));
    }
    private void claim() {
        CouponDto selected = table.selectedItem();
        if (submitting || selected == null || selected.isClaimed() || selected.isUsed() || expired(selected)) return;
        submitting = true; claim.setEnabled(false); state.setText("正在领取…");
        AsyncTask.run(() -> service.claimCoupon(new CouponClaimRequest(selected.getCode())),
                new AsyncTask.Callback<CouponDto>() {
                    @Override public void onSuccess(CouponDto value) {
                        submitting = false; state.setText("优惠券已领取"); table.reload();
                    }
                    @Override public void onFailure(Throwable error) {
                        submitting = false; select(table.selectedItem()); state.setText(AsyncTask.message(error));
                        page.showError(AsyncTask.message(error));
                    }
                });
    }
}
