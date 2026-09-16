package edu.seu.vcampus.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** javaw 启动时也能保留的客户端异常日志。 */
final class ClientFailureLog {
    private static final Object LOCK = new Object();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ClientFailureLog() {
    }

    static void installUncaughtExceptionHandler() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, failure) -> {
            record("线程 " + thread.getName(), failure);
            if (previous != null) previous.uncaughtException(thread, failure);
        });
    }

    static void record(String operation, Throwable failure) {
        if (failure == null) return;
        synchronized (LOCK) {
            Path log = logPath();
            try {
                Files.createDirectories(log.getParent());
                try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(log,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND))) {
                    writer.println("[" + LocalDateTime.now().format(TIME) + "] " + operation);
                    failure.printStackTrace(writer);
                    writer.println();
                }
            } catch (IOException | SecurityException ignored) {
                failure.printStackTrace();
            }
        }
    }

    static Path logPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home", "."))
                : Paths.get(localAppData);
        return base.resolve("VCampus").resolve("logs").resolve("client-error.log");
    }
}
