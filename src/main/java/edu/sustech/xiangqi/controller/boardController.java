package edu.sustech.xiangqi.controller;

import com.almasb.fxgl.animation.Interpolators;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.awt.Point;
import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;
import static edu.sustech.xiangqi.XiangQiApp.CELL_SIZE;

public class boardController {

    private ChessBoardModel model;
    private Entity selectedEntity = null;
    private final AIService aiService = new AIService();
    private NetworkClient netClient;
    private boolean isOnlineMode = false;

    // 记录联机模式下自己的阵营
    private boolean amIRed = true;

    private TimerAction aiAutoStartTimer = null;

    public boardController(ChessBoardModel model) {
        this.model = model;
    }

    // =========================================================
    //                 网络 / 联机逻辑
    // =========================================================

    public void connectToRoom(String ip, String roomId) {
        isOnlineMode = true;
        netClient = new NetworkClient();
        netClient.setOnMessage(this::onNetworkMessage);

        ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);
        getDialogService().showMessageBox("正在连接服务器 (房间 " + roomId + ")...");

        new Thread(() -> {
            try {
                netClient.connect(ip, 9999, roomId);
            } catch (Exception e) {
                Platform.runLater(() -> getDialogService().showMessageBox("连接失败: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 处理服务器发来的所有消息
     */
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
                    syncInputLock(); // 对方走完，轮到我解锁
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

    // --- 本地执行动作 (网络指令的接收端) ---

    private void syncInputLock() {
        if (!isOnlineMode) return;
        // 如果当前回合是我的颜色，解锁；否则锁住
        if (model.isRedTurn() == amIRed) {
            ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false);
        } else {
            ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);
        }
    }

    private void doUndo() {
        if (model.undoMove()) {
            refreshBoardView();
            syncInputLock();
        }
    }

    private void doRestart() {
        // 【关键】联机模式不能销毁 Controller，必须软重置
        model.reset();
        refreshBoardView();

        // 重置 UI 状态
        XiangQiApp app = getAppCast();
        app.getGameOverDimmingRect().setVisible(false);
        app.getGameOverBanner().setVisible(false);

        syncInputLock();
    }

    private void doSwapAndRestart() {
        amIRed = !amIRed; // 交换身份
        doRestart();
        String role = amIRed ? "红方 (先手)" : "黑方 (后手)";
        getDialogService().showMessageBox("身份已交换，现在你是：" + role);
    }

    private void refreshBoardView() {
        XiangQiApp app = getAppCast();
        app.spawnPiecesFromModel();
        updateTurnIndicator();
        deselectPiece();
    }

    // --- 发送请求 (UI 按钮调用) ---

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


    // =========================================================
    //                 本地交互逻辑
    // =========================================================

    public void onGridClicked(int row, int col) {
        XiangQiApp app = getAppCast();
        if (app.isSettingUp()) {
            handleSetupClick(row, col, app);
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

        // 联机模式下只能选自己的棋子
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

        // 统一执行移动
        boolean success = executeMove(pieceToMove, targetRow, targetCol, false);

        if (success) {
            if (isOnlineMode) {
                netClient.sendMove(r1, c1, targetRow, targetCol);
                syncInputLock();
            } else {
                // 本地模式：检测是否启用 AI
                XiangQiApp app = (XiangQiApp) FXGL.getApp();
                // 只有当 AI 开启，且当前轮到黑方时，触发 AI
                if (app.isAIEnabled() && !model.isRedTurn() && !model.isGameOver()) {
                    startAITurn();
                }
            }
        }
        deselectPiece();
    }

    // 通用执行方法
    private boolean executeMove(AbstractPiece piece, int targetRow, int targetCol, boolean isRemote) {
        Entity pieceEntity = findEntityByLogic(piece);
        if (pieceEntity == null) return false;

        AbstractPiece targetLogic = model.getPieceAt(targetRow, targetCol);
        Entity targetEntity = findEntityByLogic(targetLogic);
        Point2D startPos = pieceEntity.getPosition();

        boolean success = model.movePiece(piece, targetRow, targetCol);

        if (success) {
            playMoveAndEndGameAnimation(pieceEntity, targetEntity, startPos, targetRow, targetCol);
        }
        return success;
    }

    // =========================================================
    //                 AI 逻辑
    // =========================================================

    public void startAITurn() {
        // 锁住 UI 防止人类干扰
        ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(true);

        Task<AIService.MoveResult> aiTask = new Task<>() {
            @Override
            protected AIService.MoveResult call() throws Exception {
                // 深度 4，假定 AI 执黑 (false)
                return aiService.search(model, 4, false);
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
            ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false);
        });

        aiTask.setOnFailed(e -> ((XiangQiApp) FXGL.getApp()).getInputHandler().setLocked(false));
        new Thread(aiTask).start();
    }

    public void requestAIHint() {
        if (model.isGameOver()) return;
        getDialogService().showMessageBox("AI正在思考提示...");

        Task<AIService.MoveResult> hintTask = new Task<>() {
            @Override
            protected AIService.MoveResult call() throws Exception {
                return aiService.search(model, 4, model.isRedTurn());
            }
        };

        hintTask.setOnSucceeded(e -> {
            AIService.MoveResult res = hintTask.getValue();
            if (res != null && res.move != null) {
                Point2D start = XiangQiApp.getVisualPosition(res.move.getStartRow(), res.move.getStartCol());
                Point2D end = XiangQiApp.getVisualPosition(res.move.getEndRow(), res.move.getEndCol());

                Entity h1 = spawn("MoveIndicator", start);
                Entity h2 = spawn("MoveIndicator", end);
                runOnce(() -> { h1.removeFromWorld(); h2.removeFromWorld(); }, Duration.seconds(3.0));
            }
        });
        new Thread(hintTask).start();
    }

    // =========================================================
    //                 排局设置 & 辅助方法
    // =========================================================

    private void handleSetupClick(int row, int col, XiangQiApp app) {
        AbstractPiece existing = model.getPieceAt(row, col);
        String type = app.getSelectedPieceType();

        if ("Eraser".equals(type)) {
            if (existing != null) { model.getPieces().remove(existing); app.spawnPiecesFromModel(); FXGL.play("按钮音效1.mp3"); }
            return;
        }

        if (type != null) {
            boolean isRed = app.isSelectedPieceRed();
            // 点击同类棋子删除
            if (existing != null && existing.isRed() == isRed && existing.getClass().getSimpleName().startsWith(type)) {
                model.getPieces().remove(existing);
                app.spawnPiecesFromModel();
                return;
            }

            AbstractPiece newPiece = createPiece(type, row, col, isRed);

            // 校验逻辑... (省略具体校验代码以节省空间，功能同上)
            // 校验将帅唯一性
            if (newPiece instanceof GeneralPiece) {
                if (!isValidPalace(newPiece)) { getDialogService().showMessageBox("必须在九宫格内"); return; }
                AbstractPiece old = model.FindKing(isRed);
                if (old != null) model.getPieces().remove(old);
            }
            // 校验仕
            if (newPiece instanceof AdvisorPiece && !isValidAdvisorPosition(newPiece)) {
                getDialogService().showMessageBox("仕位置不合法"); return;
            }
            // 校验象
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

    /**
     * 【修复】本地/单机模式的投降 (之前报错的地方)
     */
    public void surrender() {
        // 如果误触，当前是联机模式，转交联机处理
        if (isOnlineMode) {
            surrenderOnline();
            return;
        }

        if (model.isGameOver()) return;

        String winner = model.isRedTurn() ? "黑方" : "红方";
        model.endGame(winner);
        showGameOverBanner();
    }

    /**
     * 【修复】本地悔棋
     */
    public void undo() {
        if (isOnlineMode) { undoOnline(); return; }

        if (aiAutoStartTimer != null && !aiAutoStartTimer.isExpired()) {
            aiAutoStartTimer.expire(); aiAutoStartTimer = null;
        }
        if (model.undoMove()) {
            refreshBoardView();
            // 如果悔棋后轮到 AI，延迟触发
            if (!model.isRedTurn() && !model.isGameOver()) {
                XiangQiApp app = (XiangQiApp) FXGL.getApp();
                if(app.isAIEnabled()) {
                    aiAutoStartTimer = runOnce(this::startAITurn, Duration.seconds(1.0));
                }
            }
        }
    }

    /**
     * 【修复】更新界面回合指示器 (之前报错的地方)
     */
    public void updateTurnIndicator() {
        XiangQiApp app = getAppCast();
        var indicator = app.getTurnIndicator();
        if (indicator != null) {
            indicator.update(model.isRedTurn(), model.isGameOver());
        }
    }

    private void showLegalMoves(AbstractPiece piece) {
        clearMoveIndicators();
        for (Point p : piece.getLegalMoves(model)) {
            // 防止送将过滤
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
        XiangQiApp app = getAppCast();
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