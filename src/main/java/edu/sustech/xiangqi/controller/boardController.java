package edu.sustech.xiangqi.controller;

import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.time.TimerAction;
import edu.sustech.xiangqi.EntityType;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.ai.AIService;
import edu.sustech.xiangqi.model.*;
import edu.sustech.xiangqi.net.NetworkClient;
import edu.sustech.xiangqi.view.PieceComponent;
import edu.sustech.xiangqi.view.VisualStateComponent;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.scene.paint.Color;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;
import static edu.sustech.xiangqi.XiangQiApp.CELL_SIZE;

public class boardController {

    private ChessBoardModel model;
    private Entity selectedEntity = null;
    private final AIService aiService = new AIService();
    private NetworkClient netClient;
    private boolean isOnlineMode = false;
    private boolean amIRed = true;
    private TimerAction aiAutoStartTimer = null;

    // 高亮实体列表
    private List<Entity> highlightEntities = new ArrayList<>();

    public boardController(ChessBoardModel model) {
        this.model = model;
    }

    // --- 【辅助】获取强转后的 App 实例 ---
    private XiangQiApp getApp() {
        return (XiangQiApp) FXGL.getApp();
    }

    // =========================================================
    //                 网络 / 联机逻辑
    // =========================================================

    public void connectToRoom(String ip, String roomId) {
        isOnlineMode = true;
        netClient = new NetworkClient();
        netClient.setOnMessage(this::onNetworkMessage);

        getApp().getInputHandler().setLocked(true);
        getDialogService().showMessageBox("正在连接服务器 (房间 " + roomId + ")...");

        new Thread(() -> {
            try {
                netClient.connect(ip, 9999, roomId);
            } catch (Exception e) {
                Platform.runLater(() -> getDialogService().showMessageBox("连接失败: " + e.getMessage()));
            }
        }).start();
    }

    private void onNetworkMessage(String msg) {
        Platform.runLater(() -> {
            System.out.println("[网络消息] " + msg);

            if (msg.startsWith("START")) {
                if (msg.contains("RED")) {
                    amIRed = true;
                    getDialogService().showMessageBox("匹配成功！你是红方 (先手)");
                } else {
                    amIRed = false;
                    getDialogService().showMessageBox("匹配成功！你是黑方 (后手)");
                }
                syncInputLock();
            }
            else if (msg.startsWith("MOVE")) {
                String[] parts = msg.split(" ");
                int r1 = Integer.parseInt(parts[1]);
                int c1 = Integer.parseInt(parts[2]);
                int r2 = Integer.parseInt(parts[3]);
                int c2 = Integer.parseInt(parts[4]);

                AbstractPiece piece = model.getPieceAt(r1, c1);
                if (piece != null) {
                    executeMove(piece, r2, c2, true);
                    syncInputLock();
                }
            }
            else if (msg.equals("SURRENDER")) {
                model.endGame(amIRed ? "红方" : "黑方");
                showGameOverBanner();
            }
            // --- 协议处理 ---
            else if (msg.equals("UNDO_REQUEST")) {
                getDialogService().showConfirmationBox("对方请求悔棋，是否同意？", yes -> {
                    netClient.sendRaw(yes ? "UNDO_AGREE" : "UNDO_REFUSE");
                    if (yes) doUndo();
                });
            }
            else if (msg.equals("UNDO_AGREE")) {
                getDialogService().showMessageBox("对方同意悔棋。");
                doUndo();
            }
            else if (msg.equals("UNDO_REFUSE")) {
                getDialogService().showMessageBox("对方拒绝悔棋。");
            }
            else if (msg.equals("RESTART_REQUEST")) {
                getDialogService().showConfirmationBox("对方请求重新开始，是否同意？", yes -> {
                    netClient.sendRaw(yes ? "RESTART_AGREE" : "RESTART_REFUSE");
                    if (yes) doRestart();
                });
            }
            else if (msg.equals("RESTART_AGREE")) {
                getDialogService().showMessageBox("对方同意重新开始。");
                doRestart();
            }
            else if (msg.equals("RESTART_REFUSE")) {
                getDialogService().showMessageBox("对方拒绝重新开始。");
            }
            else if (msg.equals("SWAP_REQUEST")) {
                getDialogService().showConfirmationBox("对方请求交换先手并重开，是否同意？", yes -> {
                    netClient.sendRaw(yes ? "SWAP_AGREE" : "SWAP_REFUSE");
                    if (yes) doSwapAndRestart();
                });
            }
            else if (msg.equals("SWAP_AGREE")) {
                getDialogService().showMessageBox("对方同意交换先手。");
                doSwapAndRestart();
            }
            else if (msg.equals("SWAP_REFUSE")) {
                getDialogService().showMessageBox("对方拒绝交换先手。");
            }
        });
    }

    // --- 本地执行动作 ---

    private void syncInputLock() {
        if (!isOnlineMode) return;
        if (model.isRedTurn() == amIRed) {
            getApp().getInputHandler().setLocked(false);
        } else {
            getApp().getInputHandler().setLocked(true);
        }
    }

    private void doUndo() {
        if (model.undoMove()) {
            refreshBoardView();
            syncInputLock();
        }
    }

    private void doRestart() {
        model.reset();
        refreshBoardView();

        XiangQiApp app = getApp();
        app.getGameOverDimmingRect().setVisible(false);
        app.getGameOverBanner().setVisible(false);

        syncInputLock();
    }

    private void doSwapAndRestart() {
        amIRed = !amIRed;
        doRestart();
        String role = amIRed ? "红方 (先手)" : "黑方 (后手)";
        getDialogService().showMessageBox("身份已交换，现在你是：" + role);
    }

    private void refreshBoardView() {
        XiangQiApp app = getApp();
        app.spawnPiecesFromModel();
        updateTurnIndicator();
        app.updateHistoryPanel();
        deselectPiece();

        // 清除高亮
        for (Entity e : highlightEntities) e.removeFromWorld();
        highlightEntities.clear();
    }

    // --- UI 按钮调用 (网络请求) ---

    public void surrenderOnline() {
        if (!isOnlineMode) return;
        getDialogService().showConfirmationBox("确定要投降吗？", yes -> {
            if (yes) {
                netClient.sendRaw("SURRENDER");
                model.endGame(!amIRed ? "红方" : "黑方");
                showGameOverBanner();
            }
        });
    }
    public void undoOnline() { if (isOnlineMode) { netClient.sendRaw("UNDO_REQUEST"); getDialogService().showMessageBox("请求已发送..."); } }
    public void restartOnline() { if (isOnlineMode) { netClient.sendRaw("RESTART_REQUEST"); getDialogService().showMessageBox("请求已发送..."); } }
    public void swapOnline() { if (isOnlineMode) { netClient.sendRaw("SWAP_REQUEST"); getDialogService().showMessageBox("请求已发送..."); } }

    // --- 【重要修复】本地/标准模式必须的接口 ---

    public void surrender() {
        if (isOnlineMode) {
            surrenderOnline();
            return;
        }
        if (model.isGameOver()) return;

        String winner = model.isRedTurn() ? "黑方" : "红方";
        model.endGame(winner);
        showGameOverBanner();
    }

    public void updateTurnIndicator() {
        if (getApp().getTurnIndicator() != null)
            getApp().getTurnIndicator().update(model.isRedTurn(), model.isGameOver());
    }

    public void undo() {
        if (isOnlineMode) { undoOnline(); return; }

        if (aiAutoStartTimer != null && !aiAutoStartTimer.isExpired()) {
            aiAutoStartTimer.expire(); aiAutoStartTimer = null;
        }
        if (model.undoMove()) {
            refreshBoardView();
            // 悔棋后如果是 AI 回合，延迟触发 AI
            if (!model.isRedTurn() && !model.isGameOver()) {
                if(getApp().isAIEnabled()) {
                    aiAutoStartTimer = runOnce(() -> startAITurn(getApp().getAIDifficulty()), Duration.seconds(1.0));
                }
            }
        }
    }

    // =========================================================
    //                 本地交互逻辑
    // =========================================================

    public void onGridClicked(int row, int col) {
        if (getApp().isSettingUp()) {
            handleSetupClick(row, col, getApp());
            return;
        }
        if (model.isGameOver()) return;

        Entity clickedEntity = findEntityAt(row, col);
        if (selectedEntity != null) {
            if (clickedEntity == selectedEntity) {
                deselectPiece();
                return;
            }
            handleMove(row, col);
        } else {
            if (clickedEntity != null) {
                handleSelection(clickedEntity);
            }
        }
    }

    private void handleSelection(Entity pieceEntity) {
        AbstractPiece logicPiece = pieceEntity.getComponent(PieceComponent.class).getPieceLogic();
        if (isOnlineMode && logicPiece.isRed() != amIRed) return;

        if (logicPiece.isRed() == model.isRedTurn()) {
            this.selectedEntity = pieceEntity;
            this.selectedEntity.getComponent(VisualStateComponent.class).setInactive();
            showLegalMoves(logicPiece);
        }
    }

    private void handleMove(int targetRow, int targetCol) {
        AbstractPiece pieceToMove = selectedEntity.getComponent(PieceComponent.class).getPieceLogic();
        int r1 = pieceToMove.getRow();
        int c1 = pieceToMove.getCol();

        boolean success = executeMove(pieceToMove, targetRow, targetCol, false);

        if (success) {
            if (isOnlineMode) {
                netClient.sendMove(r1, c1, targetRow, targetCol);
                syncInputLock();
            } else {
                XiangQiApp app = getApp();
                if (app.isAIEnabled() && !model.isRedTurn() && !model.isGameOver()) {
                    startAITurn(app.getAIDifficulty());
                }
            }
        }
        deselectPiece();
    }

    private boolean executeMove(AbstractPiece piece, int targetRow, int targetCol, boolean isRemote) {
        Entity pieceEntity = findEntityByLogic(piece);
        if (pieceEntity == null) return false;

        AbstractPiece targetLogic = model.getPieceAt(targetRow, targetCol);
        Entity targetEntity = findEntityByLogic(targetLogic);
        Point2D startPos = pieceEntity.getPosition();

        boolean success = model.movePiece(piece, targetRow, targetCol);

        if (success) {
            playMoveAndEndGameAnimation(pieceEntity, targetEntity, startPos, targetRow, targetCol);
            getApp().updateHistoryPanel();

            // 绘制终点高亮 (黄色圆形)
            drawMoveHighlight(targetRow, targetCol, Color.YELLOW, 0);
        }
        return success;
    }

    // =========================================================
    //                 AI 逻辑
    // =========================================================

    public void startAITurn(int depth) {
        getApp().getInputHandler().setLocked(true);

        Task<AIService.MoveResult> aiTask = new Task<>() {
            @Override
            protected AIService.MoveResult call() throws Exception {
                return aiService.search(model, depth, false);
            }
        };

        aiTask.setOnSucceeded(e -> {
            if (!model.isGameOver() && !model.isRedTurn()) {
                AIService.MoveResult res = aiTask.getValue();
                if (res != null && res.move != null) {
                    MoveCommand cmd = res.move;
                    AbstractPiece realPiece = model.getPieceAt(cmd.getStartRow(), cmd.getStartCol());
                    if (realPiece != null) {
                        executeMove(realPiece, cmd.getEndRow(), cmd.getEndCol(), false);
                    }
                }
            }
            getApp().getInputHandler().setLocked(false);
        });

        aiTask.setOnFailed(e -> getApp().getInputHandler().setLocked(false));
        new Thread(aiTask).start();
    }

    public void requestAIHint() {
        if (model.isGameOver()) return;
        getDialogService().showMessageBox("AI正在思考提示...");

        Task<AIService.MoveResult> hintTask = new Task<>() {
            @Override
            protected AIService.MoveResult call() throws Exception {
                // 请求 AI 计算一步最佳走法
                return aiService.search(model, 4, model.isRedTurn());
            }
        };

        hintTask.setOnSucceeded(e -> {
            AIService.MoveResult res = hintTask.getValue();
            if (res != null && res.move != null) {
                // 获取 AI 建议移动的棋子
                AbstractPiece pieceToMove = res.move.getMovedPiece();

                int r2 = res.move.getEndRow();
                int c2 = res.move.getEndCol();

                // 【修改】不再画圆，而是显示该棋子的半透明虚影
                showGhostPieceHint(pieceToMove, r2, c2);
            }
        });

        // 启动后台线程
        new Thread(hintTask).start();
    }

    // =========================================================
    //                 高亮 & 辅助
    // =========================================================

    private void drawMoveHighlight(int row, int col, Color color, double autoRemoveTime) {
        if (autoRemoveTime <= 0) {
            for (Entity e : highlightEntities) e.removeFromWorld();
            highlightEntities.clear();
        }

        Point2D topLeft = XiangQiApp.getVisualPosition(row, col);
        double offset = (CELL_SIZE - 8) / 2.0;
        Point2D center = topLeft.add(offset, offset);

        // 圆形高亮
        double radius = offset + 2;
        Entity circle = entityBuilder()
                .at(center)
                .view(new Circle(radius, color.deriveColor(0, 1, 1, 0.5))) // 50% 透明
                .zIndex(-1)
                .buildAndAttach();

        if (autoRemoveTime > 0) {
            runOnce(circle::removeFromWorld, Duration.seconds(autoRemoveTime));
        } else {
            highlightEntities.add(circle);
        }
    }

    /**
     * 【新增】在指定位置显示一个半透明的棋子虚影
     */
    private void showGhostPieceHint(AbstractPiece piece, int row, int col) {
        // 1. 清除旧的高亮/虚影
        for (Entity e : highlightEntities) e.removeFromWorld();
        highlightEntities.clear();

        // 2. 计算图片文件名 (逻辑与 Factory 中一致)
        // 格式例如: "RedCannon.png" 或 "BlackHorse.png"
        String colorPrefix = piece.isRed() ? "Red" : "Black";
        String typeName = piece.getClass().getSimpleName().replace("Piece", "");
        String textureName = colorPrefix + typeName + ".png";

        // 3. 加载图片纹理
        var texture = FXGL.getAssetLoader().loadTexture(textureName);

        // 设置尺寸 (需与 XiangQiApp.CELL_SIZE 配合，这里减去一点边距)
        double visualSize = XiangQiApp.CELL_SIZE - 8;
        texture.setFitWidth(visualSize);
        texture.setFitHeight(visualSize);
        texture.setPreserveRatio(true);

        // 【关键】设置透明度 (0.0 完全透明 - 1.0 完全不透明)
        // 0.6 左右可以营造出不错的“虚影”效果
        texture.setOpacity(0.6);

        // 4. 计算屏幕坐标
        Point2D pos = XiangQiApp.getVisualPosition(row, col);

        // 5. 生成实体
        Entity ghost = entityBuilder()
                .at(pos) // 设置位置
                .view(texture) // 设置半透明图片
                .zIndex(100)   // 放在最上层，确保能覆盖住目标位置可能存在的敌方棋子
                .buildAndAttach();

        // 6. 添加到列表以便管理，并设置 3 秒后自动消失
        highlightEntities.add(ghost);
        runOnce(ghost::removeFromWorld, Duration.seconds(3.0));
    }

    // ... (排局处理 handleSetupClick 等保持不变) ...
    private void handleSetupClick(int row, int col, XiangQiApp app) {
        AbstractPiece existing = model.getPieceAt(row, col);
        String type = app.getSelectedPieceType();

        if ("Eraser".equals(type)) {
            if (existing != null) { model.getPieces().remove(existing); app.spawnPiecesFromModel(); FXGL.play("按钮音效1.mp3"); }
            return;
        }

        if (type != null) {
            boolean isRed = app.isSelectedPieceRed();
            if (existing != null && existing.isRed() == isRed && existing.getClass().getSimpleName().startsWith(type)) {
                model.getPieces().remove(existing);
                app.spawnPiecesFromModel();
                return;
            }

            AbstractPiece newPiece = createPiece(type, row, col, isRed);

            // 校验逻辑
            if (newPiece instanceof GeneralPiece) {
                if (!isValidPalace(newPiece)) { getDialogService().showMessageBox("必须在九宫格内"); return; }
                AbstractPiece old = model.FindKing(isRed);
                if (old != null) model.getPieces().remove(old);
            }
            if (newPiece instanceof AdvisorPiece && !isValidAdvisorPosition(newPiece)) {
                getDialogService().showMessageBox("仕位置不合法"); return;
            }
            if (newPiece instanceof ElephantPiece && !isValidElephantPosition(newPiece)) {
                getDialogService().showMessageBox("象位置不合法"); return;
            }

            model.addPiece(newPiece);
            app.spawnPiecesFromModel();
            FXGL.play("按钮音效1.mp3");
        } else if (existing != null) {
            model.getPieces().remove(existing);
            app.spawnPiecesFromModel();
        }
    }

    // 校验逻辑
    private boolean isValidPalace(AbstractPiece p) {
        int r = p.getRow(); int c = p.getCol();
        if (c < 3 || c > 5) return false;
        return p.isRed() ? (r >= 7 && r <= 9) : (r >= 0 && r <= 2);
    }
    private boolean isValidAdvisorPosition(AbstractPiece p) {
        if (!isValidPalace(p)) return false;
        if (p.getCol() == 4) return (p.isRed() ? p.getRow() == 8 : p.getRow() == 1);
        return true;
    }
    private boolean isValidElephantPosition(AbstractPiece p) {
        int r = p.getRow(); int c = p.getCol();
        if (c % 2 != 0) return false;
        if (p.isRed()) {
            if (r < 5) return false;
            return (r == 5 || r == 9) ? (c == 2 || c == 6) : (r == 7 && (c == 0 || c == 4 || c == 8));
        } else {
            if (r > 4) return false;
            return (r == 0 || r == 4) ? (c == 2 || c == 6) : (r == 2 && (c == 0 || c == 4 || c == 8));
        }
    }

    private void showLegalMoves(AbstractPiece piece) {
        clearMoveIndicators();
        for (Point p : piece.getLegalMoves(model)) {
            if (model.tryMoveAndCheckSafe(piece, p.y, p.x)) {
                spawn("MoveIndicator", XiangQiApp.getVisualPosition(p.y, p.x));
            }
        }
    }

    // 辅助方法
    private Entity findEntityAt(int row, int col) {
        Point2D tl = XiangQiApp.getVisualPosition(row, col);
        return getGameWorld().getEntitiesInRange(new Rectangle2D(tl.getX(), tl.getY(), CELL_SIZE-8, CELL_SIZE-8))
                .stream().filter(e -> e.isType(EntityType.PIECE)).findFirst().orElse(null);
    }
    private Entity findEntityByLogic(AbstractPiece logic) {
        if (logic == null) return null;
        return getGameWorld().getEntitiesByType(EntityType.PIECE).stream()
                .filter(e -> e.getComponent(PieceComponent.class).getPieceLogic() == logic)
                .findFirst().orElse(null);
    }
    private void deselectPiece() {
        if (selectedEntity != null) {
            selectedEntity.getComponent(VisualStateComponent.class).setNormal();
            selectedEntity = null;
            clearMoveIndicators();
        }
    }
    private void clearMoveIndicators() { getGameWorld().getEntitiesByType(EntityType.MOVE_INDICATOR).forEach(Entity::removeFromWorld); }

    private void playMoveAndEndGameAnimation(Entity e, Entity t, Point2D start, int r, int c) {
        Point2D target = XiangQiApp.getVisualPosition(r, c);
        e.setPosition(target);

        animationBuilder().duration(Duration.seconds(0.2)).translate(e).from(start).to(target).buildAndPlay();

        runOnce(() -> {
            if (t != null) t.removeFromWorld();
            if (model.isGameOver()) showGameOverBanner();
            updateTurnIndicator();
        }, Duration.seconds(0.25));
    }

    private void showGameOverBanner() {
        XiangQiApp app = getApp();
        Text banner = app.getGameOverBanner();
        banner.setText(model.getWinner() + " 胜！");
        app.centerTextInApp(banner);
        app.getGameOverDimmingRect().setVisible(true);
        banner.setVisible(true);
        animationBuilder().duration(Duration.seconds(0.5)).scale(banner).from(new Point2D(0,0)).to(new Point2D(1,1)).buildAndPlay();
        updateTurnIndicator();
    }

    private AbstractPiece createPiece(String type, int r, int c, boolean red) {
        switch (type) {
            case "General": return new GeneralPiece(red?"帅":"将", r, c, red);
            case "Advisor": return new AdvisorPiece(red?"仕":"士", r, c, red);
            case "Elephant":return new ElephantPiece(red?"相":"象", r, c, red);
            case "Horse":   return new HorsePiece("马", r, c, red);
            case "Chariot": return new ChariotPiece("车", r, c, red);
            case "Cannon":  return new CannonPiece("炮", r, c, red);
            default:        return new SoldierPiece(red?"兵":"卒", r, c, red);
        }
    }
}