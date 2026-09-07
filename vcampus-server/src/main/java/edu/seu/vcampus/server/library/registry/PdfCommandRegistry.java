package edu.seu.vcampus.server.library.registry;

import edu.seu.vcampus.server.library.service.PdfLibraryService;
import edu.seu.vcampus.server.library.handler.LibraryCommandHandler;
import edu.seu.vcampus.server.router.CommandRouter;
import edu.seu.vcampus.common.security.Permission;
import edu.seu.vcampus.common.protocol.command.PdfCommands;
import edu.seu.vcampus.common.dto.library.LibraryIdRequest;
import edu.seu.vcampus.common.dto.library.PdfQuery;
import edu.seu.vcampus.common.dto.library.PdfUploadRequest;
import edu.seu.vcampus.common.dto.library.PdfUploadChunk;
import edu.seu.vcampus.common.dto.library.PdfUploadId;
import edu.seu.vcampus.common.dto.library.PdfReviewRequest;
import edu.seu.vcampus.common.dto.library.PdfDownloadChunk;
import edu.seu.vcampus.common.dto.library.PdfDownloadFinish;

/** 将文件命令接入现有认证路由。 */
public final class PdfCommandRegistry {
    private PdfCommandRegistry() { }
    public static CommandRouter register(CommandRouter router, PdfLibraryService service) {
        router.register(PdfCommands.LIST, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfQuery body = LibraryCommandHandler.payload(p, PdfQuery.class);
            return service.list(s, body);
        }));
        router.register(PdfCommands.DETAIL, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            LibraryIdRequest body = LibraryCommandHandler.payload(p, LibraryIdRequest.class);
            return service.detail(s, body.getId());
        }));
        router.register(PdfCommands.UPLOAD_BEGIN, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfUploadRequest body = LibraryCommandHandler.payload(p, PdfUploadRequest.class);
            return service.beginUpload(s, body);
        }));
        router.register(PdfCommands.UPLOAD_CHUNK, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfUploadChunk body = LibraryCommandHandler.payload(p, PdfUploadChunk.class);
            return service.uploadChunk(s, body);
        }));
        router.register(PdfCommands.UPLOAD_COMMIT, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfUploadId body = LibraryCommandHandler.payload(p, PdfUploadId.class);
            return service.commitUpload(s, body.getUploadId());
        }));
        router.register(PdfCommands.UPLOAD_ABORT, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfUploadId body = LibraryCommandHandler.payload(p, PdfUploadId.class);
            return service.abortUpload(s, body.getUploadId());
        }));
        router.register(PdfCommands.REVIEW, new LibraryCommandHandler(Permission.LIBRARY_MANAGE, (p, s) -> {
            PdfReviewRequest body = LibraryCommandHandler.payload(p, PdfReviewRequest.class);
            return service.review(s, body);
        }));
        router.register(PdfCommands.DOWNLOAD_BEGIN, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            LibraryIdRequest body = LibraryCommandHandler.payload(p, LibraryIdRequest.class);
            return service.downloads().begin(s, body.getId());
        }));
        router.register(PdfCommands.DOWNLOAD_CHUNK, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfDownloadChunk body = LibraryCommandHandler.payload(p, PdfDownloadChunk.class);
            return service.downloads().chunk(s, body);
        }));
        router.register(PdfCommands.DOWNLOAD_FINISH, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfDownloadFinish body = LibraryCommandHandler.payload(p, PdfDownloadFinish.class);
            return service.downloads().finish(s, body);
        }));
        router.register(PdfCommands.DOWNLOAD_HISTORY, new LibraryCommandHandler(Permission.LIBRARY_READ, (p, s) -> {
            PdfQuery body = LibraryCommandHandler.payload(p, PdfQuery.class);
            return service.downloads().history(s, body);
        }));
        return router;
    }
}
