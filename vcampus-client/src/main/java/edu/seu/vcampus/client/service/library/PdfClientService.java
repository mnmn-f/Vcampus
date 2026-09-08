package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;

/** 学生和管理员共用的 PDF 客户端边界。 */
public interface PdfClientService {
    PageResult<PdfResourceView> list(PdfQuery query) throws Exception;
    PdfResourceView detail(long id) throws Exception;
    String beginUpload(PdfUploadRequest request) throws Exception;
    long uploadChunk(PdfUploadChunk chunk) throws Exception;
    PdfResourceView commitUpload(String id) throws Exception;
    void abortUpload(String id) throws Exception;
    PdfResourceView review(PdfReviewRequest request) throws Exception;
    PdfDownloadRecord beginDownload(long id) throws Exception;
    byte[] downloadChunk(PdfDownloadChunk request) throws Exception;
    PdfDownloadRecord finishDownload(PdfDownloadFinish request) throws Exception;
    PageResult<PdfDownloadRecord> history(PdfQuery query) throws Exception;
}
