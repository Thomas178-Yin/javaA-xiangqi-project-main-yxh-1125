package edu.sustech.xiangqi.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class XiangQiServer {
    private static final int PORT = 9999;

    // 【核心】房间映射表：房间号 -> 等待中的玩家
    // 如果房间里还没人，这就存第一个人。
    // 如果房间里有人了，第二个人来了就配对，然后从表里移除。
    private static ConcurrentHashMap<String, ClientHandler> waitingPlayers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("高级象棋服务器启动 (支持房间)...");

        while (true) {
            Socket socket = server.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {
        Socket socket;
        ClientHandler opponent;
        PrintWriter out;
        BufferedReader in;
        String roomId;

        public ClientHandler(Socket socket) { this.socket = socket; }

        public void send(String msg) { if (out != null) out.println(msg); }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 1. 读取第一条消息：必须是 "JOIN <RoomID>"
                String request = in.readLine();
                if (request != null && request.startsWith("JOIN")) {
                    roomId = request.split(" ")[1];
                    System.out.println("玩家加入房间: " + roomId);

                    handleMatching(roomId);
                }

                // 2. 游戏循环：转发消息
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (opponent != null) {
                        opponent.send(msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("连接断开");
            } finally {
                // 清理逻辑：如果断开了，且还在等待列表中，要移除
                if (roomId != null && waitingPlayers.get(roomId) == this) {
                    waitingPlayers.remove(roomId);
                }
                try { socket.close(); } catch (Exception e) {}
            }
        }

        // 处理配对逻辑 (线程安全需要注意，这里简化处理)
        private synchronized void handleMatching(String roomId) {
            if (waitingPlayers.containsKey(roomId)) {
                // 房间里已经有 P1 了，我是 P2
                ClientHandler p1 = waitingPlayers.get(roomId);

                // 互相绑定
                this.opponent = p1;
                p1.opponent = this;

                // 从等待列表移除（房间满了）
                waitingPlayers.remove(roomId);

                // 发送开始指令
                p1.send("START RED");   // 等待的人先手
                this.send("START BLACK"); // 后进的人后手
                System.out.println("房间 " + roomId + " 配对成功！");

            } else {
                // 房间是空的，我是 P1
                waitingPlayers.put(roomId, this);
                this.send("WAIT");
                System.out.println("房间 " + roomId + " 等待对手...");
            }
        }
    }
}
