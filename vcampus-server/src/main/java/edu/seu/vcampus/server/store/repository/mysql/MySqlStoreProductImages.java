package edu.seu.vcampus.server.store.repository.mysql;

import edu.seu.vcampus.server.store.repository.StoreRepositoryException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;

/** 商品原图与缩略图的独立持久化入口。 */
final class MySqlStoreProductImages {
    byte[] find(Connection connection, String reference, boolean manager, String variant) {
        String sql = "SELECT i.id,i.original_data,i.thumbnail_data FROM store_product_images i "
                + "JOIN products p ON p.id=i.product_id WHERE i.reference=?"
                + (manager ? "" : " AND p.status='ON_SALE'");
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setString(1, reference);
            try (ResultSet result = prepared.executeQuery()) {
                if (result.next()) {
                    long id = result.getLong("id");
                    byte[] original = result.getBytes("original_data");
                    if (!"THUMBNAIL".equals(variant)) return original;
                    byte[] small = result.getBytes("thumbnail_data");
                    if (small != null) return small;
                    small = thumbnail(original);
                    saveThumbnail(connection, id, small);
                    return small;
                }
            }
            return legacy(connection, reference, manager);
        } catch (SQLException ex) {
            throw new StoreRepositoryException("读取商品图片失败", ex);
        }
    }

    private static void saveThumbnail(Connection connection, long id, byte[] data)
            throws SQLException {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE store_product_images SET thumbnail_data=? WHERE id=?")) {
            update.setBytes(1, data); update.setLong(2, id); update.executeUpdate();
        }
    }

    void sync(Connection connection, long productId, String reference, byte[] original)
            throws SQLException {
        if (original == null && reference != null && reference.startsWith("store-image:")) return;
        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM store_product_images WHERE product_id=?")) {
            delete.setLong(1, productId); delete.executeUpdate();
        }
        if (original == null) return;
        String sql = "INSERT INTO store_product_images(product_id,reference,original_data,"
                + "thumbnail_data,is_primary,sort_order) VALUES(?,?,?,?,1,0)";
        try (PreparedStatement insert = connection.prepareStatement(sql)) {
            insert.setLong(1, productId); insert.setString(2, reference);
            insert.setBytes(3, original); insert.setBytes(4, thumbnail(original));
            insert.executeUpdate();
        }
    }

    private static byte[] legacy(Connection connection, String reference, boolean manager)
            throws SQLException {
        String sql = "SELECT image_data FROM products WHERE image_url=?"
                + (manager ? "" : " AND status='ON_SALE'");
        try (PreparedStatement prepared = connection.prepareStatement(sql)) {
            prepared.setString(1, reference);
            try (ResultSet result = prepared.executeQuery()) {
                return result.next() ? result.getBytes(1) : null;
            }
        }
    }

    private static byte[] thumbnail(byte[] original) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) throw new IllegalArgumentException("图片无法解码");
            BufferedImage target = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, 320, 240);
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                double scale = Math.min(320d / source.getWidth(), 240d / source.getHeight());
                int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
                int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
                graphics.drawImage(source, (320 - width) / 2, (240 - height) / 2,
                        width, height, null);
            } finally { graphics.dispose(); }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(target, "jpg", output); return output.toByteArray();
        } catch (Exception ex) {
            throw new StoreRepositoryException("生成商品缩略图失败", ex);
        }
    }
}
