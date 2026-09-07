package edu.seu.vcampus.server.library.repository;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import java.sql.Connection;

/** PDF 元数据、审核结果及下载历史；事务由服务层管理。 */
public interface PdfRepository {
    PageResult<PdfResourceView> list(Connection c, PdfQuery query, long owner) throws Exception;
    PdfResourceView find(Connection c, long id, boolean lock) throws Exception;
    PdfResourceView save(Connection c, PdfResourceView value) throws Exception;
    PdfDownloadRecord download(Connection c, long id, boolean lock) throws Exception;
    PdfDownloadRecord saveDownload(Connection c, PdfDownloadRecord value) throws Exception;
    PageResult<PdfDownloadRecord> history(Connection c, long owner, PdfQuery query) throws Exception;
}
