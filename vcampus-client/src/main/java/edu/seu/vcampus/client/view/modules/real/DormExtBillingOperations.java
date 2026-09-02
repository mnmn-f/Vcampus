package edu.seu.vcampus.client.view.modules.real;

import edu.seu.vcampus.client.service.dorm.ext.DormExtClientService;
import edu.seu.vcampus.client.view.BasePage;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateRequest;
import edu.seu.vcampus.common.dto.dorm.ext.BillGenerateResultDto;
import edu.seu.vcampus.common.dto.dorm.ext.MeterReadingDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 将抄表选中项转换为出账请求，避免面板同时承担批处理逻辑。 */
final class DormExtBillingOperations {
    private final BasePage page;
    private final DormExtClientService service;
    private final Runnable reload;

    DormExtBillingOperations(BasePage page, DormExtClientService service, Runnable reload) {
        this.page = page;
        this.service = service;
        this.reload = reload;
    }

    void selected(List<MeterReadingDto> rows) {
        if (rows.isEmpty()) { page.showWarning("请先在表格里选中要出账的读数，可多选。"); return; }
        List<BillGenerateRequest> requests = new ArrayList<BillGenerateRequest>();
        int locked = 0;
        for (MeterReadingDto row : rows) {
            if (row.isLocked()) { locked++; continue; }
            requests.add(new BillGenerateRequest(Long.valueOf(row.getRoomId()), row.getPeriodStart(), row.getPeriodEnd(), null));
        }
        if (requests.isEmpty()) { page.showWarning("选中的读数都已出账，无需重复出账。"); return; }
        run(requests, locked);
    }

    void wholePeriod(List<MeterReadingDto> rows) {
        if (rows.isEmpty()) { page.showWarning("请先选中一行，用它的账期作为出账范围。"); return; }
        MeterReadingDto first = rows.get(0);
        List<BillGenerateRequest> requests = new ArrayList<BillGenerateRequest>();
        requests.add(new BillGenerateRequest(null, first.getPeriodStart(), first.getPeriodEnd(), null));
        run(requests, 0);
    }

    private void run(final List<BillGenerateRequest> requests, final int skippedLocked) {
        AsyncTask.run(new AsyncTask.Work<BillGenerateResultDto>() {
            @Override public BillGenerateResultDto run() throws Exception {
                int bills = 0, allocations = 0, skipped = 0;
                BigDecimal total = BigDecimal.ZERO;
                List<String> notes = new ArrayList<String>();
                for (BillGenerateRequest request : requests) {
                    BillGenerateResultDto one = service.generateBills(request);
                    bills += one.getBillCount(); allocations += one.getAllocationCount();
                    skipped += one.getSkippedCount(); total = total.add(one.getTotalAmount());
                    notes.addAll(one.getNotes());
                }
                return new BillGenerateResultDto(bills, allocations, skipped, total, notes);
            }
        }, new AsyncTask.Callback<BillGenerateResultDto>() {
            @Override public void onSuccess(BillGenerateResultDto value) {
                String message = "生成账单 " + value.getBillCount() + " 张、分摊 "
                        + value.getAllocationCount() + " 条，合计 " + value.getTotalAmount() + " 元";
                if (value.getSkippedCount() > 0) message += "，跳过 " + value.getSkippedCount() + " 条";
                if (skippedLocked > 0) message += "；选中项里有 " + skippedLocked + " 条已出账，已忽略";
                page.showSuccess(message + "。"); reload.run();
            }
            @Override public void onFailure(Throwable error) { page.showError(AsyncTask.message(error)); }
        });
    }
}
