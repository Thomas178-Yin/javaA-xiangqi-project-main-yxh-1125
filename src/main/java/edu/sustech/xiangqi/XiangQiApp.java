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
import javafx.util.Duration;

import static com.almasb.fxgl.dsl.FXGL.*;

public class XiangQiApp extends GameApplication {

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
    public static final int PIECE_OFFSET_X = 5;
    public static final int PIECE_OFFSET_Y = 5;

    private Text gameOverBanner;
    private Rectangle gameOverDimmingRect;
    private TurnIndicator turnIndicator;
    private Font gameFont;

    private boolean isCustomMode = false;
    private boolean isSettingUp = false;
    private boolean isLoadedGame = false;
    private boolean isRestartingCustom = false;
    private boolean isOnlineLaunch = false;
    private boolean isAIEnabled = false; // AI 开关

    private ChessBoardModel customSetupSnapshot;
    private String selectedPieceType = null;
    private boolean selectedPieceIsRed = true;

    // UI 引用
    private VBox leftSetupPanel;
    private VBox rightSetupPanel;
    private VBox standardGameUI;
    private VBox leftGameUI; // 【新增】左侧标准 UI (AI面板)
    private VBox turnSelectionPanel;

    private ChessBoardModel model;
    private boardController boardController;
    private InputHandler inputHandler;
    private UserManager userManager;
    private String currentUser = "Guest";
    private boolean isGuestMode = true;
    private static final String SAVE_DIR = "saves/";

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
    public boolean isAIEnabled() { return isAIEnabled; }

    public void login(String username) { this.currentUser = username; this.isGuestMode = false; }
    public void loginAsGuest() { this.currentUser = "Guest"; this.isGuestMode = true; }

    public void centerTextInApp(Text text) {
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        text.setTranslateX((APP_WIDTH - textWidth) / 2);
        text.setTranslateY((APP_HEIGHT - textHeight) / 2 + text.getFont().getSize() * 0.3);
    }

    public static Point2D getVisualPosition(int row, int col) {
        double centerX = BOARD_START_X + MARGIN + col * CELL_SIZE + PIECE_OFFSET_X;
        double centerY = BOARD_START_Y + MARGIN + row * CELL_SIZE + PIECE_OFFSET_Y;
        double pieceRadius = (CELL_SIZE - 8) / 2.0;
        return new Point2D(centerX - pieceRadius, centerY - pieceRadius);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("中国象棋 1.0");
        settings.setVersion("1.0");
        settings.setWidth(APP_WIDTH);
        settings.setHeight(APP_HEIGHT);
        settings.setMainMenuEnabled(true);
        settings.setSceneFactory(new SceneFactory() {
            @Override public FXGLMenu newMainMenu() { return new MainMenuScene(); }
            @Override public FXGLMenu newGameMenu() { return new InGameMenuScene(); }
        });
    }

    @Override
    protected void onPreInit() {
        try {
            gameFont = getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(20);
            userManager = new UserManager();
        } catch (Exception e) {
            gameFont = Font.font("System", FontWeight.BOLD, 20);
        }
    }

    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new XiangQiFactory());

        if (isLoadedGame) {
            isLoadedGame = false;
            if (this.model == null) this.model = new ChessBoardModel();
            isSettingUp = false;
        } else if (isRestartingCustom) {
            isRestartingCustom = false;
            this.model = deepCopy(customSetupSnapshot);
            isSettingUp = true;
            selectedPieceType = null;
        } else if (isOnlineLaunch) {
            this.model = new ChessBoardModel();
            isCustomMode = false; isLoadedGame = false; isSettingUp = false;
        } else {
            this.model = new ChessBoardModel();
            if (isCustomMode) {
                this.model.clearBoard();
                isSettingUp = true;
                selectedPieceType = null;
                customSetupSnapshot = null;
            } else {
                isSettingUp = false;
            }
        }

        spawn("background", 0, 0);
        spawn("board", BOARD_START_X, BOARD_START_Y);

        this.boardController = new boardController(this.model);
        this.inputHandler = new InputHandler(this.boardController);

        spawnPiecesFromModel();
    }

    public void spawnPiecesFromModel() {
        getGameWorld().getEntitiesByType(EntityType.PIECE).forEach(entity -> entity.removeFromWorld());
        for (AbstractPiece pieceLogic : model.getPieces()) {
            String prefix = pieceLogic.isRed() ? "Red" : "Black";
            String type = pieceLogic.getClass().getSimpleName().replace("Piece", "");
            Point2D pos = getVisualPosition(pieceLogic.getRow(), pieceLogic.getCol());
            spawn(prefix + type, new SpawnData(pos).put("pieceLogic", pieceLogic));
        }
    }

    public void startOnlineConnection(String ip, String roomId, Text statusText) {
        this.isOnlineLaunch = true;
        this.setCustomMode(false);
        this.setLoadedGame(false);
        getGameController().startNewGame();
        runOnce(() -> {
            if (boardController != null) boardController.connectToRoom(ip, roomId);
        }, Duration.seconds(0.1));
    }

    @Override
    protected void initUI() {
        // 清理旧 UI (特别是左侧)
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
            if (isSettingUp) initSetupUI();
            else initStandardGameUI();
        } else {
            initStandardGameUI();
        }
    }

    /**
     * 【新增】标准模式 UI (含 AI 面板)
     */
    private void initStandardGameUI() {
        // 右侧
        double rightX = BOARD_START_X + BOARD_WIDTH + UI_GAP - 20;
        var btnUndo = new PixelatedButton("悔棋", "Button1", () -> { if (boardController != null) boardController.undo(); });
        var btnSurrender = new PixelatedButton("投降", "Button1", () -> { if (boardController != null) boardController.surrender(); });
        var btnSave = new PixelatedButton("保存游戏", "Button1", this::openSaveDialog);
        var btnRestart = new PixelatedButton("重新开始", "Button1", this::handleRestartGame);
        var btnHistory = new PixelatedButton("历史记录", "Button1", () -> getGameController().gotoGameMenu());
        standardGameUI = new VBox(10, btnUndo, btnSave, btnRestart, btnSurrender, btnHistory);
        addUINode(standardGameUI, rightX, 50);

        // 左侧 (AI)
        double leftX = BOARD_START_X - UI_GAP - UI_WIDTH;
        double safeLeftX = Math.max(20, leftX);

        var btnToggleAI = new PixelatedButton("AI: 关闭", "Button1", null);
        btnToggleAI.setOnMouseClicked(e -> {
            isAIEnabled = !isAIEnabled;
            ((Text)btnToggleAI.getChildren().get(1)).setText(isAIEnabled ? "AI: 开启" : "AI: 关闭");
            if (isAIEnabled && !model.isRedTurn() && boardController != null) boardController.startAITurn();
        });

        var btnHint = new PixelatedButton("AI 提示", "Button1", () -> {
            if (boardController != null) boardController.requestAIHint();
        });

        leftGameUI = new VBox(10, btnToggleAI, btnHint);
        addUINode(leftGameUI, safeLeftX, 50);

        turnIndicator = new TurnIndicator();
        turnIndicator.update(model.isRedTurn(), false);
        addUINode(turnIndicator, rightX, 750);
    }

    /**
     * 【新增】联机模式 UI
     */
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
    }

    private void handleRestartGame() {
        getDialogService().showConfirmationBox("确定要重新开始吗？", yes -> {
            if (yes) {
                if (isCustomMode && customSetupSnapshot != null) {
                    isRestartingCustom = true;
                    getGameController().startNewGame();
                } else {
                    isCustomMode = false; isLoadedGame = false;
                    getGameController().startNewGame();
                }
            }
        });
    }

    private void initSetupUI() {
        double leftX = Math.max(20, BOARD_START_X - UI_GAP - UI_WIDTH);
        leftSetupPanel = createPiecePalette(true);
        leftSetupPanel.setTranslateX(leftX); leftSetupPanel.setTranslateY(50);

        double rightX = BOARD_START_X + BOARD_WIDTH + UI_GAP;
        rightSetupPanel = createPiecePalette(false);
        rightSetupPanel.setTranslateX(rightX); rightSetupPanel.setTranslateY(50);

        // 橡皮擦
        PixelatedButton btnEraser = new PixelatedButton("橡皮擦", "Button1", () -> {
            resetPaletteStyles();
            this.selectedPieceType = "Eraser"; this.selectedPieceIsRed = false;
            getDialogService().showMessageBox("橡皮擦模式：点击棋子删除");
        });
        btnEraser.setScaleX(0.9); btnEraser.setScaleY(0.9);

        // 先手选择
        ToggleGroup turnGroup = new ToggleGroup();
        ToggleButton rbRedFirst = createStyledToggleButton("红先", true);
        ToggleButton rbBlackFirst = createStyledToggleButton("黑先", false);
        rbRedFirst.setToggleGroup(turnGroup); rbBlackFirst.setToggleGroup(turnGroup); rbRedFirst.setSelected(true);

        Label turnLabel = new Label("先手选择");
        turnLabel.setFont(gameFont); turnLabel.setTextFill(Color.WHITE);

        // 保存排局
        PixelatedButton btnSaveSetup = new PixelatedButton("保存排局", "Button1", this::openSaveDialog);
        btnSaveSetup.setScaleX(0.9); btnSaveSetup.setScaleY(0.9);

        turnSelectionPanel = new VBox(10, btnSaveSetup, btnEraser, turnLabel, rbRedFirst, rbBlackFirst);
        turnSelectionPanel.setAlignment(Pos.CENTER_LEFT);
        turnSelectionPanel.setTranslateX(leftX);
        turnSelectionPanel.setTranslateY(APP_HEIGHT - 350); // 上移一点

        // 开始按钮
        PixelatedButton btnStartCustom = new PixelatedButton("开始对局", "Button1", () -> {
            boolean isRedFirst = rbRedFirst.isSelected();
            tryStartCustomGame(isRedFirst);
        });

        Label hintLabel = new Label("提示：\n左红右黑\n点击放置");
        hintLabel.setFont(gameFont); hintLabel.setTextFill(Color.WHITE);

        VBox controlBox = new VBox(15, hintLabel, btnStartCustom);
        controlBox.setAlignment(Pos.CENTER);
        controlBox.setStyle("-fx-padding: 30 0 0 0;");
        rightSetupPanel.getChildren().add(controlBox);

        addUINode(leftSetupPanel); addUINode(rightSetupPanel); addUINode(turnSelectionPanel);
    }

    private void tryStartCustomGame(boolean isRedFirst) {
        // 飞将校验
        AbstractPiece redKing = model.FindKing(true);
        AbstractPiece blackKing = model.FindKing(false);
        if (redKing == null || blackKing == null) {
            getDialogService().showMessageBox("红黑双方必须各有一将！"); return;
        }
        if (redKing.getCol() == blackKing.getCol()) {
            boolean blocked = false;
            for(int r = Math.min(redKing.getRow(), blackKing.getRow()) + 1; r < Math.max(redKing.getRow(), blackKing.getRow()); r++) {
                if (model.getPieceAt(r, redKing.getCol()) != null) { blocked = true; break; }
            }
            if (!blocked) { getDialogService().showMessageBox("将帅不能照面（飞将）！"); return; }
        }

        this.customSetupSnapshot = deepCopy(model);
        removeUINode(leftSetupPanel); removeUINode(rightSetupPanel); removeUINode(turnSelectionPanel);

        model.setRedTurn(isRedFirst);
        this.isSettingUp = false;
        this.selectedPieceType = null;

        initStandardGameUI();
        turnIndicator.update(isRedFirst, false);

        getDialogService().showMessageBox("排局开始！", () -> {
            if (!isRedFirst && isAIEnabled && boardController != null) {
                boardController.startAITurn();
            }
        });
    }

    // --- 辅助方法 ---
    private ToggleButton createStyledToggleButton(String text, boolean isRed) {
        ToggleButton tb = new ToggleButton(text);
        tb.setFont(gameFont); tb.setPrefWidth(UI_WIDTH * 0.8);
        String base = "#D2B48C"; String sel = isRed ? "#FF6666" : "#666666";
        String style = "-fx-background-radius: 0; -fx-border-color: #5C3A1A; -fx-border-width: 2px; -fx-text-fill: black;";
        tb.setStyle("-fx-base: " + base + ";" + style);
        tb.selectedProperty().addListener((o, old, val) -> tb.setStyle("-fx-base: " + (val ? sel : base) + ";" + style));
        return tb;
    }

    private VBox createPiecePalette(boolean isRed) {
        VBox box = new VBox(10); box.setAlignment(Pos.TOP_CENTER); box.setPrefWidth(UI_WIDTH);
        Label title = new Label(isRed?"红方":"黑方"); title.setFont(gameFont); title.setTextFill(isRed?Color.RED:Color.BLACK);
        box.getChildren().add(title);
        String[] types = {"General", "Advisor", "Elephant", "Horse", "Chariot", "Cannon", "Soldier"};
        for (String type : types) {
            String prefix = isRed ? "Red" : "Black";
            ImageView img = new ImageView(FXGL.getAssetLoader().loadTexture(prefix + type + ".png").getImage());
            img.setFitWidth(55); img.setPreserveRatio(true);
            Button btn = new Button("", img);
            btn.setStyle("-fx-background-color: transparent;");
            btn.setOnAction(e -> {
                resetPaletteStyles();
                btn.setStyle("-fx-background-color: rgba(255,255,0,0.3);");
                this.selectedPieceType = type; this.selectedPieceIsRed = isRed;
                FXGL.play("按钮音效1.mp3");
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
    private void saveGameToSlot(int slot) {
        new File(SAVE_DIR).mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_DIR + currentUser + "_save_" + slot + ".dat"))) {
            oos.writeObject(model); getDialogService().showMessageBox("保存成功");
        } catch (Exception e) {}
    }
    public void openSaveDialog() {
        if (isGuestMode) { getDialogService().showMessageBox("游客无法存档"); return; }
        getDialogService().showChoiceBox("选择位置", List.of("存档 1", "存档 2", "存档 3"), s -> saveGameToSlot(Integer.parseInt(s.split(" ")[1])));
    }
    private void loadGameFromSlot(int slot) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_DIR + currentUser + "_save_" + slot + ".dat"))) {
            ChessBoardModel m = (ChessBoardModel) ois.readObject(); m.rebuildAfterLoad();
            this.model = m; this.isCustomMode = false; this.isLoadedGame = true; getGameController().startNewGame();
        } catch (Exception e) { getDialogService().showMessageBox("读取失败"); }
    }
    public void openLoadDialog() {
        if (isGuestMode) { getDialogService().showMessageBox("游客无法读档"); return; }
        List<String> slots = new ArrayList<>();
        for (int i=1; i<=3; i++) if (new File(SAVE_DIR + currentUser + "_save_" + i + ".dat").exists()) slots.add("存档 " + i);
        if (slots.isEmpty()) { getDialogService().showMessageBox("无存档"); return; }
        getDialogService().showChoiceBox("读取位置", slots, s -> loadGameFromSlot(Integer.parseInt(s.split(" ")[1])));
    }
    public boolean hasSaveFile() {
        if (isGuestMode) return false;
        for (int i=1; i<=3; i++) if (new File(SAVE_DIR + currentUser + "_save_" + i + ".dat").exists()) return true;
        return false;
    }

    @Override protected void initInput() {
        getInput().addAction(new UserAction("Click") {
            @Override protected void onActionEnd() { if (inputHandler != null) inputHandler.handleMouseClick(getInput().getMousePositionWorld()); }
        }, MouseButton.PRIMARY);
    }

    public static void main(String[] args) {
        new Thread(() -> { try { edu.sustech.xiangqi.server.XiangQiServer.main(null); } catch (Exception e) {} }).start();
        launch(args);
    }
}