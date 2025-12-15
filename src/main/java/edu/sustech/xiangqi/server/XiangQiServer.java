package edu.sustech.xiangqi.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class XiangQiServer {
    private static final int PORT = 9999;

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

        //发送信息
        public void send(String msg) { if (out != null) out.println(msg); }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                //读取第一条消息：必须是 "JOIN <RoomID>"
                String request = in.readLine();
                if (request != null && request.startsWith("JOIN")) {
                    roomId = request.split(" ")[1];
                    System.out.println("玩家加入房间: " + roomId);

                    handleMatching(roomId);
                }

                //游戏中
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (opponent != null) {
                        opponent.send(msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("连接断开");
            } finally {
                //如果断开了，且还在等待列表中，要移除
                if (roomId != null && waitingPlayers.get(roomId) == this) {
                    waitingPlayers.remove(roomId);
                }
                try { socket.close(); } catch (Exception e) {}
            }
        }

        // 处理配对逻辑
        private synchronized void handleMatching(String roomId) {
            if (waitingPlayers.containsKey(roomId)) {
                ClientHandler p1 = waitingPlayers.get(roomId);

                //绑定
                this.opponent = p1;
                p1.opponent = this;

                // 列表移除（房间满了）
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
