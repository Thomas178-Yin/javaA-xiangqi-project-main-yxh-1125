package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.core.math.FXGLMath;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.manager.UserManager;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainMenuScene extends FXGLMenu {

    private VBox contentBox;
    private VBox loginSelectionView;
    private VBox userLoginFormView;
    private VBox mainMenuView;
    private VBox onlineLobbyView;

    // 表单控件
    private TextField inputUser;
    private PasswordField inputPass;
    private PasswordField inputConfirmPass;
    private VBox confirmBox;
    private Label statusLabel;
    private boolean isRegistering = false;

    // --- 视觉粒子 ---
    private final List<PixelDust> particles = new ArrayList<>();
    private double timeAccumulator = 0;

    // --- 视觉常量 ---
    private static final Color BG_DARK = Color.web("#181010");
    private static final Color BG_LIGHT = Color.web("#2a1e15");
    private static final Color GRID_COLOR = Color.web("#ffffff", 0.03); // 极淡的网格线
    private static final Color TEXT_COLOR = Color.web("#f0e6d2");
    private static final Color ERROR_COLOR = Color.web("#ff5555");

    // 输入框样式
    private static final String INPUT_STYLE =
            "-fx-background-color: rgba(0,0,0,0.3); -fx-border-color: #665544; -fx-border-width: 0 0 2 0; -fx-text-fill: #f0e6d2; -fx-font-size: 16px; -fx-font-family: 'Monospaced';";
    private static final String INPUT_FOCUS_STYLE =
            "-fx-background-color: rgba(0,0,0,0.5); -fx-border-color: #d4a173; -fx-border-width: 0 0 2 0; -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-family: 'Monospaced';";

    public MainMenuScene() {
        super(MenuType.MAIN_MENU);

        // 1. 背景：
        Rectangle bg = new Rectangle(getAppWidth(), getAppHeight());
        bg.setFill(new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, BG_LIGHT), new Stop(1, BG_DARK)));
        getContentRoot().getChildren().add(bg);

        // 2. 装饰：绘制像素网格
        Canvas gridCanvas = createGridCanvas();
        getContentRoot().getChildren().add(gridCanvas);

        // 3. 标题：
        Text title = new Text("象  棋");
        try { title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(110)); } catch (Exception e) {}
        title.setFill(Color.web("#eec39a"));
        title.setStroke(Color.web("#5a3e2b"));
        title.setStrokeWidth(2);
        title.setEffect(new DropShadow(20, Color.BLACK));

        Text subTitle = new Text("PIXEL CHESS 2025 FOR CS109");
        try { subTitle.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(20)); } catch (Exception e) {}
        subTitle.setFill(Color.web("#886655"));

        VBox titleBox = new VBox(5, title, subTitle);
        titleBox.setAlignment(Pos.CENTER);

        // 4. 内容容器
        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);

        // 初始化视图
        initLoginSelectionView();
        initUserLoginFormView();
        initMainMenuView();
        initOnlineLobbyView();

        // 布局
        VBox layout = new VBox(40, titleBox, contentBox);
        layout.setAlignment(Pos.CENTER);
        layout.setTranslateX(getAppWidth() / 2.0 - 150); // 居中修正
        layout.setTranslateY(getAppHeight() / 2.0 - 280);

        getContentRoot().getChildren().add(layout);

        // 默认显示
        switchView(loginSelectionView);
        animateIn(layout);
    }

    // --- 粒子更新 ---
    @Override
    protected void onUpdate(double tpf) {
        super.onUpdate(tpf);

        // 生成粒子：每 0.05 秒生成一个漂浮的像素点
        timeAccumulator += tpf;
        if (timeAccumulator > 0.05) {
            timeAccumulator = 0;
            spawnParticle();
        }

        // 更新粒子位置
        Iterator<PixelDust> it = particles.iterator();
        while (it.hasNext()) {
            PixelDust p = it.next();
            p.update(tpf);
            if (p.isDead()) {
                getContentRoot().getChildren().remove(p.view);
                it.remove();
            }
        }
    }

    // --- 视觉工具方法 ---

    // 绘制一个淡入淡出的网格背景
    private Canvas createGridCanvas() {
        Canvas canvas = new Canvas(getAppWidth(), getAppHeight());
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(1);

        int step = 40; // 网格大小
        // 竖线
        for (int x = 0; x < getAppWidth(); x += step) {
            gc.strokeLine(x, 0, x, getAppHeight());
        }
        // 横线
        for (int y = 0; y < getAppHeight(); y += step) {
            gc.strokeLine(0, y, getAppWidth(), y);
        }
        return canvas;
    }

    private void spawnParticle() {
        // 在底部随机生成
        double x = FXGLMath.random(0, getAppWidth());
        double y = getAppHeight() + 10;
        double size = FXGLMath.random(2, 5); // 像素块大小
        Rectangle view = new Rectangle(size, size, Color.rgb(255, 200, 150, 1)); // 半透明金色

        PixelDust dust = new PixelDust(view, x, y);
        particles.add(dust);
        getContentRoot().getChildren().add(1, view); // 插在背景图和网格之间
    }

    // 简单的内部类管理粒子
    private static class PixelDust {
        Node view;
        double x, y;
        double speed;
        double alpha = 0;

        PixelDust(Node view, double x, double y) {
            this.view = view;
            this.x = x;
            this.y = y;
            this.speed = FXGLMath.random(30, 70);
            view.setTranslateX(x);
            view.setTranslateY(y);
        }

        void update(double tpf) {
            y -= speed * tpf;
            view.setTranslateY(y);
            // 简单的淡入淡出逻辑
            if (y > 600) alpha += tpf;
            else alpha -= tpf * 0.5;
            view.setOpacity(Math.max(0, Math.min(0.4, alpha)));
        }

        boolean isDead() { return y < -20 || alpha <= 0; }
    }

    private void setupInputStyle(TextField field) {
        field.setStyle(INPUT_STYLE);
        field.focusedProperty().addListener((obs, oldVal, newVal) ->
                field.setStyle(newVal ? INPUT_FOCUS_STYLE : INPUT_STYLE));
    }

    // --- 核心切换逻辑 ---

    private void switchView(VBox targetView) {
        contentBox.getChildren().clear();
        if (targetView != null) {
            contentBox.getChildren().add(targetView);
            animateIn(targetView); // 保持高级感动画
        }
    }

    private void animateIn(Node node) {
        node.setOpacity(0);
        node.setTranslateY(30);

        FadeTransition ft = new FadeTransition(Duration.seconds(0.6), node);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.seconds(0.6), node);
        tt.setToY(0);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.play();
    }

    // --- 界面初始化

    private void initLoginSelectionView() {
        var btnUser = new PixelatedButton("用户登录", "Button1", () -> {
            resetLoginForm();
            switchView(userLoginFormView);
        });
        var btnGuest = new PixelatedButton("游客试玩", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.loginAsGuest();
            switchView(mainMenuView);
        });
        var btnExit = new PixelatedButton("退 出", "Button1", this::fireExit);

        loginSelectionView = new VBox(15, btnUser, btnGuest, btnExit);
        loginSelectionView.setAlignment(Pos.CENTER);
    }

    private void initUserLoginFormView() {
        inputUser = new TextField();
        inputUser.setPromptText("用户名");
        inputUser.setMaxWidth(220);
        setupInputStyle(inputUser);

        inputPass = new PasswordField();
        inputPass.setPromptText("密码");
        inputPass.setMaxWidth(220);
        setupInputStyle(inputPass);

        inputConfirmPass = new PasswordField();
        inputConfirmPass.setPromptText("确认密码");
        inputConfirmPass.setMaxWidth(220);
        setupInputStyle(inputConfirmPass);

        confirmBox = new VBox(10, inputConfirmPass);
        confirmBox.setAlignment(Pos.CENTER);
        confirmBox.setVisible(false);
        confirmBox.setManaged(false);

        statusLabel = new Label(" ");
        statusLabel.setTextFill(ERROR_COLOR);
        try { statusLabel.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(14)); } catch(Exception e){}

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        // 这里的按钮文字要短一点，不然两个像素按钮并排可能太宽
        var btnConfirm = new PixelatedButton("确定", "Button1", this::handleUserLogin);
        // 手动缩小一点 PixelatedButton 的尺寸 (假设你有缩放逻辑，或者接受默认大小)
        btnConfirm.setScaleX(0.9); btnConfirm.setScaleY(0.9);

        var btnBack = new PixelatedButton("返回", "Button1", () -> switchView(loginSelectionView));
        btnBack.setScaleX(0.9); btnBack.setScaleY(0.9);

        btnBox.getChildren().addAll(btnConfirm, btnBack);

        userLoginFormView = new VBox(20, inputUser, inputPass, confirmBox, statusLabel, btnBox);
        userLoginFormView.setAlignment(Pos.CENTER);
    }

    private void initMainMenuView() {
        var btnNew = new PixelatedButton("标准对战", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(false);
            app.setLoadedGame(false);
            app.setOnlineLaunch(false);
            fireNewGame();
        });

        var btnCustom = new PixelatedButton("排局模式", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(true);
            app.setLoadedGame(false);
            app.setOnlineLaunch(false);
            fireNewGame();
        });

        var btnLoad = new PixelatedButton("读取存档", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setOnlineLaunch(false);
            app.openLoadDialog();
        });

        var btnNet = new PixelatedButton("联网对战", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            if (app.isGuest()) {
                FXGL.getDialogService().showMessageBox("游客模式无法进行联网对战。");
                return;
            }
            switchView(onlineLobbyView);
        });

        var btnLogout = new PixelatedButton("注销登录", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setOnlineLaunch(false);
            app.loginAsGuest();
            switchView(loginSelectionView);
        });

        mainMenuView = new VBox(12, btnNew, btnCustom, btnLoad, btnNet, btnLogout);
        mainMenuView.setAlignment(Pos.CENTER);
    }

    private void initOnlineLobbyView() {
        Label title = new Label("联 机 大 厅");
        title.setTextFill(TEXT_COLOR);
        try { title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(28)); } catch(Exception e){}

        TextField ipField = new TextField("127.0.0.1");
        ipField.setPromptText("服务器 IP");
        ipField.setMaxWidth(220);
        setupInputStyle(ipField);

        TextField roomField = new TextField("1001");
        roomField.setPromptText("房间号");
        roomField.setMaxWidth(220);
        setupInputStyle(roomField);

        Text statusText = new Text("等待连接...");
        statusText.setFill(Color.web("#aaaaaa"));
        try { statusText.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(14)); } catch(Exception e){}

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);

        var btnConnect = new PixelatedButton("加入", "Button1", () -> {
            String ip = ipField.getText();
            String room = roomField.getText();
            if (room.isEmpty()) {
                statusText.setText("请输入房间号");
                statusText.setFill(ERROR_COLOR);
                return;
            }
            statusText.setText("正在连接...");
            statusText.setFill(Color.YELLOW);
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.startOnlineConnection(ip, room, statusText);
        });
        btnConnect.setScaleX(0.9); btnConnect.setScaleY(0.9);

        var btnBack = new PixelatedButton("返回", "Button1", () -> switchView(mainMenuView));
        btnBack.setScaleX(0.9); btnBack.setScaleY(0.9);

        btnBox.getChildren().addAll(btnConnect, btnBack);

        onlineLobbyView = new VBox(20, title, ipField, roomField, statusText, btnBox);
        onlineLobbyView.setAlignment(Pos.CENTER);
    }

    // 逻辑部分保持原样
    private void resetLoginForm() {
        inputUser.clear(); inputPass.clear(); inputConfirmPass.clear();
        statusLabel.setText(" "); isRegistering = false;
        if (confirmBox != null) { confirmBox.setVisible(false); confirmBox.setManaged(false); }
    }

    private void handleUserLogin() {
        String user = inputUser.getText().trim();
        String pass = inputPass.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("请输入完整信息"); statusLabel.setTextFill(ERROR_COLOR); return;
        }
        XiangQiApp app = (XiangQiApp) FXGL.getApp();
        UserManager userManager = app.getUserManager();

        if (isRegistering) {
            if (pass.equals(inputConfirmPass.getText())) {
                userManager.registerUser(user, pass);
                app.login(user);
                performDelayedSwitch();
            } else {
                statusLabel.setText("密码不一致"); statusLabel.setTextFill(ERROR_COLOR);
            }
        } else {
            if (userManager.userExists(user)) {
                if (userManager.verifyPassword(user, pass)) {
                    app.login(user);
                    performDelayedSwitch();
                } else {
                    statusLabel.setText("密码错误"); statusLabel.setTextFill(ERROR_COLOR);
                }
            } else {
                isRegistering = true;
                statusLabel.setText("新用户？请确认密码"); statusLabel.setTextFill(Color.ORANGE);
                confirmBox.setVisible(true); confirmBox.setManaged(true);
            }
        }
    }

    private void performDelayedSwitch() {
        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(e -> switchView(mainMenuView));
        delay.play();
    }
}