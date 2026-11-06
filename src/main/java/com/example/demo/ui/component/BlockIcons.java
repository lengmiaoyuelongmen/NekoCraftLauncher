package com.example.demo.ui.component;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * 等距方块图标：按版本类型（正式版 / 预览版 / 旧版本）画不同配色的小方块。
 *
 * <p>从 HelloApplication 抽出的纯绘制工具，无状态，可被多个列表/对话框复用。
 */
public final class BlockIcons {

  private static final double SIZE = 40.0;

  private BlockIcons() {}

  public static StackPane create(String type) {
    Canvas canvas = new Canvas(SIZE, SIZE);
    GraphicsContext g = canvas.getGraphicsContext2D();
    boolean old = type != null && type.startsWith("old");
    boolean release = "release".equals(type);
    String top = release ? "#7CB342" : (old ? "#9E9E9E" : "#FF9800");
    String left = release ? "#795548" : (old ? "#757575" : "#E65100");
    String right = release ? "#5D4037" : (old ? "#616161" : "#BF360C");
    String edge = release ? "#558B2F" : (old ? "#757575" : "#F57C00");
    double[] topX = {20.0, 38.0, 20.0, 2.0};
    double[] topY = {2.0, 12.0, 22.0, 12.0};
    double[] leftX = {2.0, 20.0, 20.0, 2.0};
    double[] leftY = {12.0, 22.0, 40.0, 30.0};
    double[] rightX = {38.0, 20.0, 20.0, 38.0};
    double[] rightY = {12.0, 22.0, 40.0, 30.0};

    g.setFill(Color.web(top));
    g.fillPolygon(topX, topY, 4);
    g.setFill(Color.web(left));
    g.fillPolygon(leftX, leftY, 4);
    g.setFill(Color.web(right));
    g.fillPolygon(rightX, rightY, 4);

    if (release) {
      g.setFill(Color.web("#689F38"));
      g.fillPolygon(new double[] {2.0, 20.0, 20.0, 2.0}, new double[] {12.0, 22.0, 25.0, 15.0}, 4);
      g.fillPolygon(
          new double[] {38.0, 20.0, 20.0, 38.0}, new double[] {12.0, 22.0, 25.0, 15.0}, 4);
      g.setFill(Color.web("#8BC34A"));
      g.fillRect(12.0, 7.0, 6.0, 6.0);
      g.fillRect(26.0, 12.0, 5.0, 5.0);
      g.setFill(Color.web("#6D4C41"));
      g.fillRect(8.0, 27.0, 4.0, 4.0);
      g.fillRect(12.0, 33.0, 5.0, 4.0);
      g.fillRect(26.0, 29.0, 4.0, 5.0);
      g.setFill(Color.web("#8D6E63"));
      g.fillRect(30.0, 26.0, 4.0, 4.0);
    } else if (!old) {
      g.setFill(Color.web("#FFB74D"));
      g.fillRect(14.0, 8.0, 12.0, 7.0);
      g.setFill(Color.web("#FFCC80"));
      g.fillRect(17.0, 10.0, 6.0, 3.0);
      g.setFill(Color.web("#BF360C"));
      g.fillRect(14.0, 27.0, 5.0, 5.0);
      g.fillRect(25.0, 27.0, 5.0, 5.0);
      g.setFill(Color.web("#FFE0B2"));
      g.fillRect(16.0, 29.0, 2.0, 2.0);
      g.fillRect(27.0, 29.0, 2.0, 2.0);
    } else {
      g.setFill(Color.web("#BDBDBD"));
      g.fillRect(8.0, 18.0, 5.0, 4.0);
      g.fillRect(15.0, 24.0, 4.0, 4.0);
      g.fillRect(26.0, 18.0, 4.0, 4.0);
      g.setFill(Color.web("#424242"));
      g.fillRect(11.0, 27.0, 4.0, 4.0);
      g.fillRect(28.0, 25.0, 4.0, 4.0);
    }

    g.setStroke(Color.web(edge));
    g.setLineWidth(1.0);
    g.strokePolygon(topX, topY, 4);
    g.strokeLine(20.0, 22.0, 20.0, 40.0);

    StackPane block = new StackPane(canvas);
    block.setPrefSize(SIZE, SIZE);
    block.setMinSize(SIZE, SIZE);
    block.setMaxSize(SIZE, SIZE);
    block.setMouseTransparent(true);
    return block;
  }
}
