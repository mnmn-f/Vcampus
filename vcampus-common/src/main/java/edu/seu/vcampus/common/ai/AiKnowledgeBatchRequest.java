package edu.seu.vcampus.common.ai;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 管理端一次性导入的知识分段；服务端在同一事务中处理。 */
public final class AiKnowledgeBatchRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final List<AiKnowledgeSaveRequest> items;
    private final boolean skipDuplicates;

    public AiKnowledgeBatchRequest(List<AiKnowledgeSaveRequest> items, boolean skipDuplicates) {
        this.items = items == null ? Collections.<AiKnowledgeSaveRequest>emptyList()
                : Collections.unmodifiableList(new ArrayList<AiKnowledgeSaveRequest>(items));
        this.skipDuplicates = skipDuplicates;
    }
    public List<AiKnowledgeSaveRequest> getItems() { return items; }
    public boolean isSkipDuplicates() { return skipDuplicates; }
}
