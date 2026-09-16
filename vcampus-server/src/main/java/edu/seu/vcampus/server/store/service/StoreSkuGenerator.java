package edu.seu.vcampus.server.store.service;

import edu.seu.vcampus.common.dto.store.ProductWriteRequest;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.store.repository.StoreRecordRepository;

import java.sql.Connection;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/** 按商品分类生成便于识别的唯一业务编码。 */
final class StoreSkuGenerator {
    private final ConcurrentMap<String, AtomicLong> sequences =
            new ConcurrentHashMap<String, AtomicLong>();

    ProductWriteRequest generate(StoreRecordRepository repository, Connection connection,
                                 ProductWriteRequest request) throws StoreServiceException {
        String category = StoreServiceSupport.required(request.getCategory(), "商品分类")
                .trim().toUpperCase(Locale.ROOT);
        if (!category.matches("[A-Z0-9]+")) {
            throw new StoreServiceException(ResultCodes.INVALID_INPUT, "商品分类不正确");
        }
        AtomicLong sequence = sequences.putIfAbsent(category, new AtomicLong(1L));
        if (sequence == null) sequence = sequences.get(category);
        for (int attempt = 0; attempt < 1000000; attempt++) {
            String sku = String.format(Locale.ROOT, "SEU-%s-%03d", category,
                    sequence.getAndIncrement());
            if (!repository.skuExists(connection, sku, 0L)) return copy(request, sku);
        }
        throw new StoreServiceException(ResultCodes.CONFLICT, "无法生成商品编码");
    }

    private static ProductWriteRequest copy(ProductWriteRequest request, String sku) {
        return new ProductWriteRequest(0L, sku, request.getName(), request.getCategory(),
                request.getDescription(), request.getPrice(), request.getStockQty(),
                request.getStatus(), request.getImageUrl(), request.getImageData());
    }
}
