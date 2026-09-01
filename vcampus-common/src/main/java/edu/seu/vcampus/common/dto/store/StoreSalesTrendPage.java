package edu.seu.vcampus.common.dto.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 销售趋势结果。 */
public final class StoreSalesTrendPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<StoreSalesTrendDto> items;
    public StoreSalesTrendPage(List<StoreSalesTrendDto> items) {
        this.items = Collections.unmodifiableList(new ArrayList<StoreSalesTrendDto>(
                items == null ? Collections.<StoreSalesTrendDto>emptyList() : items));
    }
    public List<StoreSalesTrendDto> getItems() { return items; }
}
