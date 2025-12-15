package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class OnlineLobbyScene extends FXGLMenu {

    private TextField ipField;
    private TextField roomField;
    private Text statusText;

    public OnlineLobbyScene() {
        super(MenuType.GAME_MENU); // 借用 GAME_MENU 类型，或者自定义

        // 背景遮罩
        var bg = new Rectangle(getAppWidth(), getAppHeight(), Color.web("000", 0.8));

        // 标题
        var title = new Text("联机大厅");
        title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(60));
        title.setFill(Color.WHITE);

        // IP 输入
        Label lblIp = new Label("服务器 IP:");
        lblIp.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        ipField = new TextField("127.0.0.1");
        ipField.setMaxWidth(300);

        // 房间号输入
        Label lblRoom = new Label("房间号 (任意数字):");
        lblRoom.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");
        roomField = new TextField("1001");
        roomField.setMaxWidth(300);

        // 状态提示
        statusText = new Text("准备连接...");
        statusText.setFill(Color.YELLOW);
        statusText.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(16));

        // 按钮
        var btnConnect = new PixelatedButton("连接并加入", "Button1", this::connectToServer);
        var btnBack = new PixelatedButton("返 回", "Button1", () -> fireExitToMainMenu());

        // 布局
        VBox box = new VBox(20, title, lblIp, ipField, lblRoom, roomField, statusText, btnConnect, btnBack);
        box.setAlignment(Pos.CENTER);
        box.setTranslateX(getAppWidth() / 2.0 - 150);
        box.setTranslateY(100);

        getContentRoot().getChildren().addAll(bg, box);
    }

    private void connectToServer() {
        String ip = ipField.getText();
        String roomID = roomField.getText();

        if (roomID.isEmpty()) {
            statusText.setText("请输入房间号！");
            return;
        }

        statusText.setText("正在连接服务器...");

        // 调用 App/Controller 的方法开始连接
        // 这里我们需要把 IP 和 RoomID 传过去
        XiangQiApp app = (XiangQiApp) FXGL.getApp();
        app.startOnlineConnection(ip, roomID, statusText);
    }
}