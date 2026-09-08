package edu.seu.vcampus.server.ai.service;

import edu.seu.vcampus.common.ai.AiKnowledgeChunk;
import edu.seu.vcampus.common.ai.AiKnowledgeQuery;
import edu.seu.vcampus.common.ai.AiKnowledgeSaveRequest;
import edu.seu.vcampus.common.ai.AiPage;
import edu.seu.vcampus.common.protocol.ResultCodes;
import edu.seu.vcampus.server.ai.repository.AiKnowledgeRepository;
import edu.seu.vcampus.server.db.TransactionManager;
import edu.seu.vcampus.server.db.TransactionWork;
import edu.seu.vcampus.server.security.SessionContext;

import java.sql.Connection;
import java.util.List;
import edu.seu.vcampus.common.ai.AiKnowledgeVersion;
import edu.seu.vcampus.common.ai.AiKnowledgeBatchRequest;
import edu.seu.vcampus.common.ai.AiKnowledgeImportResult;

/** 知识库查询、维护和混合检索。 */
public final class AiKnowledgeService {
    private final AiKnowledgeRepository repository;
    private final TransactionManager transactions;

    public AiKnowledgeService(AiKnowledgeRepository repository,
                              TransactionManager transactions) {
        this.repository = repository; this.transactions = transactions;
    }

    public AiPage<AiKnowledgeChunk> search(final AiKnowledgeQuery query) {
        return tx(new Work<AiPage<AiKnowledgeChunk>>() {
            public AiPage<AiKnowledgeChunk> run(Connection c) throws Exception {
                return repository.search(c, query == null ? new AiKnowledgeQuery() : query);
            }
        });
    }

    public List<AiKnowledgeChunk> retrieve(final String query, final int topK) {
        return tx(new Work<List<AiKnowledgeChunk>>() {
            public List<AiKnowledgeChunk> run(Connection c) throws Exception {
                return repository.retrieve(c, query, topK);
            }
        });
    }

    public AiKnowledgeChunk save(final SessionContext session,
                                 final AiKnowledgeSaveRequest request) {
        validate(request);
        return tx(new Work<AiKnowledgeChunk>() {
            public AiKnowledgeChunk run(Connection c) throws Exception {
                return repository.save(c, request, session.getUserId());
            }
        });
    }

    /** 整批导入共用一个事务，任一片段失败时全部回滚。 */
    public AiKnowledgeImportResult importBatch(final SessionContext session,
                                                final AiKnowledgeBatchRequest batch) {
        if (batch == null || batch.getItems().isEmpty() || batch.getItems().size() > 100) {
            throw invalid("一次请选择 1 至 100 个知识片段");
        }
        for (AiKnowledgeSaveRequest item : batch.getItems()) {
            validate(item);
            if (item.getChunkId() != null) throw invalid("文档导入不能覆盖已有知识片段");
        }
        return tx(new Work<AiKnowledgeImportResult>() {
            public AiKnowledgeImportResult run(Connection c) throws Exception {
                int imported = 0, skipped = 0;
                for (AiKnowledgeSaveRequest item : batch.getItems()) {
                    if (batch.isSkipDuplicates()
                            && repository.duplicate(c, item.getContent())) {
                        skipped++; continue;
                    }
                    repository.save(c, item, session.getUserId()); imported++;
                }
                return new AiKnowledgeImportResult(imported, skipped);
            }
        });
    }

    public void delete(final SessionContext session, final long id) {
        if (id <= 0) throw invalid("知识片段编号不正确");
        tx(new Work<Void>() {
            public Void run(Connection c) throws Exception {
                repository.deactivate(c, id, session.getUserId()); return null;
            }
        });
    }

    public long activeCount() {
        return tx(new Work<Long>() {
            public Long run(Connection c) throws Exception {
                return Long.valueOf(repository.activeCount(c));
            }
        }).longValue();
    }

    public List<AiKnowledgeVersion> versions(final long chunkId) {
        if (chunkId <= 0) throw invalid("知识片段编号不正确");
        return tx(new Work<List<AiKnowledgeVersion>>() {
            public List<AiKnowledgeVersion> run(Connection c) throws Exception {
                return repository.versions(c, chunkId);
            }
        });
    }

    public AiKnowledgeChunk rollback(final SessionContext session, final long chunkId,
            final long versionId) {
        if (chunkId <= 0 || versionId <= 0) throw invalid("知识版本参数不正确");
        return tx(new Work<AiKnowledgeChunk>() {
            public AiKnowledgeChunk run(Connection c) throws Exception {
                return repository.rollback(c, chunkId, versionId, session.getUserId());
            }
        });
    }

    private void validate(AiKnowledgeSaveRequest r) {
        if (r == null || blank(r.getSourceType()) || blank(r.getContent())) {
            throw invalid("来源类型和知识正文不能为空");
        }
        if (r.getContent().length() > 20000) throw invalid("单个知识片段不能超过 20000 字");
        if (r.getSourceType().trim().length() > 40) throw invalid("来源类型不能超过 40 字");
        if (r.getTitle() != null && r.getTitle().trim().length() > 240) {
            throw invalid("知识标题不能超过 240 字");
        }
        if (!("ACTIVE".equals(r.getStatus()) || "INACTIVE".equals(r.getStatus()))) {
            throw invalid("知识状态不正确");
        }
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private AiServiceException invalid(String text) {
        return new AiServiceException(ResultCodes.INVALID_INPUT, text);
    }

    private <T> T tx(final Work<T> work) {
        try {
            return transactions.execute(new TransactionWork<T>() {
                public T execute(Connection c) throws Exception { return work.run(c); }
            });
        } catch (AiServiceException ex) { throw ex; }
        catch (Exception ex) {
            throw new AiServiceException(ResultCodes.INTERNAL_ERROR, "知识库暂时不可用", ex);
        }
    }

    private interface Work<T> { T run(Connection connection) throws Exception; }
}
