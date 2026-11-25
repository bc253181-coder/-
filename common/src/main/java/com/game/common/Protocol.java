package com.game.common;

/**
 * 文本协议：每条消息占一行，第一个单词为命令，用空格分隔参数，UTF-8 编码。
 *
 * 客户端 -> 服务器
 *   JOIN <TTT|C4> <昵称>     进入匹配队列
 *   MOVE <n>                 落子（井字棋 n=格子 0..8，四子棋 n=列 0..6）
 *   CHAT <文字>              聊天
 *   REMATCH                  请求再来一局
 *   LEAVE                    离开当前对局，回到大厅
 *
 * 服务器 -> 客户端
 *   WELCOME                  连接成功
 *   WAITING                  正在等待对手
 *   START <TTT|C4> <X|O> <对手昵称>   对局开始，告知你的棋子
 *   STATE <棋盘> <轮到谁> <最后落子下标>  棋盘状态（每步后发送）
 *   OVER <X|O|DRAW> <获胜格子,逗号分隔 或 ->   对局结束
 *   CHAT <昵称> <文字>
 *   REMATCH_OFFER            对手请求再来一局
 *   OPPONENT_LEFT            对手离开/掉线
 *   ERROR <信息>
 */
public final class Protocol {
    private Protocol() {}

    public static final int DEFAULT_PORT = 5000;

    // client -> server
    public static final String JOIN = "JOIN";
    public static final String MOVE = "MOVE";
    public static final String CHAT = "CHAT";
    public static final String REMATCH = "REMATCH";
    public static final String LEAVE = "LEAVE";

    // server -> client
    public static final String WELCOME = "WELCOME";
    public static final String WAITING = "WAITING";
    public static final String START = "START";
    public static final String STATE = "STATE";
    public static final String OVER = "OVER";
    public static final String REMATCH_OFFER = "REMATCH_OFFER";
    public static final String OPPONENT_LEFT = "OPPONENT_LEFT";
    public static final String ERROR = "ERROR";
}
