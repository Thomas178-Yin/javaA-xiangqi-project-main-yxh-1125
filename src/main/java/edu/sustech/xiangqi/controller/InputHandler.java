package edu.sustech.xiangqi.controller;

import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.model.ChessBoardModel;
import javafx.geometry.Point2D;
import static edu.sustech.xiangqi.XiangQiApp.*; // 导入静态常量

public class InputHandler {

    private final boardController boardController;
    private boolean isLocked = false;
    public void setLocked(boolean locked) { this.isLocked = locked; }
    public boolean isLocked() { return isLocked; }

    public InputHandler(boardController boardController) {
        this.boardController = boardController;
    }

    public void handleMouseClick(Point2D screenPosition) {
        if (isLocked) return;
        double clickX = screenPosition.getX();
        double clickY = screenPosition.getY();

        // 边界检查：Y轴也要检查是否在棋盘范围内
        if (clickX < BOARD_START_X || clickX > BOARD_START_X + BOARD_WIDTH ||
                clickY < BOARD_START_Y || clickY > BOARD_START_Y + BOARD_HEIGHT) { // 使用 BOARD_START_Y
            return;
        }

        // 计算偏移量：Y轴减去 BOARD_START_Y
        double xInGrid = clickX - (BOARD_START_X + MARGIN);
        double yInGrid = clickY - (BOARD_START_Y + MARGIN); // 使用 BOARD_START_Y

        int visualCol = Math.round((float) (xInGrid / CELL_SIZE));
        int visualRow = Math.round((float) (yInGrid / CELL_SIZE));

        // 将视觉坐标转换为逻辑坐标
        int row, col;
        if (XiangQiApp.isBoardFlipped) {
            row = 9 - visualRow;
            col = 8 - visualCol;
        } else {
            row = visualRow;
            col = visualCol;
        }

        if (row < 0 || row >= ChessBoardModel.getRows() || col < 0 || col >= ChessBoardModel.getCols()) {
            return;
        }

        boardController.onGridClicked(row, col);
    }
}