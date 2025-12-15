package edu.sustech.xiangqi;

import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.scene.*;
import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.SceneFactory;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.SpawnData;
import edu.sustech.xiangqi.model.*;
import edu.sustech.xiangqi.view.XiangQiFactory;
import edu.sustech.xiangqi.controller.InputHandler;
import edu.sustech.xiangqi.controller.boardController;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import com.almasb.fxgl.input.UserAction;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import edu.sustech.xiangqi.manager.UserManager;
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

public class XiangQiApp extends GameApplication {

    public static final int CELL_SIZE = 90; //棋盘格子大小
    public static final int MARGIN = 31;    //留白
    public static final int UI_GAP = 60;    //UI与棋盘间距
    public static final int UI_WIDTH = 180;
    public static final int BOARD_WIDTH = 796;      //宽
    public static final int BOARD_HEIGHT = 887;     //高

    //计算总宽度/高度
    public static final int APP_WIDTH = UI_WIDTH + UI_GAP + BOARD_WIDTH + UI_GAP + UI_WIDTH + 40;
    public static final int APP_HEIGHT = BOARD_HEIGHT + 120;

    //棋盘坐标
    public static final double BOARD_START_X = (APP_WIDTH - BOARD_WIDTH) / 2.0;
    public static final double BOARD_START_Y = (APP_HEIGHT - BOARD_HEIGHT) / 2.0;

    //偏移调整
    public static final int PIECE_OFFSET_X = 5;
    public static final int PIECE_OFFSET_Y = 5;

    //UI
    private Text gameOverBanner;
    private Rectangle gameOverDimmingRect;
    private TurnIndicator turnIndicator;
    private Font gameFont;

    //游戏状态
    private boolean isCustomMode = false;       // 排局模式
    private boolean isSettingUp = false;        // 摆放棋子？/排局
    private boolean isLoadedGame = false;       // 正在加载存档？
    private boolean isRestartingCustom = false; // 正在重置排局？
    private boolean isOnlineLaunch = false;     // 联机模式启动？

    //ai等级
    private int aiLevel = 0;

    //初始状态
    private ChessBoardModel customSetupSnapshot;    // 排局模式
    public ChessBoardModel loadedGameSnapshot;      // 读档游戏
    private boolean isRestartingLoaded = false;     // 重置游戏？

    // 排局模式下放置棋子类型？
    private String selectedPieceType = null;
    private boolean selectedPieceIsRed = true;

    // UI 引用
    private VBox leftSetupPanel;      // 排局模式左侧
    private VBox rightSetupPanel;     // 排局模式右侧
    private VBox standardGameUI;      // 标准游戏右侧
    private VBox leftGameUI;          // 标准游戏左侧
    private VBox turnSelectionPanel;  // 排局模式先手选择
    private HistoryPanel historyPanel;// 棋谱

    // --- 核心组件 ---
    private ChessBoardModel model;           // 棋盘model
    private boardController boardController; // 棋盘
    private InputHandler inputHandler;
    private UserManager userManager;         // 用户管理器
    private String currentUser = "Guest";    // 当前登录用户名
    private boolean isGuestMode = true;      // 游客模式？
    private static final String SAVE_DIR = "saves/"; // 存档路径

    // 视角翻转？
    public static boolean isBoardFlipped = false;

    //音乐列表
    private static final List<String> MUSIC_LIST = new ArrayList<>();
    static {
        // 默认音乐 (根目录)
        MUSIC_LIST.add("Whisper Records - 古の森.mp3");

        // 可切换的音乐 (background 文件夹)
        MUSIC_LIST.add("background/Against_the_rising_tide.wav");
        MUSIC_LIST.add("background/AWAKEN.mp3");
        MUSIC_LIST.add("background/MYSTIC_LIGHT_GUEST.wav");
        MUSIC_LIST.add("background/反常光谱.wav");
        MUSIC_LIST.add("background/巴别塔.wav");
        MUSIC_LIST.add("background/春弦.mp3");
        MUSIC_LIST.add("background/未许之地OST.wav");
        MUSIC_LIST.add("background/浸春芜.wav");
    }
    private static int currentMusicIndex = 0;
    private static boolean isMusicStarted = false;

    // --- Getters & Setters ---
    public void setOnlineLaunch(boolean isOnline) { this.isOnlineLaunch = isOnline; }
    public boolean isOnlineLaunch() { return isOnlineLaunch; }
    public Text getGameOverBanner() { return gameOverBanner; }
    public Rectangle getGameOverDimmingRect() { return gameOverDimmingRect; }
    public void setCustomMode(boolean customMode) { this.isCustomMode = customMode; }
    public void setLoadedGame(boolean loadedGame) { this.isLoadedGame = loadedGame; }
    public boolean isSettingUp() { return isSettingUp; }
    public String getSelectedPieceType() { return selectedPieceType; }
    public boolean isSelectedPieceRed() { return selectedPieceIsRed; }
    public ChessBoardModel getModel() { return model; }
    public TurnIndicator getTurnIndicator() { return turnIndicator; }
    public InputHandler getInputHandler() { return inputHandler; }
    public UserManager getUserManager() { return userManager; }
    public String getCurrentUser() { return currentUser; }
    public boolean isGuest() { return isGuestMode; }
    public boolean isAIEnabled() { return aiLevel > 0; }
    public void setAIEnabled(boolean enabled) {
        this.aiLevel = enabled ? 1 : 0; // 默认开启设为 1，或者重置为 0
    }
    public int getAIDifficulty() {
        return aiLevel;
    }
    public boolean isCustomMode() { return isCustomMode; }

    // 登录
    public void login(String username) { this.currentUser = username; this.isGuestMode = false; }
    public void loginAsGuest() { this.currentUser = "Guest"; this.isGuestMode = true; }

    //信息文本设置
    public void centerTextInApp(Text text) {
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        text.setTranslateX((APP_WIDTH - textWidth) / 2);
        text.setTranslateY((APP_HEIGHT - textHeight) / 2 + text.getFont().getSize() * 0.3);
    }

    //计算棋子坐标
    public static Point2D getVisualPosition(int row, int col) {

        //翻转
        int visualRow = isBoardFlipped ? (9 - row) : row;
        int visualCol = isBoardFlipped ? (8 - col) : col;

        double centerX = BOARD_START_X + MARGIN + visualCol * CELL_SIZE + PIECE_OFFSET_X;
        double centerY = BOARD_START_Y + MARGIN + visualRow * CELL_SIZE + PIECE_OFFSET_Y;

        double pieceRadius = (CELL_SIZE - 8) / 2.0;
        return new Point2D(centerX - pieceRadius, centerY - pieceRadius);
    }

    // 更新棋谱/调用controller
    public void updateHistoryPanel() {
        if (historyPanel != null && model != null) {
            historyPanel.updateHistory(model.getMoveHistoryStack());
        }
    }

    //初始化
    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("中国象棋 1.0");
        settings.setVersion("1.0");
        settings.setWidth(APP_WIDTH);
        settings.setHeight(APP_HEIGHT);

        //可以调整窗口大小
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);

        //主菜单
        settings.setMainMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override public FXGLMenu newMainMenu() { return new MainMenuScene(); }
            @Override public FXGLMenu newGameMenu() { return new InGameMenuScene(); }
        });
    }

    //字体
    @Override
    protected void onPreInit() {
        try {
            gameFont = getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(20);
            userManager = new UserManager();
        } catch (Exception e) {
            gameFont = Font.font("System", FontWeight.BOLD, 20);
        }
    }

    //游戏初始化——加载一堆东西 呢
    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new XiangQiFactory());
        if (!isOnlineLaunch) {
            isBoardFlipped = false;
        }


        if (isLoadedGame) {
            // 第一次加载
            isLoadedGame = false;
            if (this.model == null) this.model = new ChessBoardModel();
            isSettingUp = false;
        } else if (isRestartingCustom) {
            // 排局重开
            isRestartingCustom = false;
            this.model = deepCopy(customSetupSnapshot);
            isSettingUp = true;
            selectedPieceType = null;

        }
        else if (isRestartingLoaded) {
            //残局/读档重开
            isRestartingLoaded = false;
            this.model = deepCopy(loadedGameSnapshot);
            isSettingUp = false;
            isCustomMode = false;
        }
        else if (isOnlineLaunch) {
            //联机
            this.model = new ChessBoardModel();
            isCustomMode = false; isLoadedGame = false; isSettingUp = false;
        } else {
            // 标准
            this.model = new ChessBoardModel();
            if (isCustomMode) {
                this.model.clearBoard();
                isSettingUp = true;
                selectedPieceType = null;
                customSetupSnapshot = null;
            } else {
                isSettingUp = false;
                this.aiLevel = 0;
            }
        }

        spawn("background", 0, 0);
        spawn("board", BOARD_START_X, BOARD_START_Y);

        this.boardController = new boardController(this.model);
        this.inputHandler = new InputHandler(this.boardController);


        spawnPiecesFromModel();




    }


    //生成棋子根据model——棋局变动
    public void spawnPiecesFromModel() {
        //移除
        getGameWorld().getEntitiesByType(EntityType.PIECE).forEach(entity -> entity.removeFromWorld());
        //重新生成
        for (AbstractPiece pieceLogic : model.getPieces()) {
            String prefix = pieceLogic.isRed() ? "Red" : "Black";
            String type = pieceLogic.getClass().getSimpleName().replace("Piece", "");
            Point2D pos = getVisualPosition(pieceLogic.getRow(), pieceLogic.getCol());
            spawn(prefix + type, new SpawnData(pos).put("pieceLogic", pieceLogic));
        }
    }

    //联机连接
    public void startOnlineConnection(String ip, String roomId, Text statusText) {
        this.isOnlineLaunch = true;
        this.setCustomMode(false);
        this.setLoadedGame(false);
        getGameController().startNewGame();
        runOnce(() -> {
            if (boardController != null) boardController.connectToRoom(ip, roomId);
        }, Duration.seconds(0.1));
    }

    //UI初始化
    @Override
    protected void initUI() {
        getGameScene().getRoot().setStyle("-fx-background-color: #3a2e24;");
        // 清理 UI
        if (leftGameUI != null) removeUINode(leftGameUI);

        gameOverDimmingRect = new Rectangle(APP_WIDTH, APP_HEIGHT, Color.web("000", 0.0));
        gameOverDimmingRect.setVisible(false);
        gameOverDimmingRect.setMouseTransparent(true);
        addUINode(gameOverDimmingRect);

        gameOverBanner = new Text();
        try { gameOverBanner.setFont(getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(80)); }
        catch (Exception e) { gameOverBanner.setFont(Font.font(80)); }
        gameOverBanner.setFill(Color.BROWN);
        gameOverBanner.setStroke(Color.BLACK);
        gameOverBanner.setStrokeWidth(3);
        gameOverBanner.setVisible(false);
        addUINode(gameOverBanner);

        if (isOnlineLaunch) {
            initOnlineGameUI();
        } else if (isCustomMode) {
            if (isSettingUp) initSetupUI(); // 排局：摆放阶段
            else initStandardGameUI();      // 排局：对战阶段
        } else {
            initStandardGameUI();
        }

    }

    //标准模式 UI
    private void initStandardGameUI() {
        // 1. 右侧面板
        double rightX = BOARD_START_X + BOARD_WIDTH + UI_GAP - 20;
        var btnUndo = new PixelatedButton("悔棋", "Button1", () -> { if (boardController != null) boardController.undo(); });
        var btnSurrender = new PixelatedButton("投降", "Button1", () -> { if (boardController != null) boardController.surrender(); });
        var btnSave = new PixelatedButton("保存游戏", "Button1", this::openSaveDialog);
        var btnRestart = new PixelatedButton("重新开始", "Button1", this::handleRestartGame);
        var btnHistory = new PixelatedButton("菜单", "Button1", () -> getGameController().gotoGameMenu());
        standardGameUI = new VBox(10, btnUndo, btnSave, btnRestart, btnSurrender, btnHistory);
        addUINode(standardGameUI, rightX, 50);

        // 2. 左侧面板 (AI + 棋谱)
        double leftX = Math.max(20, BOARD_START_X - UI_GAP - UI_WIDTH);

        var btnToggleAI = new PixelatedButton("AI: 关闭", "Button1", null);
        updateAIButtonText(btnToggleAI);

        btnToggleAI.setOnMouseClicked(e -> {
            // 循环切换难度: 0 -> 1 -> 2 -> 3 -> 4 -> 0
            aiLevel = (aiLevel + 1) % 5;

            // 更新文字
            updateAIButtonText(btnToggleAI);

            //Ai触发？
            if (aiLevel > 0 && !model.isRedTurn() && boardController != null) {
                boardController.startAITurn(aiLevel);
            }
        });

        var btnHint = new PixelatedButton("AI 提示", "Button1", () -> {
            if (boardController != null) boardController.requestAIHint();
        });

        // 棋谱按钮
        var btnNotation = new PixelatedButton("棋 谱", "Button1", () -> {
            if (historyPanel != null) {
                historyPanel.setVisible(!historyPanel.isVisible());
                if (historyPanel.isVisible()) updateHistoryPanel();
            }
        });

        leftGameUI = new VBox(10, btnToggleAI, btnHint, btnNotation);
        addUINode(leftGameUI, leftX, 50);

        //谁的回合
        turnIndicator = new TurnIndicator();
        turnIndicator.update(model.isRedTurn(), false);
        addUINode(turnIndicator, rightX, 750);

        // 3. 棋谱（隐藏）
        historyPanel = new HistoryPanel(220, 600);
        historyPanel.setTranslateX(17);
        historyPanel.setTranslateY(APP_HEIGHT / 2.0 - 200);
        historyPanel.setVisible(false);
        addUINode(historyPanel);
    }


    //联机模式 UI
    private void initOnlineGameUI() {
        double uiX = BOARD_START_X + BOARD_WIDTH + UI_GAP - 20;
        var btnUndo = new PixelatedButton("申请悔棋", "Button1", () -> { if (boardController != null) boardController.undoOnline(); });
        var btnRestart = new PixelatedButton("申请重开", "Button1", () -> { if (boardController != null) boardController.restartOnline(); });
        var btnSwap = new PixelatedButton("交换先手", "Button1", () -> { if (boardController != null) boardController.swapOnline(); });
        var btnSurrender = new PixelatedButton("投降 / 退出", "Button1", () -> { if (boardController != null) boardController.surrenderOnline(); });
        var btnExit = new PixelatedButton("返回大厅", "Button1", () -> getGameController().gotoMainMenu());

        standardGameUI = new VBox(10, btnUndo, btnRestart, btnSwap, btnSurrender, btnExit);
        addUINode(standardGameUI, uiX, 50);

        turnIndicator = new TurnIndicator();
        turnIndicator.update(true, false);
        addUINode(turnIndicator, uiX, 750);

        //左侧面板
        double leftX = Math.max(20, BOARD_START_X - UI_GAP - UI_WIDTH);
        var btnSave = new PixelatedButton("保存游戏", "Button1", this::openSaveDialog);

        var btnNotation = new PixelatedButton("棋 谱", "Button1", () -> {
            if (historyPanel != null) {
                historyPanel.setVisible(!historyPanel.isVisible());
                if (historyPanel.isVisible()) updateHistoryPanel();
            }
        });

        leftGameUI = new VBox(10, btnSave, btnNotation);
        addUINode(leftGameUI, leftX, 50);

        historyPanel = new HistoryPanel(220, 600);
        historyPanel.setTranslateX(17);
        historyPanel.setTranslateY(APP_HEIGHT / 2.0 - 200);
        historyPanel.setVisible(false);
        addUINode(historyPanel);
    }

    // 重启逻辑
    private void handleRestartGame() {
        getDialogService().showConfirmationBox("确定要重新开始吗？", yes -> {
            if (yes) {
                if (isCustomMode && customSetupSnapshot != null) {
                    isRestartingCustom = true;
                }
                else if (loadedGameSnapshot != null) {
                    isRestartingLoaded = true;
                }
                else {
                    isCustomMode = false; isLoadedGame = false;
                }
                getGameController().startNewGame();
            }
        });
    }

    // 排局 UI (内联实现)
    private void initSetupUI() {
        double leftX = Math.max(20, BOARD_START_X - UI_GAP - UI_WIDTH);
        leftSetupPanel = createPiecePalette(true);
        leftSetupPanel.setTranslateX(leftX); leftSetupPanel.setTranslateY(50);

        double rightX = BOARD_START_X + BOARD_WIDTH + UI_GAP;
        rightSetupPanel = createPiecePalette(false);
        rightSetupPanel.setTranslateX(rightX); rightSetupPanel.setTranslateY(50);

        PixelatedButton btnEraser = new PixelatedButton("橡皮擦", "Button1", () -> {
            resetPaletteStyles(); selectedPieceType = "Eraser"; selectedPieceIsRed = false;
            getDialogService().showMessageBox("橡皮擦模式：点击棋子删除");
        });
        btnEraser.setScaleX(0.7); btnEraser.setScaleY(0.7);

        // 先手选择
        setupRedFirst = true;
        PixelatedButton btnToggleTurn = new PixelatedButton("先手: 红方", "Button1", null);
        btnToggleTurn.setTextColor(Color.RED);
        btnToggleTurn.setScaleX(0.7); btnToggleTurn.setScaleY(0.7);
        btnToggleTurn.setOnMouseClicked(e -> {
            setupRedFirst = !setupRedFirst;
            if(setupRedFirst) { btnToggleTurn.setText("先手: 红方"); btnToggleTurn.setTextColor(Color.RED); }
            else { btnToggleTurn.setText("先手: 黑方"); btnToggleTurn.setTextColor(Color.BLACK); }
            FXGL.play("button.mp3");
        });

        Label turnLabel = new Label("先手选择"); turnLabel.setFont(gameFont); turnLabel.setTextFill(Color.WHITE);

        PixelatedButton btnSaveSetup = new PixelatedButton("保存排局", "Button1", this::openSaveDialog);
        btnSaveSetup.setScaleX(0.7); btnSaveSetup.setScaleY(0.7);

        turnSelectionPanel = new VBox(-15, btnSaveSetup, btnEraser, btnToggleTurn);
        turnSelectionPanel.setAlignment(Pos.CENTER_LEFT);
        turnSelectionPanel.setTranslateX(leftX); turnSelectionPanel.setTranslateY(APP_HEIGHT - 350);

        PixelatedButton btnStartCustom = new PixelatedButton("开始对局", "Button1", () -> tryStartCustomGame(setupRedFirst));
        Label hintLabel = new Label("提示：\n左红右黑\n点击放置"); hintLabel.setFont(gameFont); hintLabel.setTextFill(Color.WHITE);
        btnStartCustom.setScaleX(0.9); btnStartCustom.setScaleY(0.9);

        VBox controlBox = new VBox(15, hintLabel, btnStartCustom);
        controlBox.setAlignment(Pos.CENTER); controlBox.setStyle("-fx-padding: 30 0 0 0;");
        rightSetupPanel.getChildren().add(controlBox);

        addUINode(leftSetupPanel); addUINode(rightSetupPanel); addUINode(turnSelectionPanel);
    }

    private boolean setupRedFirst = true;

    //自定义排局验证是否正确
    public void tryStartCustomGame(boolean isRedFirst) {
        AbstractPiece rK = model.FindKing(true), bK = model.FindKing(false);
        if (rK == null || bK == null) { getDialogService().showMessageBox("必须各有一将！"); return; }
        if (rK.getCol() == bK.getCol()) {
            boolean block = false;
            for (int r=Math.min(rK.getRow(),bK.getRow())+1; r<Math.max(rK.getRow(),bK.getRow()); r++)
                if (model.getPieceAt(r, rK.getCol())!=null) { block=true; break; }
            if(!block) { getDialogService().showMessageBox("将帅不能照面（飞将）！"); return; }
        }
        customSetupSnapshot = deepCopy(model);
        removeUINode(leftSetupPanel); removeUINode(rightSetupPanel); removeUINode(turnSelectionPanel);
        model.setRedTurn(isRedFirst); isSettingUp = false; selectedPieceType = null;
        initStandardGameUI(); turnIndicator.update(isRedFirst, false);
        if (!isRedFirst && isAIEnabled() && boardController!=null) boardController.startAITurn(aiLevel);
    }

    //创建棋子
    private VBox createPiecePalette(boolean red) {
        VBox box = new VBox(10); box.setAlignment(Pos.TOP_CENTER); box.setPrefWidth(UI_WIDTH);
        Label t = new Label(red?"红方":"黑方"); t.setFont(gameFont); t.setTextFill(red?Color.RED:Color.BLACK); box.getChildren().add(t);
        String[] types = {"General","Advisor","Elephant","Horse","Chariot","Cannon","Soldier"};
        for(String type : types) {
            String prefix = red ? "Red" : "Black";
            ImageView img = new ImageView(FXGL.getAssetLoader().loadTexture(prefix + type + ".png").getImage());
            img.setFitWidth(55); img.setPreserveRatio(true);
            Button btn = new Button("", img); btn.setStyle("-fx-background-color: transparent;");
            btn.setOnAction(e->{
                resetPaletteStyles(); btn.setStyle("-fx-background-color: rgba(255,255,0,0.3);");
                selectedPieceType=type; selectedPieceIsRed=red; FXGL.play("button.mp3");
            });
            box.getChildren().add(btn);
        }
        return box;
    }

    private void resetPaletteStyles() {
        if(leftSetupPanel!=null) leftSetupPanel.getChildren().forEach(n->n.setStyle("-fx-background-color: transparent;"));
        if(rightSetupPanel!=null) rightSetupPanel.getChildren().forEach(n->n.setStyle("-fx-background-color: transparent;"));
    }

    private ChessBoardModel deepCopy(ChessBoardModel original) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
            return (ChessBoardModel) new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray())).readObject();
        } catch (Exception e) { return null; }
    }

    // --- 存档读档 ---

    //自动保存逻辑
    public void saveAutoGame() {
        if (isGuestMode) return;
        new File(SAVE_DIR + currentUser + "/").mkdirs();
        // 文件名格式：Username_autosave.dat

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_DIR + currentUser + "/" + "autosave.dat"))) {
            oos.writeObject(model);
            System.out.println("自动保存成功: 第 " + model.getMoveHistoryStack().size() + " 步");
        } catch (Exception e) {
            System.out.println("自动保存失败: " + e.getMessage());
        }
    }

    //保存在某个位置
    private void saveGameToSlot(int slot) {
        new File(SAVE_DIR+ currentUser + "/").mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_DIR + currentUser + "/" + "save_" + slot + ".dat"))) {
            oos.writeObject(model);
            getDialogService().showMessageBox("保存成功");
        } catch (Exception e) {}
    }

    //对话框
    public void openSaveDialog() {
        if (isGuestMode) { getDialogService().showMessageBox("游客无法存档"); return; }

        // 加入 "返 回" 选项
        getDialogService().showChoiceBox("选择位置", List.of("存档 1", "存档 2", "存档 3", "返 回"), s -> {
            if (s.equals("返 回")) return;
            saveGameToSlot(Integer.parseInt(s.split(" ")[1]));
        });
    }

    //加载存档
    private void loadGameFromSlot(int slot) {
        String fileName;
        if (slot == -1) {
            fileName = SAVE_DIR + currentUser + "/" + "autosave.dat";
        } else {
            fileName = SAVE_DIR + currentUser + "/" + "save_" + slot + ".dat";
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            ChessBoardModel m = (ChessBoardModel) ois.readObject();
            m.rebuildAfterLoad();

            this.model = m;
            this.loadedGameSnapshot = deepCopy(m);
            this.isCustomMode = false;
            this.isLoadedGame = true;
            getGameController().startNewGame();
        }
        catch (Exception e) {
            getDialogService().showMessageBox("读取失败"); }
    }

    //读取框
    public void openLoadDialog() {
        if (isGuestMode) { getDialogService().showMessageBox("游客无法读档"); return; }
        List<String> slots = new ArrayList<>();
        if (new File(SAVE_DIR + currentUser + "/" + "autosave.dat").exists()) {
            slots.add("自动存档");
        }

        for (int i=1; i<=3; i++) if (new File(SAVE_DIR + currentUser + "/" + "save_" + i + ".dat").exists()) slots.add("存档 " + i);
        if (slots.isEmpty()) {
            getDialogService().showMessageBox("无存档");
            return;
        }
        getDialogService().showChoiceBox("读取位置", slots, s -> {
            if (s.equals("自动存档")) {
                loadGameFromSlot(-1); // -1 代表自动存档
            } else {
                loadGameFromSlot(Integer.parseInt(s.split(" ")[1]));
            }
        });
    }

    public boolean hasSaveFile() {
        if (isGuestMode) return false;
        for (int i=1; i<=3; i++) if (new File(SAVE_DIR + currentUser + "/" + "_save_" + i + ".dat").exists()) return true;
        return false;
    }

    @Override protected void initInput() {
        getInput().addAction(new UserAction("Click") {
            @Override protected void onActionEnd() { if (inputHandler != null) inputHandler.handleMouseClick(getInput().getMousePositionWorld()); }
        }, MouseButton.PRIMARY);
    }

    //残局模式读取文件
    public void loadEndgameFromFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ChessBoardModel m = (ChessBoardModel) ois.readObject();
            m.rebuildAfterLoad();

            this.model = m;
            this.loadedGameSnapshot = deepCopy(m);

            this.isCustomMode = false;
            this.isLoadedGame = true; // 标记为读档模式
            this.isSettingUp = false;
            this.isOnlineLaunch = false;
            this.aiLevel = 4;//默认打开AI
            // 进入游戏场景
            getGameController().startNewGame();

        } catch (Exception e) {
            e.printStackTrace();
            getDialogService().showMessageBox("读取残局文件失败: " + file.getName());
        }
    }

    //AI难度显示
    private void updateAIButtonText(PixelatedButton btn) {
        String text = "";
        switch (aiLevel) {
            case 0: text = "AI: 关闭"; break;
            case 1: text = "AI: 新手"; break; // 深度 1
            case 2: text = "AI: 中等"; break; // 深度 2
            case 3: text = "AI: 困难"; break; // 深度 3
            case 4: text = "AI: 专家"; break; // 深度 4
        }
        btn.setText(text);

        //根据难度改变文字颜色
        if (aiLevel == 0) btn.setTextColor(Color.WHITE);
        else if (aiLevel <= 2) btn.setTextColor(Color.LIGHTGREEN);
        else if (aiLevel == 3) btn.setTextColor(Color.YELLOW);
        else btn.setTextColor(Color.RED);
    }

    //背景音乐
    public static String getCurrentMusicName() {
        String path = MUSIC_LIST.get(currentMusicIndex);
        int lastSlash = path.lastIndexOf('/');
        String fileName = (lastSlash == -1) ? path : path.substring(lastSlash + 1);
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? fileName : fileName.substring(0, lastDot);
    }

    //切歌
    public static void switchNextMusic() {
        currentMusicIndex = (currentMusicIndex + 1) % MUSIC_LIST.size();

        //停止当前所有音乐——播放新的
        FXGL.getAudioPlayer().stopAllMusic();
        FXGL.loopBGM(MUSIC_LIST.get(currentMusicIndex));

        isMusicStarted = true;
    }

    //判断
    public static void ensureMusicPlaying() {
        if (!isMusicStarted) {
            FXGL.loopBGM(MUSIC_LIST.get(currentMusicIndex));
            isMusicStarted = true;
        }
    }

    //main
    public static void main(String[] args) {
        new Thread(() -> { try { edu.sustech.xiangqi.server.XiangQiServer.main(null); } catch (Exception e) {} }).start();
        launch(args);
    }
}