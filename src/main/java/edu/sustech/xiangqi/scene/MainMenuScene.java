package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.manager.UserManager;
import javafx.animation.FillTransition;
import javafx.animation.PauseTransition; // 【新增】导入原生动画类
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.List;

public class MainMenuScene extends FXGLMenu {

    private VBox contentBox; // 主内容容器
    private VBox loginSelectionView;
    private VBox userLoginFormView;
    private VBox mainMenuView;

    // 登录表单控件
    private TextField inputUser;
    private PasswordField inputPass;
    private PasswordField inputConfirmPass;
    private VBox confirmBox;
    private Label statusLabel;

    private boolean isRegistering = false;

    public MainMenuScene() {
        super(MenuType.MAIN_MENU);

        // --- 背景设置 ---
        var bgStops = List.of(new Stop(0, Color.web("#D3B08C")), new Stop(1, Color.web("#4A2C12")));
        var bgGradient = new RadialGradient(0.5, 0.5, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, bgStops);
        getContentRoot().setBackground(new Background(new BackgroundFill(bgGradient, null, null)));

        var rect = new Rectangle(getAppWidth(), getAppHeight(), Color.web("000", 0.0));
        rect.setMouseTransparent(true);
        FillTransition ft = new FillTransition(Duration.seconds(3), rect, Color.TRANSPARENT, Color.web("000", 0.2));
        ft.setCycleCount(-1);
        ft.setAutoReverse(true);
        ft.play();

        // --- 标题 ---
        var title = new Text("中国象棋");
        try {
            title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(120));
        } catch (Exception e) {
            title.setFont(javafx.scene.text.Font.font(120));
        }
        title.setFill(Color.web("#F0E68C"));
        title.setStroke(Color.web("#5C3A1A"));
        title.setStrokeWidth(3);
        title.setEffect(new DropShadow(15, Color.BLACK));

        var titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);

        // --- 核心容器 ---
        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);

        // 初始化所有视图
        initLoginSelectionView();
        initUserLoginFormView();
        initMainMenuView();

        // 默认显示登录选择界面
        switchView(loginSelectionView);

        // --- 整体布局 ---
        var mainLayout = new VBox(30, titleBox, contentBox);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setTranslateX((getAppWidth() - mainLayout.getBoundsInLocal().getWidth()) / 2);
        mainLayout.setTranslateY((getAppHeight() - mainLayout.getBoundsInLocal().getHeight()) / 2 - 200);

        getContentRoot().getChildren().addAll(rect, mainLayout);
    }

    /**
     * 界面1：选择 用户登录 或 游客登录
     */
    private void initLoginSelectionView() {
        PixelatedButton btnUserLogin = new PixelatedButton("用户登录", "Button1", () -> {
            resetLoginForm();
            switchView(userLoginFormView);
        });

        PixelatedButton btnGuestLogin = new PixelatedButton("游客登录", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.loginAsGuest();
            switchView(mainMenuView);
        });

        PixelatedButton btnExit = new PixelatedButton("退出游戏", "Button1", this::fireExit);

        loginSelectionView = new VBox(15, btnUserLogin, btnGuestLogin, btnExit);
        loginSelectionView.setAlignment(Pos.CENTER);
    }

    /**
     * 界面2：用户账号密码输入
     */
    private void initUserLoginFormView() {
        String fieldStyle = "-fx-background-color: rgba(255,255,255,0.9); -fx-font-size: 16px; -fx-pref-width: 200px; -fx-text-fill: black;";
        String labelStyle = "-fx-text-fill: white; -fx-font-size: 16px; -fx-effect: dropshadow(one-pass-box, black, 2, 0, 0, 0);";

        Label lblUser = new Label("用户名:");
        lblUser.setStyle(labelStyle);
        inputUser = new TextField();
        inputUser.setStyle(fieldStyle);

        Label lblPass = new Label("密码:");
        lblPass.setStyle(labelStyle);
        inputPass = new PasswordField();
        inputPass.setStyle(fieldStyle);

        Label lblConfirm = new Label("确认密码 (注册新用户):");
        lblConfirm.setStyle(labelStyle);
        inputConfirmPass = new PasswordField();
        inputConfirmPass.setStyle(fieldStyle);

        confirmBox = new VBox(5, lblConfirm, inputConfirmPass);
        confirmBox.setAlignment(Pos.CENTER);
        confirmBox.setVisible(false);
        confirmBox.setManaged(false);

        statusLabel = new Label("请输入账号密码");
        statusLabel.setStyle("-fx-text-fill: yellow; -fx-font-size: 14px; -fx-effect: dropshadow(one-pass-box, black, 2, 0, 0, 0);");

        PixelatedButton btnConfirm = new PixelatedButton("确 定", "Button1", this::handleUserLogin);

        PixelatedButton btnBack = new PixelatedButton("返 回", "Button1", () -> {
            switchView(loginSelectionView);
        });

        userLoginFormView = new VBox(10, lblUser, inputUser, lblPass, inputPass, confirmBox, statusLabel, btnConfirm, btnBack);
        userLoginFormView.setAlignment(Pos.CENTER);
    }

    /**
     * 重置表单状态
     */
    private void resetLoginForm() {
        inputUser.clear();
        inputPass.clear();
        inputConfirmPass.clear();

        statusLabel.setText("请输入账号密码");
        statusLabel.setStyle("-fx-text-fill: yellow; -fx-font-size: 14px;");

        isRegistering = false;

        if (confirmBox != null) {
            confirmBox.setVisible(false);
            confirmBox.setManaged(false);
        }
    }

    /**
     * 核心逻辑：处理用户登录/注册
     */
    private void handleUserLogin() {
        String user = inputUser.getText().trim();
        String pass = inputPass.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            statusLabel.setText("账号或密码不能为空");
            statusLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 14px;");
            return;
        }

        XiangQiApp app = (XiangQiApp) FXGL.getApp();
        UserManager userManager = app.getUserManager();

        // 1. 注册确认阶段
        if (isRegistering) {
            String confirm = inputConfirmPass.getText();
            if (pass.equals(confirm)) {
                userManager.registerUser(user, pass);
                app.login(user);

                statusLabel.setText("注册成功！正在进入...");
                statusLabel.setStyle("-fx-text-fill: #55FF55; -fx-font-size: 14px;");

                // 【修复】使用 PauseTransition 代替 FXGL.getGameTimer()，确保在菜单界面能正常跳转
                performDelayedSwitch();
            } else {
                statusLabel.setText("两次密码不一致");
                statusLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 14px;");
            }
            return;
        }

        // 2. 正常登录逻辑
        if (userManager.userExists(user)) {
            if (userManager.verifyPassword(user, pass)) {
                app.login(user);

                statusLabel.setText("登录成功！");
                statusLabel.setStyle("-fx-text-fill: #55FF55; -fx-font-size: 14px;");

                // 【修复】使用 PauseTransition
                performDelayedSwitch();
            } else {
                statusLabel.setText("密码错误");
                statusLabel.setStyle("-fx-text-fill: #FF5555; -fx-font-size: 14px;");
            }
        } else {
            // 用户不存在 -> 进入注册
            isRegistering = true;
            statusLabel.setText("新账号，请再次输入密码以注册");
            statusLabel.setStyle("-fx-text-fill: orange; -fx-font-size: 14px;");

            confirmBox.setVisible(true);
            confirmBox.setManaged(true);
            inputConfirmPass.requestFocus();
        }
    }

    /**
     * 【新增】执行延时跳转
     */
    private void performDelayedSwitch() {
        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(e -> switchView(mainMenuView));
        delay.play();
    }

    /**
     * 界面3：游戏主菜单
     */
    private void initMainMenuView() {
        var btnNewGame = new PixelatedButton("标准对战", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(false);
            app.setLoadedGame(false);
            fireNewGame();
        });

        var btnCustomGame = new PixelatedButton("排局模式", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(true);
            app.setLoadedGame(false);
            fireNewGame();
        });

        var btnLoadGame = new PixelatedButton("读取存档", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.openLoadDialog();
        });

        var btnLogout = new PixelatedButton("注销登录", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.loginAsGuest();
            switchView(loginSelectionView);
        });

        mainMenuView = new VBox(10, btnNewGame, btnCustomGame, btnLoadGame, btnLogout);
        mainMenuView.setAlignment(Pos.CENTER);
    }

    private void switchView(VBox targetView) {
        contentBox.getChildren().clear();
        if (targetView != null) {
            contentBox.getChildren().add(targetView);
        }
    }
}