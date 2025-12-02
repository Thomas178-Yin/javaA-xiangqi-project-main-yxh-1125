package edu.sustech.xiangqi.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isRunning = false;

    // 回调接口：当收到服务器消息时执行这里的代码
    private Consumer<String> onMessageReceived;

    /**
     * 设置消息接收的回调函数
     * 在 Controller 里调用: client.setOnMessage(this::onNetworkMessage);
     */
    public void setOnMessage(Consumer<String> callback) {
        this.onMessageReceived = callback;
    }

    /**
     * 连接服务器并加入房间
     * @param ip 服务器IP (本机测试用 127.0.0.1)
     * @param port 端口 (我们设定的是 9999)
     * @param roomId 房间号 (例如 "1001")
     * @throws IOException 连接失败时抛出，由 UI 层捕获并提示
     */
    public void connect(String ip, int port, String roomId) throws IOException {
        // 1. 建立 Socket 连接
        this.socket = new Socket(ip, port);

        // 2. 初始化输入输出流 (autoFlush = true 非常重要)
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.isRunning = true;

        // 3. 【关键协议】连接建立后的第一件事：发送加入房间指令
        // 服务器端会读取这一行来决定把你分配到哪一桌
        out.println("JOIN " + roomId);

        // 4. 开启后台线程，专门负责“听”服务器说话
        new Thread(this::listen).start();
    }

    /**
     * 监听循环 (运行在后台线程)
     */
    private void listen() {
        try {
            String msg;
            // 阻塞式读取，直到断开连接
            while (isRunning && (msg = in.readLine()) != null) {
                // 如果设置了回调，就触发回调
                if (onMessageReceived != null) {
                    onMessageReceived.accept(msg);
                }
            }
        } catch (IOException e) {
            System.out.println("网络连接已断开: " + e.getMessage());
        } finally {
            close();
        }
    }

    /**
     * 发送移动指令
     * 协议格式: "MOVE r1 c1 r2 c2"
     */
    public void sendMove(int r1, int c1, int r2, int c2) {
        if (out != null) {
            out.println("MOVE " + r1 + " " + c1 + " " + r2 + " " + c2);
        }
    }

    /**
     * 发送任意指令 (例如 "SURRENDER", "UNDO_REQUEST")
     */
    public void sendRaw(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    /**
     * 断开连接并释放资源
     */
    public void close() {
        isRunning = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}