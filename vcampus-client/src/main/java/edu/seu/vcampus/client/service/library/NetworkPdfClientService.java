package edu.seu.vcampus.client.service.library;

import edu.seu.vcampus.client.network.NetworkClientService;
import edu.seu.vcampus.client.session.ClientSession;
import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.PageResult;
import edu.seu.vcampus.common.dto.library.PdfResourceView;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.dto.library.PdfUploadId;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfDownloadRecord;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;
import edu.seu.vcampus.common.protocol.command.PdfCommands;

/** 将 PDF 操作映射到经过鉴权的 TCP 命令。 */
public final class NetworkPdfClientService implements PdfClientService {
    private final NetworkClientService network;
    private final ClientSession session;
    private final String token;
    public NetworkPdfClientService(NetworkClientService network, ClientSession session) {
        this.network = network; this.session = session;
        this.token = session == null ? network.getSessionToken() : session.getSessionToken();
    }
    private Object request(String command, java.io.Serializable body) throws Exception {
        if (session != null && (!session.isAuthenticated() || !token.equals(session.getSessionToken())))
            throw new IllegalStateException("登录会话已结束，请重新登录后操作");
        return network.requestWithToken(command, body, token).getPayload();
    }
    @SuppressWarnings("unchecked")
    @Override public PageResult<PdfResourceView> list(PdfQuery query) throws Exception { return (PageResult<PdfResourceView>) request(PdfCommands.LIST, query); }
    @Override public PdfResourceView detail(long id) throws Exception { return (PdfResourceView) request(PdfCommands.DETAIL, new LibraryIdRequest(id)); }
    @Override public String beginUpload(PdfUploadRequest request) throws Exception { return (String) request(PdfCommands.UPLOAD_BEGIN, request); }
    @Override public long uploadChunk(PdfUploadChunk chunk) throws Exception { return (Long) request(PdfCommands.UPLOAD_CHUNK, chunk); }
    @Override public PdfResourceView commitUpload(String id) throws Exception { return (PdfResourceView) request(PdfCommands.UPLOAD_COMMIT, new PdfUploadId(id)); }
    @Override public void abortUpload(String id) throws Exception { request(PdfCommands.UPLOAD_ABORT, new PdfUploadId(id)); }
    @Override public PdfResourceView review(PdfReviewRequest request) throws Exception { return (PdfResourceView) request(PdfCommands.REVIEW, request); }
    @Override public PdfDownloadRecord beginDownload(long id) throws Exception { return (PdfDownloadRecord) request(PdfCommands.DOWNLOAD_BEGIN, new LibraryIdRequest(id)); }
    @Override public byte[] downloadChunk(PdfDownloadChunk request) throws Exception { return (byte[]) request(PdfCommands.DOWNLOAD_CHUNK, request); }
    @Override public PdfDownloadRecord finishDownload(PdfDownloadFinish request) throws Exception { return (PdfDownloadRecord) request(PdfCommands.DOWNLOAD_FINISH, request); }
    @SuppressWarnings("unchecked")
    @Override public PageResult<PdfDownloadRecord> history(PdfQuery query) throws Exception { return (PageResult<PdfDownloadRecord>) request(PdfCommands.DOWNLOAD_HISTORY, query); }
}
