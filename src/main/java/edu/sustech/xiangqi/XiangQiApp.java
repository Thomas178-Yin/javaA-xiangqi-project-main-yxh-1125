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
    // 增加高度以实现垂直居中
    public static final int APP_HEIGHT = BOARD_HEIGHT + 120;

    // 棋盘居中坐标
    public static final double BOARD_START_X = (APP_WIDTH - BOARD_WIDTH) / 2.0;
    public static final double BOARD_START_Y = (APP_HEIGHT - BOARD_HEIGHT) / 2.0;

    // 文本提示
    private Text gameOverBanner;
    private Rectangle gameOverDimmingRect;
    private TurnIndicator turnIndicator;
    private Font gameFont;

    // --- 状态变量 ---
    private boolean isCustomMode = false;
    private boolean isSettingUp = false;

    // 【关键】标记：当前是否是通过“读取存档”进入的游戏
    private boolean isLoadedGame = false;

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

    // --- Getters & Setters ---
    public Text getGameOverBanner() { return gameOverBanner; }
    public Rectangle getGameOverDimmingRect() { return gameOverDimmingRect; }

    public void setCustomMode(boolean customMode) { this.isCustomMode = customMode; }

    // 【关键】这就是报错缺少的那个方法
    public void setLoadedGame(boolean loadedGame) { this.isLoadedGame = loadedGame; }

    public boolean isSettingUp() { return isSettingUp; }
    public String getSelectedPieceType() { return selectedPieceType; }
    public boolean isSelectedPieceRed() { return selectedPieceIsRed; }
    public ChessBoardModel getModel() { return model; }
    public TurnIndicator getTurnIndicator() { return turnIndicator; }

    public void centerTextInApp(Text text) {
        double textWidth = text.getLayoutBounds().getWidth();
        double textHeight = text.getLayoutBounds().getHeight();
        double centerX = (APP_WIDTH - textWidth) / 2;
        double centerY = (APP_HEIGHT - textHeight) / 2 + text.getFont().getSize() * 0.3;
        text.setTranslateX(centerX);
        text.setTranslateY(centerY);
    }

    public static Point2D getVisualPosition(int row, int col) {
        double centerX = BOARD_START_X + MARGIN + col * CELL_SIZE;
        double centerY = BOARD_START_Y + MARGIN + row * CELL_SIZE;
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
        } catch (Exception e) {
            System.out.println("字体加载失败，使用默认字体");
            gameFont = Font.font("System", FontWeight.BOLD, 20);
        }
    }

    // --- 核心初始化逻辑 ---
    @Override
    protected void initGame() {
        getGameWorld().addEntityFactory(new XiangQiFactory());

        if (isLoadedGame) {
            // 1. 如果是读档：使用已加载的 Model
            isLoadedGame = false; // 重置标记
            if (this.model == null) {
                this.model = new ChessBoardModel(); // 防御性编程
            }
            isSettingUp = false;
        } else {
            // 2. 如果是新游戏（标准或排局）：强制 NEW 一个新的 Model
            this.model = new ChessBoardModel();

            if (isCustomMode) {
                // 排局：清空
                this.model.clearBoard();
                isSettingUp = true;
                selectedPieceType = null;
            } else {
                // 标准：使用默认棋盘
                isSettingUp = false;
            }
        }

        spawn("background", 0, 0);
        spawn("board", BOARD_START_X, BOARD_START_Y);

        this.boardController = new boardController(this.model);
        this.inputHandler = new InputHandler(this.boardController);

        // 如果不是在排局设置阶段（即标准开局或读档完毕），生成棋子实体
        if (!isSettingUp) {
            spawnPiecesFromModel();
        }
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
            initSetupUI();
        } else {
            initStandardGameUI();
        }
    }

    private void initStandardGameUI() {
        double uiX = BOARD_START_X + BOARD_WIDTH + UI_GAP;

        var btnUndo = new PixelatedButton("悔棋", "Button1", () -> { if (boardController != null) boardController.undo(); });
        var btnSurrender = new PixelatedButton("投降", "Button1", () -> { if (boardController != null) boardController.surrender(); });
        var btnSave = new PixelatedButton("保存游戏", "Button1", this::openSaveDialog);
        var btnHistory = new PixelatedButton("历史记录", "Button1", () -> getGameController().gotoGameMenu());

        standardGameUI = new VBox(10, btnUndo, btnSave, btnSurrender, btnHistory);
        // standardGameUI.setPrefWidth(UI_WIDTH); // 让 StackPane 按钮决定宽度

        addUINode(standardGameUI, uiX, 50);

        turnIndicator = new TurnIndicator();
        turnIndicator.update(model.isRedTurn(), false);
        addUINode(turnIndicator, uiX, 750);
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

        // 先手选择
        ToggleGroup turnGroup = new ToggleGroup();
        ToggleButton rbRedFirst = createStyledToggleButton("红先", true);
        ToggleButton rbBlackFirst = createStyledToggleButton("黑先", false);
        rbRedFirst.setToggleGroup(turnGroup);
        rbBlackFirst.setToggleGroup(turnGroup);
        rbRedFirst.setSelected(true);

        Label turnLabel = new Label("先手选择");
        turnLabel.setFont(gameFont);
        turnLabel.setTextFill(Color.WHITE);

        turnSelectionPanel = new VBox(10, turnLabel, rbRedFirst, rbBlackFirst);
        turnSelectionPanel.setAlignment(Pos.CENTER_LEFT);
        turnSelectionPanel.setTranslateX(safeLeftX);
        turnSelectionPanel.setTranslateY(APP_HEIGHT - 200);

        // --- 【新增】橡皮擦按钮 ---
        Button btnEraser = new Button("清除");
        btnEraser.setFont(gameFont);
        // 使用醒目的橙色或灰色
        btnEraser.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnEraser.setPrefWidth(UI_WIDTH); // 占满宽度
        btnEraser.setOnAction(e -> {
            // 清除左右两侧棋子选择的样式
            leftSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
            rightSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));

            // 设置选中状态为橡皮擦
            this.selectedPieceType = "Eraser";
            this.selectedPieceIsRed = false; // 颜色不重要

            // 给自己加个高亮边框表示选中
            btnEraser.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5; -fx-border-color: yellow; -fx-border-width: 3;");
            FXGL.play("按钮音效1.mp3");
        });

        // 开始按钮
        Button btnStartCustom = new Button("开始对局");
        btnStartCustom.setFont(gameFont);
        btnStartCustom.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnStartCustom.setOnAction(e -> {
            boolean isRedFirst = rbRedFirst.isSelected();
            tryStartCustomGame(isRedFirst);
        });

        Label hintLabel = new Label("操作提示:\n1.点击两侧棋子选中\n2.点击棋盘放置\n3.点击已放棋子移除");
        try {
            hintLabel.setFont(getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(16));
        } catch (Exception e) {
            hintLabel.setFont(Font.font(16));
        }
        hintLabel.setStyle("-fx-text-fill: white; -fx-padding: 10; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 5;");
        hintLabel.setWrapText(true);
        hintLabel.setPrefWidth(UI_WIDTH);

        VBox controlBox = new VBox(20, hintLabel, btnEraser, btnStartCustom);
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
        String baseColor = isRed ? "#ffcccc" : "#cccccc";
        String selectedColor = isRed ? "#ff9999" : "#999999";
        tb.setStyle("-fx-base: " + baseColor + "; -fx-background-radius: 5;");
        tb.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                tb.setStyle("-fx-base: " + selectedColor + "; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);");
            } else {
                tb.setStyle("-fx-base: " + baseColor + "; -fx-background-radius: 5;");
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
                    // 重置左右面板所有按钮样式
                    leftSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));
                    rightSetupPanel.getChildren().stream().filter(n -> n instanceof Button).forEach(n -> n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;"));

                    // 高亮当前按钮
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

    // --- 存档功能 ---

    private void saveGameToSlot(int slotIndex) {
        String filename = "savegame_" + slotIndex + ".dat";
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(model);
            getDialogService().showMessageBox("成功保存到：存档 " + slotIndex);
        } catch (IOException e) {
            e.printStackTrace();
            getDialogService().showMessageBox("存档失败：" + e.getMessage());
        }
    }

    public void openSaveDialog() {
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
        String filename = "savegame_" + slotIndex + ".dat";
        File file = new File(filename);

        if (!file.exists()) {
            getDialogService().showMessageBox("错误：存档 " + slotIndex + " 不存在！");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ChessBoardModel loadedModel = (ChessBoardModel) ois.readObject();
            loadedModel.rebuildAfterLoad(); // 修复 transient 数据

            this.model = loadedModel;
            this.isCustomMode = false;

            // 【关键】标记这是加载的游戏，告诉 initGame 不要覆盖
            this.isLoadedGame = true;

            getGameController().startNewGame();

        } catch (Exception e) {
            e.printStackTrace();
            getDialogService().showMessageBox("读取失败：" + e.getMessage());
        }
    }

    public void openLoadDialog() {
        List<String> availableSlots = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            if (new File("savegame_" + i + ".dat").exists()) {
                availableSlots.add("存档 " + i);
            }
        }

        if (availableSlots.isEmpty()) {
            getDialogService().showMessageBox("没有找到任何存档记录。");
            return;
        }

        getDialogService().showChoiceBox("请选择读取位置", availableSlots, selected -> {
            int slot = Integer.parseInt(selected.replace("存档 ", ""));
            loadGameFromSlot(slot);
        });
    }

    // 供主菜单检查是否有存档
    public boolean hasSaveFile() {
        return new File("savegame_1.dat").exists() ||
                new File("savegame_2.dat").exists() ||
                new File("savegame_3.dat").exists();
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