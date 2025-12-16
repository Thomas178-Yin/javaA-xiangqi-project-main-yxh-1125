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
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
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

import java.io.File;
import java.util.*;

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

    // 视觉粒子
    private final List<PixelDust> particles = new ArrayList<>();
    private double timeAccumulator = 0;

    //视觉常量
    private static final Color BG_DARK = Color.web("#181010");
    private static final Color BG_LIGHT = Color.web("#2a1e15");
    private static final Color GRID_COLOR = Color.web("#ffffff", 0.03); // 极淡的网格线
    private static final Color TEXT_COLOR = Color.web("#f0e6d2");
    private static final Color ERROR_COLOR = Color.web("#ff5555");

    private VBox endgameView;               // 残局选择界面
    private GridPane endgameGrid;           // 用于放按钮的网格
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 16;
    private List<File> allEndgameFiles = new ArrayList<>();
    private Label pageLabel;                // 显示 "第 1 / 3 页"

    //设置
    private VBox mainMenuBox;   // 主界面
    private VBox settingsBox;   // 设置界面
    private VBox creditsBox;    // 制作人界面

    // 输入框样式
    private static final String INPUT_STYLE =
            "-fx-background-color: rgba(0,0,0,0.3); -fx-border-color: #665544; -fx-border-width: 0 0 2 0; -fx-text-fill: #f0e6d2; -fx-font-size: 16px; -fx-font-family: 'Monospaced';";
    private static final String INPUT_FOCUS_STYLE =
            "-fx-background-color: rgba(0,0,0,0.5); -fx-border-color: #d4a173; -fx-border-width: 0 0 2 0; -fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-family: 'Monospaced';";

    public MainMenuScene() {
        super(MenuType.MAIN_MENU);

        //音乐
        XiangQiApp.ensureMusicPlaying();

//        初始化
        initSettingsMenu();
        initCreditsMenu();
        initMainMenuView();
        //背景
        Rectangle bg = new Rectangle(getAppWidth(), getAppHeight());
        bg.setFill(new RadialGradient(0, 0, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, BG_LIGHT), new Stop(1, BG_DARK)));
        getContentRoot().getChildren().add(bg);

        //像素网格
        Canvas gridCanvas = createGridCanvas();
        getContentRoot().getChildren().add(gridCanvas);

        //标题
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

        //内容
        contentBox = new VBox(20);
        contentBox.setAlignment(Pos.TOP_CENTER);

        // 初始化视图
        initLoginSelectionView();
        initUserLoginFormView();
        initMainMenuView();
        initOnlineLobbyView();

        // 布局
        VBox layout = new VBox(40, titleBox, contentBox);
        layout.setAlignment(Pos.TOP_CENTER);

//        // 120 这个数值你可以微调，越小标题越靠上
//        layout.setTranslateY(120);
//        layout.setTranslateX(getAppWidth() / 2.0 - 150);
//
//        getContentRoot().getChildren().add(layout);
        // 占满全屏的 StackPane
        StackPane rootWrapper = new StackPane();
        rootWrapper.setPrefSize(getAppWidth(), getAppHeight());

        //居中
        rootWrapper.setAlignment(Pos.TOP_CENTER);

        rootWrapper.getChildren().add(layout);

        // 控制标题距离顶部的距离
        rootWrapper.setPadding(new javafx.geometry.Insets(120, 0, 0, 0));

        getContentRoot().getChildren().add(rootWrapper);

        // 默认显示
        switchView(loginSelectionView);
        animateIn(layout);
    }

    //粒子更新
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

    // 管理粒子
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

    //切换逻辑

    private void switchView(VBox targetView) {
        getContentRoot().getChildren().remove(settingsBox);
        getContentRoot().getChildren().remove(creditsBox);
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

    // 初始化登录选择界面

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

    //用户登录
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

        // 这里的按钮文字要调整
        var btnConfirm = new PixelatedButton("确定", "Button1", this::handleUserLogin);
        // 手动缩小一点尺寸!!!!!!!
        btnConfirm.setScaleX(0.9); btnConfirm.setScaleY(0.9);

        var btnBack = new PixelatedButton("返回", "Button1", () -> switchView(loginSelectionView));
        btnBack.setScaleX(0.9); btnBack.setScaleY(0.9);

        btnBox.getChildren().addAll(btnConfirm, btnBack);

        userLoginFormView = new VBox(20, inputUser, inputPass, confirmBox, statusLabel, btnBox);
        userLoginFormView.setAlignment(Pos.CENTER);
    }

    //初始化主功能菜单界面
    private void initMainMenuView() {
        double scale = 0.6;

        var btnNew = new PixelatedButton("标准对战", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(false);
            app.setLoadedGame(false);
            app.setOnlineLaunch(false);
            app.loadedGameSnapshot = null;
            app.setReplayMode(false);
            fireNewGame();
        });
        btnNew.setScaleX(scale); btnNew.setScaleY(scale);

        var btnCustom = new PixelatedButton("排局模式", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(true);
            app.setLoadedGame(false);
            app.setOnlineLaunch(false);
            app.loadedGameSnapshot = null;
            app.setReplayMode(false);
            fireNewGame();
        });
        btnCustom.setScaleX(scale); btnCustom.setScaleY(scale);

//        //////////////////////////////
//        var btnEndgame = new PixelatedButton("残局定式", "Button1", () -> {
//            refreshEndgameList(); // 每次点击都刷新一下文件列表
//            switchView(endgameView);
//        });
//        btnEndgame.setScaleX(scale); btnEndgame.setScaleY(scale);

        var btnChallenge = new PixelatedButton("残局定式", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setReplayMode(false);
            initEndgameView(); // 初始化或刷新
            switchView(endgameView);
        });
        btnChallenge.setScaleX(scale); btnChallenge.setScaleY(scale);

        var btnReplay = new PixelatedButton("精彩回放", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            showReplayFileSelector();

        });
        btnReplay.setScaleX(scale); btnReplay.setScaleY(scale);

        var btnLoad = new PixelatedButton("读取存档", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setOnlineLaunch(false);
            app.setReplayMode(false);
            app.openLoadDialog();
        });
        btnLoad.setScaleX(scale); btnLoad.setScaleY(scale);

        var btnNet = new PixelatedButton("联网对战", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setReplayMode(false);

            if (app.isGuest()) {
                FXGL.getDialogService().showMessageBox("游客模式无法进行联网对战。");
                return;
            }
            switchView(onlineLobbyView);
        });
        btnNet.setScaleX(scale); btnNet.setScaleY(scale);
        var btnSettings = new PixelatedButton("设 置", "Button1", () -> switchView(settingsBox));
        btnSettings.setScaleX(scale); btnSettings.setScaleY(scale);

        var btnLogout = new PixelatedButton("注销登录", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setOnlineLaunch(false);
            app.loginAsGuest();
            switchView(loginSelectionView);
        });
        btnLogout.setScaleX(scale); btnLogout.setScaleY(scale);

        mainMenuView = new VBox(-50, btnNew, btnCustom,
//                btnEndgame,
                btnChallenge, btnLoad, btnReplay, btnNet, btnSettings, btnLogout);
        mainMenuView.setAlignment(Pos.CENTER);

    }

    //初始化残局界面
    private void initEndgameView() {
        if (endgameView != null) {
            refreshEndgameList();
            return;
        }

        Label title = new Label("残 局 挑 战");
        title.setTextFill(TEXT_COLOR);
        try { title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(36)); } catch(Exception e){}

        endgameGrid = new GridPane();
        endgameGrid.setAlignment(Pos.CENTER);
        endgameGrid.setHgap(15);
        endgameGrid.setVgap(15);

        //网格固定
        endgameGrid.setPrefSize(550, 240);
        endgameGrid.setMaxSize(550, 240);
        endgameGrid.setMinSize(550, 240);

        //列宽
        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25); // 每列占 25% 宽度
            col.setHalignment(HPos.CENTER); // 按钮在格子里居中
            endgameGrid.getColumnConstraints().add(col);
        }

        // 行高
        for (int i = 0; i < 4; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(25); // 每行占 25% 高度
            row.setValignment(VPos.CENTER);
            endgameGrid.getRowConstraints().add(row);
        }

        var btnPrev = new PixelatedButton("上一页", "Button1", this::prevPage);
        btnPrev.setScaleX(0.7); btnPrev.setScaleY(0.7);

        var btnNext = new PixelatedButton("下一页", "Button1", this::nextPage);
        btnNext.setScaleX(0.7); btnNext.setScaleY(0.7);

        pageLabel = new Label("1 / 1");
        pageLabel.setTextFill(Color.WHITE);
        pageLabel.setStyle("-fx-font-size: 16px;");

        HBox navBox = new HBox(20, btnPrev, pageLabel, btnNext);
        navBox.setAlignment(Pos.CENTER);

        var btnBack = new PixelatedButton("返回主菜单", "Button1", () -> switchView(mainMenuView));
        btnBack.setScaleX(0.9); btnBack.setScaleY(0.9);

        endgameView = new VBox(20, title, endgameGrid, navBox, btnBack);
        endgameView.setAlignment(Pos.CENTER);



        refreshEndgameList();
    }

    //数据加载与分页逻辑

    private void refreshEndgameList() {
        // 1. 扫描 saves 目录
        File dir = new File("saves/canju/");
        if (!dir.exists()) dir.mkdirs();

        File[] files = dir.listFiles((d, name) -> name.startsWith("canju") && name.endsWith(".dat"));

        allEndgameFiles.clear();
        if (files != null) {
            // 按文件名排序---canju01, canju02...
            Arrays.sort(files, Comparator.comparing(File::getName));
            allEndgameFiles.addAll(Arrays.asList(files));
        }

        currentPage = 0;
        updateGrid();
    }

    //更新关卡按钮
    private void updateGrid() {
        endgameGrid.getChildren().clear(); // 清空内容，但保留上面设置的行列约束

        int totalPages = (int) Math.ceil((double) allEndgameFiles.size() / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        pageLabel.setText((currentPage + 1) + " / " + totalPages);

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allEndgameFiles.size());

        int row = 0;
        int col = 0;

        for (int i = start; i < end; i++) {
            File file = allEndgameFiles.get(i);
            String fileName = "第 " + (i + 1) + " 关";

            PixelatedButton btn = new PixelatedButton(fileName, "Button1", () -> {
                XiangQiApp app = (XiangQiApp) FXGL.getApp();
                app.loadEndgameFromFile(file);
            });
            btn.setScaleX(0.6);btn.setScaleY(0.6);


            StackPane cellContainer = new StackPane(btn);
            cellContainer.setPrefSize(120, 50);
            cellContainer.setAlignment(Pos.CENTER);

            endgameGrid.add(cellContainer, col, row);

            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }

        // 提示
        if (allEndgameFiles.isEmpty()) {
            Label emptyLabel = new Label("暂无残局文件");
            emptyLabel.setTextFill(Color.GRAY);
            endgameGrid.add(emptyLabel, 1, 1, 2, 1); // 放在中间
        }
    }

    private void prevPage() {
        if (currentPage > 0) {
            currentPage--;
            updateGrid();
        }
    }

    private void nextPage() {
        int totalPages = (int) Math.ceil((double) allEndgameFiles.size() / ITEMS_PER_PAGE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            updateGrid();
        }
    }

    //初始化联机大厅界面
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

    // 登录逻辑
    private void resetLoginForm() {
        inputUser.clear(); inputPass.clear(); inputConfirmPass.clear();
        statusLabel.setText(" "); isRegistering = false;
        if (confirmBox != null) { confirmBox.setVisible(false); confirmBox.setManaged(false); }
    }

    private void handleUserLogin() {
        String user = inputUser.getText().trim();
        String pass = inputPass.getText();
        if (user.equals("canju")){
            statusLabel.setText("不能使用这个用户名"); statusLabel.setTextFill(ERROR_COLOR); return;
        }
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

    //区分switchView,这里不能用此方法,会冲突
    private void switchMenu(VBox menu) {
        getContentRoot().getChildren().remove(mainMenuBox);
        getContentRoot().getChildren().remove(settingsBox);
        getContentRoot().getChildren().remove(creditsBox);
        if(menu != null){getContentRoot().getChildren().add(menu);}

        menu.setTranslateX(XiangQiApp.APP_WIDTH / 2.0 - 100);
        menu.setTranslateY(XiangQiApp.APP_HEIGHT / 2.0 - 200);

    }

    //初始化设置菜单
    private void initSettingsMenu() {
        //音量
        Text volTitle = new Text("音 量");
        volTitle.setFill(Color.WHITE);
        volTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        //显示
        Text volValue = new Text();
        volValue.setFill(Color.LIGHTGREEN);
        volValue.setStyle("-fx-font-size: 18px;");
        // 绑定全局音量
        volValue.textProperty().bind(
                Bindings.format("%.0f%%", FXGL.getSettings().globalSoundVolumeProperty().multiply(100))
        );

        var btnVolDown = new PixelatedButton("-", "Button1", () -> {
            double v = FXGL.getSettings().getGlobalSoundVolume();
            FXGL.getSettings().setGlobalSoundVolume(Math.max(v - 0.1, 0));

            FXGL.getSettings().setGlobalMusicVolume(FXGL.getSettings().getGlobalSoundVolume()); // 同步音乐
        });
        btnVolDown.setMinWidth(60); btnVolDown.setPrefWidth(60);
        btnVolDown.setScaleX(0.3);btnVolDown.setScaleY(0.3);
        btnVolDown.setFontSize(80);
        btnVolDown.setTextY(-20);

        var btnVolUp = new PixelatedButton("+", "Button1", () -> {
            double v = FXGL.getSettings().getGlobalSoundVolume();
            FXGL.getSettings().setGlobalSoundVolume(Math.min(v + 0.1, 1));

            FXGL.getSettings().setGlobalMusicVolume(FXGL.getSettings().getGlobalSoundVolume()); // 同步音乐
        });
        btnVolUp.setMinWidth(60); btnVolUp.setPrefWidth(60);
        btnVolUp.setScaleX(0.3);btnVolUp.setScaleY(0.3);
        btnVolUp.setFontSize(80);
        btnVolUp.setTextY(-20);

        VBox volBox = new VBox(5, volTitle, new javafx.scene.layout.HBox(10, btnVolDown, volValue, btnVolUp));
        ((javafx.scene.layout.HBox)volBox.getChildren().get(1)).setAlignment(Pos.CENTER);
        volBox.setAlignment(Pos.CENTER);

        //音乐
        var btnMusic = new PixelatedButton("音乐: " + XiangQiApp.getCurrentMusicName(), "Button1", () -> {
            XiangQiApp.switchNextMusic();
        });
        btnMusic.setOnMouseClicked(e -> {
            XiangQiApp.switchNextMusic();
            btnMusic.setText("音乐: " + XiangQiApp.getCurrentMusicName());
            FXGL.play("按钮音效1.mp3");
        });
        btnMusic.setFontSize(25);
        btnMusic.setScaleX(0.8);btnMusic.setScaleY(0.8);

        //屏幕大小
        var btnFullscreen = new PixelatedButton("切换全屏/窗口", "Button1", () -> {
            var stage = FXGL.getPrimaryStage();
            stage.setFullScreen(!stage.isFullScreen());
        });
        btnFullscreen.setFontSize(25);
        btnFullscreen.setScaleX(0.8);btnFullscreen.setScaleY(0.8);

        //制作人
        var btnCredits = new PixelatedButton("制作人信息", "Button1", () -> switchView(creditsBox));
        btnCredits.setScaleX(0.8);btnCredits.setScaleY(0.8);

        //返回
        var btnBack = new PixelatedButton("返 回", "Button1", () -> switchView(mainMenuView));
        btnBack.setScaleX(0.8);btnBack.setScaleY(0.8);

        settingsBox = new VBox(-20, volBox, btnMusic, btnFullscreen, btnCredits, btnBack);
        settingsBox.setAlignment(Pos.CENTER);
    }

    //回放
    private void showReplayFileSelector() {
        XiangQiApp app = (XiangQiApp) FXGL.getApp();
        String currentUser = app.getCurrentUser();
        String saveDir = "saves/";

        List<String> options = new ArrayList<>();
        // 扫描存档
        if (new File(saveDir + currentUser + "/" + "autosave.dat").exists()) options.add("自动存档 (最近一局)");
        for (int i=1; i<=3; i++) {
            if (new File(saveDir + currentUser + "/" + "save_" + i + ".dat").exists()) {
                options.add("存档 " + i);
            }
        }

        if (options.isEmpty()) {
            FXGL.getDialogService().showMessageBox("没有可回放的存档。");
            return;
        }

        FXGL.getDialogService().showChoiceBox("选择要回放的对局", options, selected -> {
            String filename;
            if (selected.contains("自动")) {
                filename = saveDir + currentUser + "/" + "autosave.dat";
            } else {
                // 解析 "存档 1" -> 1
                String num = selected.split(" ")[1];
                filename = saveDir + currentUser + "/" +"save_" + num + ".dat";
            }
            app.loadReplayFromFile(new File(filename));
        });
    }

    //制作人
    private void initCreditsMenu() {
        Text title = new Text("=== 制作团队 ===");
        title.setFill(Color.GOLD);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");


        String content =
                "主程序: 陶飞翔、尹玺骅\n" +
                        "AI设计: 陶飞翔\n" +
                        "游戏模式: 尹玺骅\n\n" +
                        "特别感谢: 陶伊达老师、王大兴老师\n" +
                        "Sustech CS109.\n" +
                        "2025/12\n\n"+
                        "github:\n" +
                        "项目代码：https://github.com/Thomas178-Yin/javaA-xiangqi-project-main-yxh-1125/tree/project_1202_online1.0\n" +
                        "尹玺骅：https://github.com/Thomas178-Yin\n" +
                        "陶飞翔：https://github.com/kevin258258";

        Text text = new Text(content);
        text.setFill(Color.WHITE);
        text.setStyle("-fx-font-size: 18px; -fx-line-spacing: 8px;");
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        var btnBack = new PixelatedButton("返 回", "Button1", () -> switchView(settingsBox));

        creditsBox = new VBox(20, title, text, btnBack);
        creditsBox.setAlignment(Pos.CENTER);
        creditsBox.setTranslateY(-50);
    }
}
