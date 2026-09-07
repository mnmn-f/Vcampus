package edu.seu.vcampus.server.library.bootstrap;

import edu.seu.vcampus.server.db.JdbcConnectionFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 启动时补齐图书馆扩展结构和演示馆藏。
 *
 * <p>V1/V2 仍负责创建基础数据库和账号；本类只处理图书详情、借还库存一致性
 * 以及 PDF 资源所需的增量结构。所有操作都可重复执行。</p>
 */
public final class LibrarySchemaBootstrap {
    private static final String AUTO_UPGRADE_PROPERTY =
            "vcampus.library.schema.auto-upgrade";

    private LibrarySchemaBootstrap() {
    }

    public static void initialize(JdbcConnectionFactory connections)
            throws SQLException, IOException {
        if (!Boolean.parseBoolean(System.getProperty(AUTO_UPGRADE_PROPERTY, "true"))) {
            return;
        }
        if (connections == null) throw new IllegalArgumentException("connections is required");
        try (Connection connection = connections.open()) {
            requireBaseTable(connection, "books");
            ensureColumn(connection, "books", "publication_year",
                    "ALTER TABLE books ADD COLUMN publication_year INT NULL");
            ensureColumn(connection, "books", "cover_image",
                    "ALTER TABLE books ADD COLUMN cover_image MEDIUMBLOB NULL");
            createPdfTables(connection);
            seedBooks(connection);
            reconcileInventory(connection);
        }
        System.out.println("[INFO] 图书馆数据库结构与馆藏数据已就绪");
    }

    private static void requireBaseTable(Connection connection, String table)
            throws SQLException {
        if (!tableExists(connection, table)) {
            throw new SQLException("缺少基础表 " + table
                    + "，请先执行 V1__baseline.sql 和 V2__demo_data.sql");
        }
    }

    private static boolean tableExists(Connection connection, String table)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema=DATABASE() AND table_name=?")) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) > 0;
            }
        }
    }

    private static void ensureColumn(Connection connection, String table,
                                     String column, String ddl) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema=DATABASE() AND table_name=? AND column_name=?")) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getInt(1) > 0) return;
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
        }
    }

    private static void createPdfTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS library_pdf_resources ("
                    + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "title VARCHAR(240) NOT NULL,description TEXT NULL,"
                    + "file_name VARCHAR(200) NOT NULL,file_size BIGINT NOT NULL,"
                    + "sha256 CHAR(64) NOT NULL,uploader_id BIGINT UNSIGNED NOT NULL,"
                    + "uploader_name VARCHAR(160) NOT NULL,"
                    + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING',"
                    + "uploaded_at BIGINT NOT NULL,reviewer_id BIGINT NOT NULL DEFAULT 0,"
                    + "reviewer_name VARCHAR(160) NULL,reviewed_at BIGINT NOT NULL DEFAULT 0,"
                    + "rejection_reason VARCHAR(1000) NULL,"
                    + "KEY idx_pdf_public(status,id),KEY idx_pdf_owner(uploader_id,id),"
                    + "CONSTRAINT fk_pdf_uploader FOREIGN KEY(uploader_id) REFERENCES users(id),"
                    + "CONSTRAINT ck_pdf_status CHECK(status IN "
                    + "('PENDING','APPROVED','REJECTED','INACTIVE')),"
                    + "CONSTRAINT ck_pdf_size CHECK(file_size BETWEEN 8 AND 52428800)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS library_pdf_downloads ("
                    + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "user_id BIGINT UNSIGNED NOT NULL,resource_id BIGINT UNSIGNED NOT NULL,"
                    + "title VARCHAR(240) NOT NULL,file_name VARCHAR(200) NOT NULL,"
                    + "file_size BIGINT NOT NULL,sha256 CHAR(64) NOT NULL,"
                    + "transferred BIGINT NOT NULL DEFAULT 0,status VARCHAR(20) NOT NULL,"
                    + "started_at BIGINT NOT NULL,finished_at BIGINT NOT NULL DEFAULT 0,"
                    + "failure_reason VARCHAR(1000) NULL,"
                    + "KEY idx_pdf_download_user(user_id,id),"
                    + "CONSTRAINT fk_pdf_download_user FOREIGN KEY(user_id) REFERENCES users(id),"
                    + "CONSTRAINT fk_pdf_download_resource FOREIGN KEY(resource_id) "
                    + "REFERENCES library_pdf_resources(id),"
                    + "CONSTRAINT ck_pdf_download_status CHECK(status IN "
                    + "('DOWNLOADING','COMPLETED','FAILED'))"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
        }
    }

    private static void seedBooks(Connection connection) throws SQLException, IOException {
        Long creator = librarianId(connection);
        String sql = "INSERT INTO books (isbn,title,author,publisher,category,total_copies,"
                + "available_copies,location,description,status,publication_year,cover_image,created_by) "
                + "VALUES (?,?,?,?,?,?,?,?,?,'ON_SHELF',?,?,?) ON DUPLICATE KEY UPDATE "
                + "title=VALUES(title),author=VALUES(author),publisher=VALUES(publisher),"
                + "category=VALUES(category),location=VALUES(location),description=VALUES(description),"
                + "publication_year=COALESCE(books.publication_year,VALUES(publication_year)),"
                + "cover_image=COALESCE(books.cover_image,VALUES(cover_image))";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SeedBook book : books()) {
                statement.setString(1, book.isbn);
                statement.setString(2, book.title);
                statement.setString(3, book.author);
                statement.setString(4, book.publisher);
                statement.setString(5, book.category);
                statement.setInt(6, book.copies);
                statement.setInt(7, book.copies);
                statement.setString(8, book.location);
                statement.setString(9, book.description);
                statement.setInt(10, book.year);
                statement.setBytes(11, resource("/library/covers/" + book.cover));
                if (creator == null) statement.setNull(12, java.sql.Types.BIGINT);
                else statement.setLong(12, creator.longValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static Long librarianId(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM users WHERE username='demo_librarian' LIMIT 1");
             ResultSet result = statement.executeQuery()) {
            return result.next() ? Long.valueOf(result.getLong(1)) : null;
        }
    }

    /** 修复重复导入演示数据后可能出现的库存与未归还记录不一致。 */
    private static void reconcileInventory(Connection connection) throws SQLException {
        String sql = "UPDATE books b LEFT JOIN (SELECT book_id,COUNT(*) active_count "
                + "FROM borrow_records WHERE status IN ('BORROWED','OVERDUE','LOST') "
                + "GROUP BY book_id) active ON active.book_id=b.id "
                + "SET b.available_copies=GREATEST(0,b.total_copies-COALESCE(active.active_count,0)) "
                + "WHERE b.available_copies<>GREATEST(0,b.total_copies-COALESCE(active.active_count,0))";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static byte[] resource(String path) throws IOException {
        try (InputStream input = LibrarySchemaBootstrap.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("缺少图书封面资源：" + path);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) >= 0;) {
                if (read > 0) output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static SeedBook[] books() {
        return new SeedBook[]{
                new SeedBook("DEMO-978000000001", "软件工程实践导论", "VCampus 编写组",
                        "东南大学出版社", "计算机", 2026, 4, "二楼 A区-01-01",
                        "从需求分析、架构设计到测试交付的软件工程实践入门。", "01-software.png"),
                new SeedBook("9787111213826", "Java 核心技术", "Cay S. Horstmann",
                        "机械工业出版社", "计算机", 2022, 5, "二楼 A区-01-02",
                        "Java 语言基础、面向对象、集合与并发编程参考书。", "02-java.png"),
                new SeedBook("9787115428028", "算法图解", "Aditya Bhargava",
                        "人民邮电出版社", "计算机", 2021, 4, "二楼 A区-02-01",
                        "用图示方式介绍查找、排序、图算法和动态规划。", "03-algorithm.png"),
                new SeedBook("9787111544937", "深入理解计算机系统", "Randal E. Bryant",
                        "机械工业出版社", "计算机", 2016, 3, "二楼 A区-02-02",
                        "从程序员视角理解处理器、存储器、链接与并发。", "04-systems.png"),
                new SeedBook("9787040406641", "数据库系统概论", "王珊、萨师煊",
                        "高等教育出版社", "计算机", 2014, 5, "二楼 A区-03-01",
                        "数据库原理、SQL、设计方法、恢复和并发控制基础。", "05-database.png"),
                new SeedBook("9787020002207", "红楼梦", "曹雪芹",
                        "人民文学出版社", "文学", 2020, 4, "三楼 B区-01-01",
                        "中国古典文学名著。", "06-red-chamber.png"),
                new SeedBook("9787101003048", "史记", "司马迁",
                        "中华书局", "历史", 2019, 3, "三楼 B区-02-01",
                        "中国纪传体通史经典。", "07-history.png"),
                new SeedBook("9787544270878", "百年孤独", "加西亚·马尔克斯",
                        "南海出版公司", "文学", 2017, 4, "三楼 B区-03-01",
                        "魔幻现实主义文学代表作。", "08-solitude.png"),
                new SeedBook("9787506365437", "活着", "余华",
                        "作家出版社", "文学", 2012, 5, "三楼 B区-03-02",
                        "关于生命韧性与时代变迁的长篇小说。", "09-living.png")
        };
    }

    private static final class SeedBook {
        private final String isbn;
        private final String title;
        private final String author;
        private final String publisher;
        private final String category;
        private final int year;
        private final int copies;
        private final String location;
        private final String description;
        private final String cover;

        private SeedBook(String isbn, String title, String author, String publisher,
                         String category, int year, int copies, String location,
                         String description, String cover) {
            this.isbn = isbn;
            this.title = title;
            this.author = author;
            this.publisher = publisher;
            this.category = category;
            this.year = year;
            this.copies = copies;
            this.location = location;
            this.description = description;
            this.cover = cover;
        }
    }
}
