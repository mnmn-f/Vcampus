package edu.seu.vcampus.fx;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.List;

/** 学籍概览页控制器，展示学生四项成绩指标和学期成绩趋势。 */
public final class AcademicController {
    @FXML private ComboBox<String> termSelector;
    @FXML private Label weightedGpa;
    @FXML private Label averageGpa;
    @FXML private Label weightedScore;
    @FXML private Label averageScore;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private TableView<GradeRow> gradeTable;
    @FXML private TableColumn<GradeRow, String> courseColumn;
    @FXML private TableColumn<GradeRow, String> creditColumn;
    @FXML private TableColumn<GradeRow, String> scoreColumn;
    @FXML private TableColumn<GradeRow, String> pointColumn;
    @FXML private TableColumn<GradeRow, String> statusColumn;

    @FXML
    private void initialize() {
        termSelector.setItems(FXCollections.observableArrayList("2025–2026 春季学期", "2025–2026 秋季学期", "全部学期"));
        termSelector.getSelectionModel().selectFirst();
        courseColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().course()));
        creditColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().credit()));
        scoreColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().score()));
        pointColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().point()));
        statusColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));
        refreshTerm();
    }

    @FXML
    private void refreshTerm() {
        boolean allTerms = "全部学期".equals(termSelector.getValue());
        weightedGpa.setText(allTerms ? "3.72" : "3.86");
        averageGpa.setText(allTerms ? "3.61" : "3.78");
        weightedScore.setText(allTerms ? "86.4" : "88.1");
        averageScore.setText(allTerms ? "84.9" : "87.3");
        gradeTable.setItems(FXCollections.observableArrayList(List.of(
                new GradeRow("软件工程", "3.0", "92", "4.0", "已通过"),
                new GradeRow("数据库系统", "3.0", "88", "3.7", "已通过"),
                new GradeRow("大学英语（四）", "2.0", "86", "3.3", "已通过"),
                new GradeRow("软件项目管理", "2.0", "90", "4.0", "已通过"),
                new GradeRow("体育（四）", "1.0", "85", "3.0", "已通过"))));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("加权均分");
        series.getData().addAll(new XYChart.Data<>("大一上", 82), new XYChart.Data<>("大一下", 84),
                new XYChart.Data<>("大二上", 86), new XYChart.Data<>("大二下", 88),
                new XYChart.Data<>("本学期", 88.1));
        trendChart.getData().setAll(series);
    }

    record GradeRow(String course, String credit, String score, String point, String status) {
    }
}
