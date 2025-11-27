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
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import com.almasb.fxgl.input.Input;
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
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import static com.almasb.fxgl.dsl.FXGL.*;

public class XiangQiApp extends GameApplication {

    // --- 常量定义 ---
    public static final int CELL_SIZE = 90;
    public static final int MARGIN = 31;
    public static final int UI_GAP = 60;
    public static final int UI_WIDTH = 180;

    public static final int BOARD_WIDTH = 796;
    public static final int BOARD_HEIGHT = 887;

    public static final int APP_WIDTH = UI_WIDTH + UI_GAP + BOARD_WIDTH + UI_GAP + UI_WIDTH + 40;
    public static final int APP_HEIGHT = BOARD_HEIGHT + 120;

    public static final double BOARD_START_X = (APP_WIDTH - BOARD_WIDTH) / 2.0;
    public static final double BOARD_START_Y = (APP_HEIGHT - BOARD_HEIGHT) / 2.0;

    // 微调偏移量
    public static final int PIECE_OFFSET_X = 5;
    public static final int PIECE_OFFSET_Y = 5;

    // 文本提示
    private Text gameOverBanner;
    private Rectangle gameOverDimmingRect;
    private TurnIndicator turnIndicator;
    private Font gameFont;

    // --- 状态变量 ---
    private boolean isCustomMode = false;
    private boolean isSettingUp = false;
    private boolean isLoadedGame = false;

    // 【新增】标记：是否正在重置排局（用于在 initGame 中区分）
    private boolean isRestartingCustom = false;
    // 【新增】排局快照：用于存储点击“开始对局”前的棋盘状态
    private ChessBoardModel customSetupSnapshot;

    private String selectedPieceType = null;
    private boolean selectedPieceIsRed = true;

    // UI 容器引用
    private VBox leftSetupPanel;
    private VBox rightSetupPanel;
    private VBox standardGameUI;
    private VBox turnSelectionPanel;

    private ChessBoardModel model;
    private boardController boardController;
    private InputHandler inputHandler;
    // 【新增】用户管理器
    private UserManager userManager;
    private String currentUser = "Guest";
    private boolean isGuestMode = true;
    //储存地址
    private static final String SAVE_DIR = "saves/";

    // --- Getters & Setters ---
    public Text getGameOverBanner() { return gameOverBanner; }
    public Rectangle getGameOverDimmingRect() { return gameOverDimmingRect; }
    public void setCustomMode(boolean customMode) { this.isCustomMode = customMode; }
    public void setLoadedGame(boolean loadedGame) { this.isLoadedGame = loadedGame; }
    public boolean isSettingUp() { return isSettingUp; }
    public String getSelectedPieceType() { return selectedPieceType; }
    public boolean isSelectedPieceRed() { return selectedPieceIsRed; }
    public ChessBoardModel getModel() { return model; }
    public TurnIndicator getTurnIndicator() { return turnIndicator; }
    public UserManager getUserManager() { return userManager; }
    public String getCurrentUser() { return currentUser; }
    public boolean isGuest() { return isGuestMode; }

    public void centerTextInApp(Text text) {
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        double centerX = (APP_WIDTH - textWidth) / 2;
        double centerY = (APP_HEIGHT - textHeight) / 2 + text.getFont().getSize() * 0.3;
        text.setTranslateX(centerX);
        text.setTranslateY(centerY);
    }

    // 【新增】登录方法，供 UI 调用
    public void login(String username) {
        this.currentUser = username;
        this.isGuestMode = false;
    }

    // 【新增】游客登录方法
    public void loginAsGuest() {
        this.currentUser = "Guest";
        this.isGuestMode = true;
    }

    public static Point2D getVisualPosition(int row, int col) {
        double centerX = BOARD_START_X + MARGIN + col * CELL_SIZE;
        double centerY = BOARD_START_Y + MARGIN + row * CELL_SIZE;

        centerX += PIECE_OFFSET_X;
        centerY += PIECE_OFFSET_Y;

        double pieceRadius = (CELL_SIZE - 8) / 2.0;
        double topLeftX = centerX - pieceRadius;
        double topLeftY = centerY - pieceRadius;
        return new Point2D(topLeftX, topLeftY);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("中国象棋 1.0");
        settings.setVersion("1.0");
        settings.setWidth(APP_WIDTH);
        settings.setHeight(APP_HEIGHT);
        settings.setMainMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override
            public FXGLMenu newMainMenu() { return new MainMenuScene(); }
            @Override
            public FXGLMenu newGameMenu() { return new InGameMenuScene(); }
        });
    }

    @Override
    protected void onPreInit() {
        try {
            gameFont = getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(20);
            userManager = new UserManager();
        }
        catch (Exception e) {
            System.out.println("字体加载失败，使用默认字体");
            gameFont = Font.font("System", FontWeight.BOLD, 20);
        }
    }

    // --- 核心初始化逻辑 ---
    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new XiangQiFactory());

        // 1. 读档模式
        if (isLoadedGame) {
            isLoadedGame = false;
            if (this.model == null) this.model = new ChessBoardModel();
            isSettingUp = false;
        }
        // 2. 【新增】排局重置模式：恢复快照
        else if (isRestartingCustom) {
            isRestartingCustom = false;
            // 从快照恢复 Model
            this.model = deepCopy(customSetupSnapshot);
            // 恢复后，要把状态设回“设置中”，这样 UI 就会显示选择面板
            isSettingUp = true;
            selectedPieceType = null;
        }
        // 3. 全新游戏
        else {
            this.model = new ChessBoardModel();

            if (isCustomMode) {
                // 排局：清空
                this.model.clearBoard();
                isSettingUp = true;
                selectedPieceType = null;
                customSetupSnapshot = null; // 清空旧快照
            } else {
                // 标准：使用默认棋盘
                isSettingUp = false;
            }
        }

        spawn("background", 0, 0);
        spawn("board", BOARD_START_X, BOARD_START_Y);

        this.boardController = new boardController(this.model);
        this.inputHandler = new InputHandler(this.boardController);

        // 如果不是在排局设置阶段，生成棋子
        // 注意：如果是 RestartingCustom，这里 isSettingUp 为 true，所以不会生成棋子
        // 而是等到 initSetupUI 显示后，通过 spawnPiecesFromModel 手动刷新或者用户点击刷新
        // 但为了让用户看到之前的残局，我们在这里还是生成一次比较好
        // 修正：排局设置阶段也需要显示棋子
        spawnPiecesFromModel();
    }

    public void spawnPiecesFromModel() {
        getGameWorld().getEntitiesByType(EntityType.PIECE).forEach(entity -> entity.removeFromWorld());
        for (AbstractPiece pieceLogic : model.getPieces()) {
            String colorPrefix = pieceLogic.isRed() ? "Red" : "Black";
            String pieceTypeName = pieceLogic.getClass().getSimpleName().replace("Piece", "");
            String entityID = colorPrefix + pieceTypeName;
            Point2D visualPos = getVisualPosition(pieceLogic.getRow(), pieceLogic.getCol());
            spawn(entityID, new SpawnData(visualPos).put("pieceLogic", pieceLogic));
        }
    }

    // --- UI 初始化 ---
    @Override
    protected void initUI() {
        gameOverDimmingRect = new Rectangle(APP_WIDTH, APP_HEIGHT, Color.web("000", 0.0));
        gameOverDimmingRect.setVisible(false);
        gameOverDimmingRect.setMouseTransparent(true);

        gameOverBanner = new Text();
        try {
            gameOverBanner.setFont(getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(80));
        } catch (Exception e) {
            gameOverBanner.setFont(Font.font(80));
        }
        gameOverBanner.setFill(Color.BROWN);
        gameOverBanner.setStroke(Color.BLACK);
        gameOverBanner.setStrokeWidth(3);
        gameOverBanner.setEffect(new DropShadow(15, Color.BLACK));
        gameOverBanner.setVisible(false);
        addUINode(gameOverDimmingRect);
        addUINode(gameOverBanner);

        if (isCustomMode) {
            // 如果是排局模式（无论是刚进还是玩了一半），根据 isSettingUp 决定显示哪个 UI
            if (isSettingUp) {
                initSetupUI();
            } else {
                initStandardGameUI();
            }
        } else {
            initStandardGameUI();
        }
    }

    private void initStandardGameUI() {
        double uiX = BOARD_START_X + BOARD_WIDTH + UI_GAP - 20;

        var btnUndo = new PixelatedButton("悔棋", "Button1", () -> { if (boardController != null) boardController.undo(); });
        var btnSurrender = new PixelatedButton("投降", "Button1", () -> { if (boardController != null) boardController.surrender(); });
        var btnSave = new PixelatedButton("保存游戏", "Button1", this::openSaveDialog);

        // --- 【新增】重新开始按钮 ---
        var btnRestart = new PixelatedButton("重新开始", "Button1", this::handleRestartGame);

        var btnHistory = new PixelatedButton("历史记录", "Button1", () -> getGameController().gotoGameMenu());

        // 将 restart 按钮加入布局
        standardGameUI = new VBox(10, btnUndo, btnSave, btnRestart, btnSurrender, btnHistory);

        addUINode(standardGameUI, uiX, 50);

        turnIndicator = new TurnIndicator();
        turnIndicator.update(model.isRedTurn(), false);
        addUINode(turnIndicator, uiX, 750);
    }

    // --- 【新增】处理重新开始逻辑 ---
    private void handleRestartGame() {
        getDialogService().showConfirmationBox("确定要重新开始吗？", yes -> {
            if (yes) {
                if (isCustomMode && customSetupSnapshot != null) {
                    // 排局模式：标记为重置自定义，并开始新游戏
                    isRestartingCustom = true;
                    getGameController().startNewGame();
                } else {
                    // 标准模式：普通重开
                    isCustomMode = false;
                    isLoadedGame = false;
                    getGameController().startNewGame();
                }
            }
        });
    }

    // --- 排局 UI 构建 ---
    private void initSetupUI() {
        double leftPanelX = BOARD_START_X - UI_GAP - UI_WIDTH;
        double safeLeftX = Math.max(20, leftPanelX);
        leftSetupPanel = createPiecePalette(true);
        leftSetupPanel.setTranslateX(safeLeftX);
        leftSetupPanel.setTranslateY(50);

        double rightPanelX = BOARD_START_X + BOARD_WIDTH + UI_GAP;
        rightSetupPanel = createPiecePalette(false);
        rightSetupPanel.setTranslateX(rightPanelX);
        rightSetupPanel.setTranslateY(50);

        // 左下角：保存排局 + 先手选择


        PixelatedButton btnEraser = new PixelatedButton("橡皮擦", "Button1", () -> {
            leftSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
            rightSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));

            this.selectedPieceType = "Eraser";
            this.selectedPieceIsRed = false;
            getDialogService().showMessageBox("已进入橡皮擦模式\n点击棋子即可删除");
        });
        btnEraser.setScaleX(0.6);
        btnEraser.setScaleY(0.6);

        ToggleGroup turnGroup = new ToggleGroup();
        ToggleButton rbRedFirst = createStyledToggleButton("红先", true);
        ToggleButton rbBlackFirst = createStyledToggleButton("黑先", false);
        rbRedFirst.setToggleGroup(turnGroup);
        rbBlackFirst.setToggleGroup(turnGroup);
        rbRedFirst.setSelected(true);

        Label turnLabel = new Label("先手选择");
        turnLabel.setFont(gameFont);
        turnLabel.setTextFill(Color.WHITE);
        turnLabel.setStyle("-fx-effect: dropshadow(one-pass-box, black, 2, 0, 1, 1);");

        turnSelectionPanel = new VBox(10, btnEraser, turnLabel, rbRedFirst, rbBlackFirst);
        turnSelectionPanel.setAlignment(Pos.CENTER_LEFT);
        turnSelectionPanel.setTranslateX(safeLeftX);
        turnSelectionPanel.setTranslateY(APP_HEIGHT - 320);

        // 右侧控制区
        PixelatedButton btnSaveSetup = new PixelatedButton("保存排局", "Button1", this::openSaveDialog);
        btnSaveSetup.setScaleX(0.6);
        btnSaveSetup.setScaleY(0.6);

        PixelatedButton btnStartCustom = new PixelatedButton("开始对局", "Button1", () -> {
            boolean isRedFirst = rbRedFirst.isSelected();
            tryStartCustomGame(isRedFirst);
        });
        btnStartCustom.setScaleX(0.6);
        btnStartCustom.setScaleY(0.6);


        Label hintLabel = new Label("操作提示:\n1.选择棋子放置\n2.点击同类棋子删除\n3.使用橡皮擦清除");
        try {
            hintLabel.setFont(getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(16));
        } catch (Exception e) {
            hintLabel.setFont(Font.font(16));
        }
        hintLabel.setStyle("-fx-text-fill: white; -fx-padding: 10; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 5;");
        hintLabel.setWrapText(true);
        hintLabel.setPrefWidth(UI_WIDTH);

        VBox controlBox = new VBox(-25, hintLabel, btnSaveSetup, btnStartCustom);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setStyle("-fx-padding: 30 0 0 0;");
        rightSetupPanel.getChildren().add(controlBox);

        addUINode(leftSetupPanel);
        addUINode(rightSetupPanel);
        addUINode(turnSelectionPanel);
    }

    private ToggleButton createStyledToggleButton(String text, boolean isRed) {
        ToggleButton tb = new ToggleButton(text);
        tb.setFont(gameFont);
        tb.setPrefWidth(UI_WIDTH * 0.8);
        String baseColor = "#D2B48C";
        String selectedColor = isRed ? "#FF6666" : "#666666";
        String commonStyle = "-fx-background-radius: 0; -fx-border-color: #5C3A1A; -fx-border-width: 2px; -fx-text-fill: black;";
        tb.setStyle("-fx-base: " + baseColor + "; " + commonStyle);

        tb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                tb.setStyle("-fx-base: " + selectedColor + "; " + commonStyle + "-fx-text-fill: white;");
            } else {
                tb.setStyle("-fx-base: " + baseColor + "; " + commonStyle);
            }
        });
        return tb;
    }

    private VBox createPiecePalette(boolean isRed) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(UI_WIDTH);

        String colorName = isRed ? "红方" : "黑方";
        Label title = new Label(colorName + "棋库");
        title.setFont(gameFont);
        title.setTextFill(isRed ? Color.web("#ff3333") : Color.web("#333333"));
        title.setStyle("-fx-padding: 5 15; -fx-background-color: rgba(255,255,255,0.7); -fx-background-radius: 10;");
        box.getChildren().add(title);

        String[] types = {"General", "Advisor", "Elephant", "Horse", "Chariot", "Cannon", "Soldier"};

        for (String type : types) {
            String colorPrefix = isRed ? "Red" : "Black";
            String textureName = colorPrefix + type + ".png";
            ImageView pieceImage;
            try {
                pieceImage = new ImageView(getAssetLoader().loadTexture(textureName).getImage());
                pieceImage.setFitWidth(55);
                pieceImage.setFitHeight(55);
                pieceImage.setPreserveRatio(true);
            } catch (Exception e) {
                System.err.println("无法加载棋子图片用于UI: " + textureName);
                pieceImage = null;
            }

            Button btn = new Button();
            btn.setPrefWidth(70);
            btn.setPrefHeight(70);

            if (pieceImage != null) {
                btn.setGraphic(pieceImage);
                btn.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 2; -fx-border-radius: 5;");
                btn.setOnAction(e -> {
                    leftSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
                    rightSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
                    btn.setStyle("-fx-background-color: rgba(255,255,0,0.3); -fx-border-color: yellow; -fx-border-width: 2; -fx-border-radius: 5;");
                    this.selectedPieceType = type;
                    this.selectedPieceIsRed = isRed;
                    FXGL.play("按钮音效1.mp3");
                });
            } else {
                btn.setText(type);
            }
            box.getChildren().add(btn);
        }
        return box;
    }

    private void tryStartCustomGame(boolean isRedFirst) {
        if (model.FindKing(true) == null || model.FindKing(false) == null) {
            getDialogService().showMessageBox("规则错误：\n红黑双方必须各有一只帅/将才能开始！");
            return;
        }

        // 【核心】保存当前排局的快照，以便重新开始时恢复
        this.customSetupSnapshot = deepCopy(model);

        removeUINode(leftSetupPanel);
        removeUINode(rightSetupPanel);
        removeUINode(turnSelectionPanel);

        model.setRedTurn(isRedFirst);
        this.isSettingUp = false;
        this.selectedPieceType = null;

        initStandardGameUI();

        turnIndicator.update(isRedFirst, false);
        getDialogService().showMessageBox("排局开始！\n由 " + (isRedFirst ? "红方" : "黑方") + " 先行。");
    }

    // --- 【新增】深拷贝工具方法 ---
    private ChessBoardModel deepCopy(ChessBoardModel original) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(original);
            oos.flush();
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            return (ChessBoardModel) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- 存档功能 ---
    private void saveGameToSlot(int slotIndex) {
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = SAVE_DIR + currentUser + "_save_" + slotIndex + ".dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(model);
            getDialogService().showMessageBox("成功保存到：存档 " + slotIndex);
        }
        catch (IOException e) {
            e.printStackTrace();
            getDialogService().showMessageBox("存档失败：" + e.getMessage());
        }
    }

    public void openSaveDialog() {
        // 【新增】游客限制
        if (isGuestMode) {
            getDialogService().showMessageBox("游客模式无法保存存档。\n请注册/登录账号后使用。");
            return;
        }

        getDialogService().showChoiceBox("请选择保存位置",
                java.util.Arrays.asList("存档 1", "存档 2", "存档 3"),
                selected -> {
                    int slot = Integer.parseInt(selected.replace("存档 ", ""));
                    saveGameToSlot(slot);
                }
        );
    }

    // --- 读档功能 ---
    private void loadGameFromSlot(int slotIndex) {
        String filename = SAVE_DIR + currentUser + "_save_" + slotIndex + ".dat";
        File file = new File(filename);
        if (!file.exists()) {
            getDialogService().showMessageBox("错误：存档 " + slotIndex + " 不存在！");
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ChessBoardModel loadedModel = (ChessBoardModel) ois.readObject();
            loadedModel.rebuildAfterLoad();
            this.model = loadedModel;
            this.isCustomMode = false;
            this.isLoadedGame = true;
            getGameController().startNewGame();
        } catch (Exception e) {
            e.printStackTrace();
            getDialogService().showMessageBox("读取失败：" + e.getMessage());
        }
    }

    public void openLoadDialog() {
        // 【新增】游客限制
        if (isGuestMode) {
            getDialogService().showMessageBox("游客模式无法读取存档。");
            return;
        }

        // 只检查当前用户的存档
        List<String> availableSlots = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String path = SAVE_DIR + currentUser + "_save_" + i + ".dat";
            if (new File(path).exists()) {
                availableSlots.add("存档 " + i);
            }
        }

        if (availableSlots.isEmpty()) {
            getDialogService().showMessageBox("没有找到 " + currentUser + " 的任何存档记录。");
            return;
        }

        getDialogService().showChoiceBox("请选择读取位置", availableSlots, selected -> {
            int slot = Integer.parseInt(selected.replace("存档 ", ""));
            loadGameFromSlot(slot);
        });
    }

    public boolean hasSaveFile() {
        if (isGuestMode) return false;
        return new File(SAVE_DIR + currentUser + "_save_1.dat").exists() ||
                new File(SAVE_DIR + currentUser + "_save_2.dat").exists() ||
                new File(SAVE_DIR + currentUser + "_save_3.dat").exists();
    }

    @Override
    protected void initInput() {
        Input input = getInput();
        UserAction clickAction = new UserAction("Click") {
            @Override
            protected void onActionEnd() {
                if (inputHandler != null) {
                    inputHandler.handleMouseClick(input.getMousePositionWorld());
                }
            }
        };
        input.addAction(clickAction, MouseButton.PRIMARY);
    }

    public static void main(String[] args) { launch(args); }
}