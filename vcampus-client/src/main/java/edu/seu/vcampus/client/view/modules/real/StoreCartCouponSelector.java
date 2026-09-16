package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.store.StoreClientService;
import edu.seu.vcampus.common.dto.store.CouponClaimRequest;
import edu.seu.vcampus.common.dto.store.CouponDto;
import edu.seu.vcampus.common.dto.store.CouponPage;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComboBox;

/** 购物车优惠券选择器：只显示当前金额可用的券，未领取时自动领取。 */
final class StoreCartCouponSelector extends JComboBox<StoreCartCouponSelector.Choice> {
    interface Listener {
        void onReady();
        void onBusy(boolean value);
        void onFailure(Throwable error);
    }

    private final StoreClientService service;
    private final Listener listener;
    private List<CouponDto> coupons = Collections.emptyList();
    private BigDecimal total = BigDecimal.ZERO;
    private int serial;
    private boolean updating;
    private boolean busy;
    private boolean interactionEnabled;

    StoreCartCouponSelector(StoreClientService service, Listener listener) {
        this.service = service; this.listener = listener;
        setPreferredSize(new Dimension(230, 36));
        addActionListener(e -> selected());
        render(null);
    }

    void load(BigDecimal cartTotal) {
        total = amount(cartTotal); final int request = ++serial;
        AsyncTask.run(() -> service.listCoupons(), new AsyncTask.Callback<CouponPage>() {
            @Override public void onSuccess(CouponPage value) {
                if (request != serial) return;
                coupons = value == null ? Collections.emptyList() : value.getItems();
                render(code());
            }
            @Override public void onFailure(Throwable error) {
                if (request != serial) return;
                coupons = Collections.emptyList(); render(null);
            }
        });
    }

    void updateTotal(BigDecimal cartTotal) { total = amount(cartTotal); render(code()); }
    String code() {
        Choice choice = (Choice) getSelectedItem();
        return choice == null || choice.value == null ? null : choice.value.getCode();
    }
    void reset() { updating = true; choose(null); updating = false; }
    void setInteractionEnabled(boolean value) {
        interactionEnabled = value; setEnabled(value && !busy);
    }

    private void selected() {
        if (updating || busy || !interactionEnabled) return;
        Choice choice = (Choice) getSelectedItem();
        if (choice != null && choice.value != null && !choice.value.isClaimed()) {
            claim(choice.value); return;
        }
        listener.onReady();
    }
    private void claim(final CouponDto value) {
        setBusy(true);
        AsyncTask.run(() -> service.claimCoupon(new CouponClaimRequest(value.getCode())),
                new AsyncTask.Callback<CouponDto>() {
                    @Override public void onSuccess(CouponDto claimed) {
                        replace(claimed); render(claimed.getCode()); setBusy(false); listener.onReady();
                    }
                    @Override public void onFailure(Throwable error) {
                        render(null); setBusy(false); listener.onFailure(error);
                    }
                });
    }
    private void replace(CouponDto value) {
        ArrayList<CouponDto> updated = new ArrayList<CouponDto>(coupons);
        for (int i = 0; i < updated.size(); i++) if (updated.get(i).getId() == value.getId()) {
            updated.set(i, value); coupons = updated; return;
        }
        updated.add(value); coupons = updated;
    }
    private void render(String selectedCode) {
        updating = true; removeAllItems(); addItem(Choice.none());
        for (CouponDto value : coupons) if (usable(value)) addItem(new Choice(value));
        choose(selectedCode); updating = false;
    }
    private boolean usable(CouponDto value) {
        return value != null && !value.isUsed()
                && (value.getExpiresAt() == null || value.getExpiresAt().isAfter(org.threeten.bp.LocalDateTime.now()))
                && (value.getThreshold() == null || total.compareTo(value.getThreshold()) >= 0);
    }
    private void choose(String selectedCode) {
        for (int i = 0; i < getItemCount(); i++) {
            Choice choice = getItemAt(i);
            if (selectedCode == null ? choice.value == null
                    : choice.value != null && selectedCode.equals(choice.value.getCode())) {
                setSelectedIndex(i); return;
            }
        }
        if (getItemCount() > 0) setSelectedIndex(0);
    }
    private void setBusy(boolean value) {
        busy = value; setEnabled(interactionEnabled && !value); listener.onBusy(value);
    }
    private static BigDecimal amount(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    static final class Choice {
        private final CouponDto value;
        private Choice(CouponDto value) { this.value = value; }
        private static Choice none() { return new Choice(null); }
        @Override public String toString() {
            if (value == null) return "不使用优惠券";
            return value.getName() + "（满¥" + money(value.getThreshold())
                    + "减¥" + money(value.getDiscountAmount()) + "）";
        }
        private static String money(BigDecimal value) {
            return value == null ? "0.00" : value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
    }
}
