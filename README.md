# 在线多人井字棋 / 四子棋（JavaFX + Socket）

客户端-服务器架构的联网对战游戏，支持 **井字棋 (3×3，连 3)** 和 **四子棋 (6×7，连 4，带掉落动画)**。

```
common/   共享代码：游戏规则（Game / TicTacToe / ConnectFour）、协议常量
server/   服务器（纯 Java，不依赖 JavaFX，部署在你的服务器上）
client/   JavaFX 客户端（在玩家电脑上运行）
deploy/   systemd 服务文件
```

功能：自动匹配、服务器判定胜负（防作弊）、获胜连线高亮、聊天、再来一局（交换先后手）、换对手、断线通知。

## 环境要求
- JDK 17+（推荐 21）
- Maven 3.8+
- 客户端不需要单独安装 JavaFX，Maven 会自动下载

## 一、本地测试（一台电脑）

```bash
mvn install                                # 编译全部模块
java -jar server/target/game-server.jar    # 终端 1：启动服务器（默认端口 5000）
cd client && mvn javafx:run                # 终端 2：客户端 A
cd client && mvn javafx:run                # 终端 3：客户端 B
```
两个客户端都填 `127.0.0.1`、选同一种游戏，就会自动配对。

快速测试可以带参数自动连接：
```bash
mvn javafx:run -Djavafx.args="--host=127.0.0.1 --name=Alice --game=C4 --auto=true"
```

## 二、部署到你的服务器

1. 在本地打包：`mvn -pl server -am package`，得到 `server/target/game-server.jar`
2. 上传到服务器：
   ```bash
   scp server/target/game-server.jar 用户名@服务器IP:/opt/game-server/
   ```
3. 服务器上装 Java：`sudo apt install openjdk-21-jre-headless`（Ubuntu/Debian）
4. 运行：
   - 临时运行：`java -Dstdout.encoding=UTF-8 -jar /opt/game-server/game-server.jar 5000`
   - 长期运行（推荐）：使用 `deploy/game-server.service`
     ```bash
     sudo cp deploy/game-server.service /etc/systemd/system/
     sudo systemctl daemon-reload
     sudo systemctl enable --now game-server
     journalctl -u game-server -f     # 查看日志
     ```
5. **开放端口 5000（TCP）**，否则外部连不上：
   - 服务器防火墙：`sudo ufw allow 5000/tcp`
   - 如果是云服务器（阿里云 / 腾讯云 / AWS 等），还要在控制台的**安全组**里放行 TCP 5000
6. 玩家在客户端的“服务器地址”里填你服务器的**公网 IP** 即可对战

## 通信协议

每条消息一行文本（UTF-8），详见 `common/src/main/java/com/game/common/Protocol.java`：

| 方向 | 消息 | 含义 |
|---|---|---|
| C→S | `JOIN <TTT\|C4> <昵称>` | 进入匹配 |
| C→S | `MOVE <n>` | 落子（井字棋为格子 0–8，四子棋为列 0–6） |
| C→S | `CHAT <文字>` / `REMATCH` / `LEAVE` | 聊天 / 再来一局 / 离开 |
| S→C | `WAITING` | 等待对手 |
| S→C | `START <类型> <X\|O> <对手>` | 开局 |
| S→C | `STATE <棋盘> <轮到谁> <最后落子>` | 棋盘状态 |
| S→C | `OVER <X\|O\|DRAW> <获胜格子>` | 结束 |
| S→C | `CHAT` / `REMATCH_OFFER` / `OPPONENT_LEFT` / `ERROR` | 其他通知 |

## 关键设计说明（报告里可以写）
- **线程**：服务器每个连接一个线程（线程池）；客户端用后台线程读 Socket，通过 `Platform.runLater` 更新界面，避免界面卡死。
- **权威服务器**：客户端只发送“想下哪里”，合法性与胜负都由服务器判断，再广播给双方，保证两边状态一致。
- **同步**：`GameRoom`、`Matchmaker` 的方法使用 `synchronized`，防止两个玩家同时操作造成竞态。
- **通用胜负判断**：`Game` 以最后一步为中心沿 4 个方向数连子数，井字棋和四子棋共用同一套逻辑，只是 `winLength` 不同。
