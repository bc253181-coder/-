package com.game.client;

import com.game.common.GameType;
import com.game.common.Protocol;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** JavaFX 客户端：连接界面 -> 匹配等待 -> 对局界面。 */
public class ClientApp extends Application {
    private Stage stage;
    private Connection conn;

    private String myName;
    private GameType gameType;
    private char myMark;
    private boolean gameOver;

    // 对局界面控件
    private BoardView board;
    private Label status;
    private TextArea chatLog;
    private Button rematchBtn;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("在线对战 - 井字棋 / 四子棋");
        stage.setOnCloseRequest(e -> { if (conn != null) conn.close(); });
        showConnectScreen(null);
        stage.show();
    }

    // ---------------- 连接界面 ----------------

    private void showConnectScreen(String error) {
        Label title = new Label("在线对战");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));

        var params = getParameters().getNamed();
        TextField host = new TextField(params.getOrDefault("host", "127.0.0.1"));
        TextField port = new TextField(params.getOrDefault("port", String.valueOf(Protocol.DEFAULT_PORT)));
        TextField name = new TextField(myName != null ? myName : "玩家" + (int) (Math.random() * 1000));
        ComboBox<GameType> game = new ComboBox<>();
        game.getItems().addAll(GameType.values());
        game.setValue(gameType != null ? gameType : GameType.TTT);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("服务器地址"), host);
        form.addRow(1, new Label("端口"), port);
        form.addRow(2, new Label("昵称"), name);
        form.addRow(3, new Label("游戏"), game);

        Label err = new Label(error == null ? "" : error);
        err.setStyle("-fx-text-fill: #e53935;");
        Button connect = new Button("连接并匹配");
        connect.setDefaultButton(true);
        connect.setOnAction(e -> {
            int p;
            try {
                p = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException ex) {
                err.setText("端口必须是数字");
                return;
            }
            myName = name.getText().trim().isEmpty() ? "玩家" : name.getText().trim();
            gameType = game.getValue();
            connect.setDisable(true);
            err.setText("正在连接...");
            doConnect(host.getText().trim(), p, err, connect);
        });

        VBox root = new VBox(16, title, form, connect, err);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        form.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(root, 420, 360));

        // 支持命令行参数 --name=xx --game=C4 --auto=true，方便一键启动测试
        if (error == null && myName == null) {
            if (params.containsKey("name")) name.setText(params.get("name"));
            if (params.containsKey("game")) game.setValue(GameType.valueOf(params.get("game").toUpperCase()));
            if ("true".equals(params.get("auto"))) Platform.runLater(connect::fire);
        }
    }

    /** 在后台线程连接，避免卡住界面。 */
    private void doConnect(String host, int port, Label err, Button btn) {
        Thread t = new Thread(() -> {
            Connection c = new Connection();
            try {
                c.connect(host, port, this::onMessage, this::onDisconnect);
                Platform.runLater(() -> {
                    conn = c;
                    conn.send(Protocol.JOIN + " " + gameType.name() + " " + myName);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    err.setText("连接失败: " + ex.getMessage());
                    btn.setDisable(false);
                });
            }
        }, "connect");
        t.setDaemon(true);
        t.start();
    }

    private void onDisconnect() {
        conn = null;
        showConnectScreen("与服务器的连接已断开");
    }

    // ---------------- 等待界面 ----------------

    private void showWaitingScreen() {
        ProgressIndicator spinner = new ProgressIndicator();
        Label l = new Label("正在等待对手加入 " + gameType.displayName + " ...");
        Button cancel = new Button("取消");
        cancel.setOnAction(e -> {
            if (conn != null) conn.close();
            conn = null;
            showConnectScreen(null);
        });
        VBox root = new VBox(20, spinner, l, cancel);
        root.setAlignment(Pos.CENTER);
        stage.setScene(new Scene(root, 420, 360));
    }

    // ---------------- 对局界面 ----------------

    private void showGameScreen(String opponent) {
        board = new BoardView(gameType);
        board.setOnMove(m -> {
            if (!gameOver && conn != null) conn.send(Protocol.MOVE + " " + m);
        });

        Label info = new Label("你 (" + myName + ") 执 " + markName(myMark) + "   vs   " + opponent);
        info.setFont(Font.font(14));
        status = new Label();
        status.setFont(Font.font("System", FontWeight.BOLD, 18));

        chatLog = new TextArea();
        chatLog.setEditable(false);
        chatLog.setWrapText(true);
        chatLog.setPrefWidth(220);
        VBox.setVgrow(chatLog, Priority.ALWAYS);
        TextField chatInput = new TextField();
        chatInput.setPromptText("输入消息，回车发送");
        chatInput.setOnAction(e -> {
            String text = chatInput.getText().trim();
            if (!text.isEmpty() && conn != null) conn.send(Protocol.CHAT + " " + text);
            chatInput.clear();
        });
        VBox chat = new VBox(6, new Label("聊天"), chatLog, chatInput);

        rematchBtn = new Button("再来一局");
        rematchBtn.setDisable(true);
        rematchBtn.setOnAction(e -> {
            conn.send(Protocol.REMATCH);
            rematchBtn.setDisable(true);
            status.setText("等待对手同意再来一局...");
        });
        Button newOpp = new Button("换个对手");
        newOpp.setOnAction(e -> conn.send(Protocol.JOIN + " " + gameType.name() + " " + myName));
        Button quit = new Button("退出到主界面");
        quit.setOnAction(e -> {
            conn.close();
            conn = null;
            showConnectScreen(null);
        });
        HBox buttons = new HBox(10, rematchBtn, newOpp, quit);
        buttons.setAlignment(Pos.CENTER);

        VBox left = new VBox(12, info, status, board, buttons);
        left.setAlignment(Pos.CENTER);
        HBox root = new HBox(16, left, chat);
        root.setPadding(new Insets(16));
        stage.setScene(new Scene(root));
        stage.sizeToScene();
    }

    private void appendChat(String line) {
        if (chatLog != null) chatLog.appendText(line + "\n");
    }

    private static String markName(char mark) {
        return mark == 'X' ? "X（红）" : "O（黄）";
    }

    // ---------------- 消息处理（都在 FX 线程中执行） ----------------

    private void onMessage(String line) {
        String[] p = line.split(" ", 2);
        String cmd = p[0];
        String arg = p.length > 1 ? p[1] : "";

        switch (cmd) {
            case Protocol.WELCOME -> { }
            case Protocol.WAITING -> showWaitingScreen();
            case Protocol.START -> {
                // START <type> <mark> <opponent>
                String[] a = arg.split(" ", 3);
                gameType = GameType.valueOf(a[0]);
                myMark = a[1].charAt(0);
                gameOver = false;
                showGameScreen(a.length > 2 ? a[2] : "对手");
                appendChat("[系统] 对局开始！X 先手。");
            }
            case Protocol.STATE -> {
                // STATE <board> <turn> <last>
                String[] a = arg.split(" ");
                board.update(a[0], Integer.parseInt(a[2]));
                if (!gameOver) {
                    boolean mine = a[1].charAt(0) == myMark;
                    status.setText(mine ? "轮到你了" : "等待对手落子...");
                    status.setStyle(mine ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #616161;");
                }
            }
            case Protocol.OVER -> {
                // OVER <X|O|DRAW> <cells>
                String[] a = arg.split(" ");
                gameOver = true;
                if (a[0].equals("DRAW")) {
                    status.setText("平局！");
                    status.setStyle("-fx-text-fill: #616161;");
                } else {
                    boolean win = a[0].charAt(0) == myMark;
                    status.setText(win ? "你赢了！🎉" : "你输了");
                    status.setStyle(win ? "-fx-text-fill: #2e7d32;" : "-fx-text-fill: #e53935;");
                }
                if (a.length > 1 && !a[1].equals("-")) {
                    List<Integer> cells = Arrays.stream(a[1].split(",")).map(Integer::parseInt).collect(Collectors.toList());
                    board.highlight(cells);
                }
                rematchBtn.setDisable(false);
            }
            case Protocol.CHAT -> {
                String[] a = arg.split(" ", 2);
                appendChat(a[0] + ": " + (a.length > 1 ? a[1] : ""));
            }
            case Protocol.REMATCH_OFFER -> appendChat("[系统] 对手想再来一局，点击“再来一局”接受。");
            case Protocol.OPPONENT_LEFT -> {
                gameOver = true;
                if (status != null) {
                    status.setText("对手已离开");
                    status.setStyle("-fx-text-fill: #e53935;");
                }
                if (rematchBtn != null) rematchBtn.setDisable(true);
                appendChat("[系统] 对手离开了，点击“换个对手”重新匹配。");
            }
            case Protocol.ERROR -> {
                if (chatLog != null) appendChat("[提示] " + arg);
            }
            default -> { }
        }
    }
}
