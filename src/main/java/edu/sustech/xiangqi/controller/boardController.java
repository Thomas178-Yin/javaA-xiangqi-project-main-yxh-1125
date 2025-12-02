package edu.sustech.xiangqi.controller;

import com.almasb.fxgl.animation.Interpolators;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.SpawnData;
import edu.sustech.xiangqi.EntityType;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.view.PieceComponent;
import edu.sustech.xiangqi.view.VisualStateComponent;
import edu.sustech.xiangqi.model.*;
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

    public boardController(ChessBoardModel model) {
        this.model = model;
    }

    public void onGridClicked(int row, int col) {
        XiangQiApp app = getAppCast();

        if (app.isSettingUp()) {
            handleSetupClick(row, col, app);
            return;
        }

        if (model.isGameOver()) {
            return;
        }

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

    /**
     * 【核心修改】处理排局模式下的点击
     */
    private void handleSetupClick(int row, int col, XiangQiApp app) {
        AbstractPiece existingPiece = model.getPieceAt(row, col);
        String selectedType = app.getSelectedPieceType();

        // --- 逻辑 1：橡皮擦模式 ---
        // 如果当前选中的是“橡皮擦”（在App里设置）
        if ("Eraser".equals(selectedType)) {
            if (existingPiece != null) {
                model.getPieces().remove(existingPiece);
                app.spawnPiecesFromModel();
                FXGL.play("按钮音效1.mp3"); // 播放移除音效
            }
            return;
        }

        // --- 逻辑 2：放置模式 ---
        if (selectedType != null) {
            boolean isRed = app.isSelectedPieceRed();

            // 【新增需求】点击相同类型的棋子 -> 执行删除（橡皮擦逻辑）
            if (existingPiece != null) {
                // 检查颜色和类型是否完全一致
                if (existingPiece.isRed() == isRed &&
                        existingPiece.getClass().getSimpleName().startsWith(selectedType)) {

                    model.getPieces().remove(existingPiece);
                    app.spawnPiecesFromModel();
                    return; // 删完就走，不放新的
                }
            }

            // 创建新棋子准备放置
            AbstractPiece newPiece = createPiece(selectedType, row, col, isRed);

            // --- 位置合法性校验 ---

            // A. 将/帅 校验 (九宫格)
            if (newPiece instanceof GeneralPiece) {
                // 1. 范围校验
                if (!isValidPalace(newPiece)) {
                    getDialogService().showMessageBox(newPiece.getName() + " 只能放在九宫格内！");
                    return;
                }
                // 2. 唯一性校验（移除旧的）
                AbstractPiece oldKing = model.FindKing(newPiece.isRed());
                if (oldKing != null && (oldKing.getRow() != row || oldKing.getCol() != col)) {
                    model.getPieces().remove(oldKing);
                }
            }

            // B. 士/仕 校验 (九宫格内的5个点)
            if (newPiece instanceof AdvisorPiece) {
                if (!isValidAdvisorPosition(newPiece)) {
                    getDialogService().showMessageBox(newPiece.getName() + " 位置不合法！\n必须在九宫格的斜线或中心点上。");
                    return;
                }
            }

            // C. 象/相 校验 (本方阵地7个点)
            if (newPiece instanceof ElephantPiece) {
                if (!isValidElephantPosition(newPiece)) {
                    getDialogService().showMessageBox(newPiece.getName() + " 位置不合法！\n只能放在本方阵地的合法人字位，且不能过河。");
                    return;
                }
            }

            // D. 兵/卒 (可选)
            // 兵卒在其实际规则中初始位置只能在特定点，但排局通常允许任意位置（除了底线），暂不严格限制

            // --- 执行放置 ---
            model.addPiece(newPiece);
            app.spawnPiecesFromModel();
            FXGL.play("按钮音效1.mp3");

        } else {
            // --- 逻辑 3：未选中任何工具，点击已有棋子 -> 删除 ---
            if (existingPiece != null) {
                model.getPieces().remove(existingPiece);
                app.spawnPiecesFromModel();
            }
        }
    }

    // --- 校验辅助方法 ---

    /**
     * 判断是否在九宫格范围内 (用于将/帅基础校验)
     */
    private boolean isValidPalace(AbstractPiece p) {
        int r = p.getRow();
        int c = p.getCol();
        if (c < 3 || c > 5) return false; // 列必须在 3-5
        if (p.isRed()) {
            return r >= 7 && r <= 9; // 红方 7-9
        } else {
            return r >= 0 && r <= 2; // 黑方 0-2
        }
    }

    /**
     * 判断是否为合法的士/仕位置 (九宫格内的5个点)
     */
    private boolean isValidAdvisorPosition(AbstractPiece p) {
        // 先检查是否在九宫格大范围内
        if (!isValidPalace(p)) return false;

        int r = p.getRow();
        int c = p.getCol();

        // 合法点位特征：
        // 黑方: (0,3), (0,5), (1,4), (2,3), (2,5)
        // 红方: (9,3), (9,5), (8,4), (7,3), (7,5)
        // 规律：row + col 的奇偶性，或者枚举

        // 中心点总是合法的
        if (p.isRed()) {
            if (r == 8 && c == 4) return true;
        } else {
            if (r == 1 && c == 4) return true;
        }

        // 四角点 (列必须是3或5)
        return c == 3 || c == 5;
    }

    /**
     * 判断是否为合法的象/相位置 (7个固定点)
     */
    private boolean isValidElephantPosition(AbstractPiece p) {
        int r = p.getRow();
        int c = p.getCol();

        // 1. 绝对不能过河
        if (p.isRed() && r < 5) return false;
        if (!p.isRed() && r > 4) return false;

        // 2. 只能在固定的 7 个点
        // 黑方(Row 0-4): (0,2), (0,6), (2,0), (2,4), (2,8), (4,2), (4,6)
        // 红方(Row 5-9): (5,2), (5,6), (7,0), (7,4), (7,8), (9,2), (9,6)

        // 简便算法：列必须是偶数，且满足特定组合
        if (c % 2 != 0) return false; // 必须偶数列

        if (p.isRed()) {
            // 红方行: 5, 7, 9
            if (r == 5 || r == 9) return c == 2 || c == 6;
            if (r == 7) return c == 0 || c == 4 || c == 8;
        } else {
            // 黑方行: 0, 2, 4
            if (r == 0 || r == 4) return c == 2 || c == 6;
            if (r == 2) return c == 0 || c == 4 || c == 8;
        }
        return false;
    }


    private AbstractPiece createPiece(String type, int row, int col, boolean isRed) {
        String name = "";
        switch (type) {
            case "General":  name = isRed ? "帅" : "将"; return new GeneralPiece(name, row, col, isRed);
            case "Advisor":  name = isRed ? "仕" : "士"; return new AdvisorPiece(name, row, col, isRed);
            case "Elephant": name = isRed ? "相" : "象"; return new ElephantPiece(name, row, col, isRed);
            case "Horse":    name = "马"; return new HorsePiece(name, row, col, isRed);
            case "Chariot":  name = "车"; return new ChariotPiece(name, row, col, isRed);
            case "Cannon":   name = "炮"; return new CannonPiece(name, row, col, isRed);
            case "Soldier":  name = isRed ? "兵" : "卒"; return new SoldierPiece(name, row, col, isRed);
            default: return new SoldierPiece("兵", row, col, isRed);
        }
    }

    // --- 以下保持原有逻辑不变 ---
    // (请确保你原有的 findEntityAt, handleSelection, deselectPiece, handleMove 等方法都在这里)

    private void handleSelection(Entity pieceEntity) {
        AbstractPiece logicPiece = pieceEntity.getComponent(PieceComponent.class).getPieceLogic();
        if (logicPiece.isRed() == model.isRedTurn()) {
            this.selectedEntity = pieceEntity;
            this.selectedEntity.getComponent(VisualStateComponent.class).setInactive();
            showLegalMoves(logicPiece);
        }
    }

    private void deselectPiece() {
        if (selectedEntity != null) {
            selectedEntity.getComponent(VisualStateComponent.class).setNormal();
            selectedEntity = null;
            clearMoveIndicators();
        }
    }

    private void handleMove(int targetRow, int targetCol) {
        AbstractPiece pieceToMove = selectedEntity.getComponent(PieceComponent.class).getPieceLogic();
        Entity entityToMove = this.selectedEntity;
        Point2D startPosition = entityToMove.getPosition();
        Entity capturedEntity = findEntityAt(targetRow, targetCol);
        boolean moveSuccess = model.movePiece(pieceToMove, targetRow, targetCol);
        if (moveSuccess) {
            playMoveAndEndGameAnimation(entityToMove, capturedEntity, startPosition, targetRow, targetCol);
        }
        deselectPiece();
    }

    private void playMoveAndEndGameAnimation(Entity entityToMove, Entity capturedEntity, Point2D startPos, int targetRow, int targetCol) {
        Point2D targetPosition = XiangQiApp.getVisualPosition(targetRow, targetCol);
        entityToMove.setPosition(targetPosition);
        boolean willBeGameOver = model.isGameOver();
        animationBuilder()
                .duration(Duration.seconds(0.2))
                .translate(entityToMove)
                .from(startPos)
                .to(targetPosition)
                .buildAndPlay();
        runOnce(() -> {
            if (willBeGameOver) {
                if (capturedEntity != null) capturedEntity.removeFromWorld();
                showGameOverBanner();
            } else {
                if (capturedEntity != null) capturedEntity.removeFromWorld();
                updateTurnIndicator();
            }
        }, Duration.seconds(0.25));
    }

    private void showGameOverBanner() {
        XiangQiApp app = getAppCast();
        Text banner = app.getGameOverBanner();
        Rectangle dimmingRect = app.getGameOverDimmingRect();
        banner.setText(model.getWinner() + " 胜！");
        app.centerTextInApp(banner);
        dimmingRect.setVisible(true);
        runOnce(() -> {
            banner.setScaleX(0);
            banner.setScaleY(0);
            banner.setVisible(true);
            animationBuilder()
                    .duration(Duration.seconds(0.5))
                    .interpolator(Interpolators.EXPONENTIAL.EASE_OUT())
                    .scale(banner)
                    .to(new Point2D(1.0, 1.0))
                    .buildAndPlay();
        }, Duration.seconds(0.5));
        updateTurnIndicator();
    }

    public void updateTurnIndicator() {
        XiangQiApp app = getAppCast();
        var indicator = app.getTurnIndicator();
        indicator.update(model.isRedTurn(), model.isGameOver());
    }

    public void surrender() {
        if (model.isGameOver()) return;
        model.endGame(model.isRedTurn() ? "黑方" : "红方");
        showGameOverBanner();
    }

    private Entity findEntityAt(int row, int col) {
        Point2D topLeft = XiangQiApp.getVisualPosition(row, col);
        double pieceSize = CELL_SIZE - 8;
        Rectangle2D selectionRect = new Rectangle2D(topLeft.getX(), topLeft.getY(), pieceSize, pieceSize);
        return getGameWorld().getEntitiesInRange(selectionRect)
                .stream()
                .filter(e -> e.isType(EntityType.PIECE))
                .findFirst()
                .orElse(null);
    }

    public void undo() {
        boolean undoSuccess = model.undoMove();
        if (undoSuccess) {
            XiangQiApp app = getAppCast();
            app.spawnPiecesFromModel();
            updateTurnIndicator();
            deselectPiece();
        }
    }

    private void clearMoveIndicators() {
        getGameWorld().getEntitiesByType(EntityType.MOVE_INDICATOR).forEach(Entity::removeFromWorld);
    }

    private void showLegalMoves(AbstractPiece piece) {
        clearMoveIndicators();
        List<Point> moves = piece.getLegalMoves(model);
        for (Point p : moves) {
            if (model.tryMoveAndCheckSafe(piece, p.y, p.x)){
                Point2D pos = XiangQiApp.getVisualPosition(p.y, p.x);
                spawn("MoveIndicator", pos);
            }
        }
    }
}