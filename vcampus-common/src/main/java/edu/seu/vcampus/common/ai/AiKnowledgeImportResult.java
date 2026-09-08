package edu.seu.vcampus.common.ai;

import java.io.Serializable;

public final class AiKnowledgeImportResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int imported;
    private final int skipped;
    public AiKnowledgeImportResult(int imported, int skipped) {
        this.imported = imported; this.skipped = skipped;
    }
    public int getImported() { return imported; }
    public int getSkipped() { return skipped; }
}
