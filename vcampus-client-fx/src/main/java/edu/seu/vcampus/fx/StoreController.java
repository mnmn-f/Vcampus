package edu.seu.vcampus.fx;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;

/** 校园商店页控制器，提供分类、关键词筛选和本地购物车反馈。 */
public final class StoreController {
    private static final List<Product> PRODUCTS = List.of(
            new Product("校园咖啡", "饮品", "¥18.00", "COFFEE", "现磨咖啡与燕麦奶的平衡风味",
                    "https://images.unsplash.com/photo-1615486780246-76e6bb33e8b5?auto=format&fit=crop&w=700&q=75"),
            new Product("东大帆布袋", "文创", "¥29.00", "CAMPUS", "轻便耐用，适合日常通勤",
                    "https://images.unsplash.com/photo-1571029068328-35c3c82907ab?auto=format&fit=crop&w=700&q=75"),
            new Product("便携充电宝", "数码", "¥59.00", "TECH", "10000mAh，支持双向快充",
                    "https://images.unsplash.com/photo-1525858907241-d230b66fb9fa?auto=format&fit=crop&w=700&q=75"),
            new Product("校园纪念徽章", "文创", "¥12.00", "BADGE", "东大校园建筑系列",
                    "https://images.unsplash.com/photo-1592280771190-3e2e4d571952?auto=format&fit=crop&w=700&q=75"));
    @FXML private TextField searchField;
    @FXML private FlowPane productGrid;
    @FXML private Label cartCount;
    @FXML private Label cartTotal;
    private String category = "全部商品";
    private int itemCount;
    private double total;

    @FXML
    private void initialize() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> render());
        render();
    }

    @FXML
    private void showAll() { category = "全部商品"; render(); }

    @FXML
    private void showFood() { category = "饮品"; render(); }

    @FXML
    private void showCulture() { category = "文创"; render(); }

    @FXML
    private void showTech() { category = "数码"; render(); }

    private void render() {
        String keyword = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        productGrid.getChildren().clear();
        PRODUCTS.stream().filter(product -> matches(product, keyword))
                .forEach(product -> productGrid.getChildren().add(card(product)));
    }

    private boolean matches(Product product, String keyword) {
        return ("全部商品".equals(category) || category.equals(product.category()))
                && (keyword.isEmpty() || product.name().toLowerCase().contains(keyword)
                || product.description().toLowerCase().contains(keyword));
    }

    private Node card(Product product) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");
        StackPane image = new StackPane();
        image.getStyleClass().addAll("product-image", "image-" + product.imageKey().toLowerCase());
        Label fallback = new Label(product.name());
        fallback.getStyleClass().add("image-fallback");
        ImageView imageView = new ImageView();
        imageView.setFitWidth(205);
        imageView.setFitHeight(118);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setClip(new Rectangle(205, 118));
        Image remoteImage = new Image(product.imageUrl(), 205, 118, false, true, true);
        imageView.setImage(remoteImage);
        fallback.setVisible(true);
        fallback.setManaged(true);
        imageView.setVisible(false);
        imageView.setManaged(false);
        remoteImage.progressProperty().addListener((observable, oldValue, progress) -> {
            if (progress.doubleValue() >= 1.0 && !remoteImage.isError()) {
                fallback.setVisible(false);
                fallback.setManaged(false);
                imageView.setVisible(true);
                imageView.setManaged(true);
            }
        });
        remoteImage.errorProperty().addListener((observable, oldValue, error) -> {
            if (error) {
                fallback.setText(product.name());
                fallback.setVisible(true);
                fallback.setManaged(true);
                imageView.setVisible(false);
                imageView.setManaged(false);
            }
        });
        image.getChildren().addAll(imageView, fallback);
        Label categoryLabel = new Label(product.category());
        categoryLabel.getStyleClass().add("eyebrow");
        Label title = new Label(product.name());
        title.getStyleClass().add("product-title");
        Label description = new Label(product.description());
        description.getStyleClass().add("muted");
        description.setWrapText(true);
        Label price = new Label(product.price());
        price.getStyleClass().add("price");
        Button add = new Button("加入购物车");
        add.getStyleClass().add("small-primary");
        add.setOnAction(event -> add(product, add));
        card.getChildren().addAll(image, categoryLabel, title, description, price, add);
        return card;
    }

    private void add(Product product, Button source) {
        itemCount++;
        total += Double.parseDouble(product.price().substring(1));
        cartCount.setText(Integer.toString(itemCount));
        cartTotal.setText(String.format("¥%.2f", total));
        source.setText("已加入");
        source.setDisable(true);
    }

    FlowPane productGridForTest() {
        return productGrid;
    }

    static List<Product> productsForTest() {
        return PRODUCTS;
    }

    record Product(String name, String category, String price, String imageKey, String description,
                   String imageUrl) {
    }
}
