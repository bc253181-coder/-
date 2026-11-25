package com.game.client;

import com.game.common.Game;
import com.game.common.GameType;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntConsumer;

/** 棋盘绘制和点击处理。井字棋画 X/O，四子棋画彩色圆片并带掉落动画。 */
public class BoardView extends Pane {
    private static final Color X_COLOR = Color.web("#e53935");
    private static final Color O_COLOR = Color.web("#fbc02d");

    private final GameType type;
    private final double cell;
    private final Node[] pieces;
    private char[] shown;
    private final Rectangle hover;
    private IntConsumer onMove = m -> {};

    public BoardView(GameType type) {
        this.type = type;
        this.cell = type == GameType.TTT ? 110 : 72;
        this.pieces = new Node[type.rows * type.cols];
        this.shown = new char[pieces.length];
        Arrays.fill(shown, Game.EMPTY);

        double w = cell * type.cols, h = cell * type.rows;
        setPrefSize(w, h);
        setMinSize(w, h);
        setMaxSize(w, h);
        setClip(new Rectangle(w, h)); // 掉落动画时不画到棋盘外

        hover = new Rectangle(cell, type == GameType.C4 ? h : cell, Color.color(1, 1, 1, 0.15));
        hover.setVisible(false);
        hover.setMouseTransparent(true);

        if (type == GameType.TTT) {
            Rectangle bg = new Rectangle(w, h, Color.web("#263238"));
            getChildren().addAll(bg, hover);
            for (int i = 1; i < 3; i++) {
                getChildren().add(gridLine(i * cell, 8, i * cell, h - 8));
                getChildren().add(gridLine(8, i * cell, w - 8, i * cell));
            }
        } else {
            Rectangle bg = new Rectangle(w, h, Color.web("#1565c0"));
            bg.setArcWidth(20);
            bg.setArcHeight(20);
            getChildren().addAll(bg, hover);
            for (int r = 0; r < type.rows; r++)
                for (int c = 0; c < type.cols; c++)
                    getChildren().add(new Circle(c * cell + cell / 2, r * cell + cell / 2, cell * 0.4, Color.web("#0d2a4d")));
        }

        setOnMouseMoved(e -> {
            int c = (int) (e.getX() / cell), r = (int) (e.getY() / cell);
            if (c < 0 || c >= type.cols || r < 0 || r >= type.rows) { hover.setVisible(false); return; }
            hover.setX(c * cell);
            hover.setY(type == GameType.C4 ? 0 : r * cell);
            hover.setVisible(true);
        });
        setOnMouseExited(e -> hover.setVisible(false));
        setOnMouseClicked(e -> {
            int c = (int) (e.getX() / cell), r = (int) (e.getY() / cell);
            if (c < 0 || c >= type.cols || r < 0 || r >= type.rows) return;
            onMove.accept(type == GameType.TTT ? r * type.cols + c : c);
        });
    }

    private Line gridLine(double x1, double y1, double x2, double y2) {
        Line l = new Line(x1, y1, x2, y2);
        l.setStroke(Color.web("#90a4ae"));
        l.setStrokeWidth(4);
        return l;
    }

    public void setOnMove(IntConsumer onMove) { this.onMove = onMove; }

    /** 根据服务器发来的棋盘字符串更新显示。 */
    public void update(String board, int lastIndex) {
        for (int i = 0; i < board.length() && i < pieces.length; i++) {
            char ch = board.charAt(i);
            if (ch == shown[i]) continue;
            if (pieces[i] != null) getChildren().remove(pieces[i]);
            pieces[i] = null;
            if (ch != Game.EMPTY) {
                Node p = makePiece(i, ch);
                pieces[i] = p;
                getChildren().add(p);
                if (type == GameType.C4 && i == lastIndex) animateDrop(p, i);
            }
        }
        shown = board.toCharArray();
    }

    public void highlight(List<Integer> cells) {
        for (int i : cells) {
            if (i >= 0 && i < pieces.length && pieces[i] != null) {
                DropShadow glow = new DropShadow(25, Color.WHITE);
                glow.setSpread(0.6);
                pieces[i].setEffect(glow);
            }
        }
    }

    private Node makePiece(int idx, char ch) {
        int r = idx / type.cols, c = idx % type.cols;
        double cx = c * cell + cell / 2, cy = r * cell + cell / 2;
        if (type == GameType.TTT) {
            Text t = new Text(String.valueOf(ch));
            t.setFont(Font.font("System", FontWeight.BOLD, cell * 0.7));
            t.setFill(ch == Game.X ? X_COLOR : O_COLOR);
            t.setX(cx - t.getLayoutBounds().getWidth() / 2);
            t.setY(cy + t.getLayoutBounds().getHeight() / 3.2);
            t.setMouseTransparent(true);
            return t;
        }
        Circle circle = new Circle(cx, cy, cell * 0.4, ch == Game.X ? X_COLOR : O_COLOR);
        circle.setMouseTransparent(true);
        return circle;
    }

    private void animateDrop(Node piece, int idx) {
        int r = idx / type.cols;
        TranslateTransition tt = new TranslateTransition(Duration.millis(120 + r * 60), piece);
        tt.setFromY(-(r + 1) * cell);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_IN);
        tt.play();
    }
}
