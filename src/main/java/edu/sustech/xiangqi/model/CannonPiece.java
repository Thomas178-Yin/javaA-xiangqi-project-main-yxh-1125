package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class CannonPiece extends AbstractPiece {

    public CannonPiece(String name, int row, int col, boolean isRed) {
        super(name, row, col, isRed);
    }

    @Override
    public AbstractPiece copy() {
        return new CannonPiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
    }

    @Override
    public boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model) {
        // 1. 获取所有合法的走法列表
        List<Point> legalMoves = getLegalMoves(model);

        // 2. 创建一个代表目标位置的 Point 对象 (注意 x, y 对应 col, row)
        Point targetPoint = new Point(targetCol, targetRow);

        // 3. 检查列表是否包含这个目标点
        return legalMoves.contains(targetPoint);
    }

    @Override
    public List<Point> getLegalMoves(ChessBoardModel model) {
        List<Point> moves = new ArrayList<>();
        int r = getRow();
        int c = getCol();

        // 定义四个方向
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            boolean hasFoundCannonMount = false; // 在每个方向上，重置“是否已找到炮架”的状态

            // 对每个方向进行检测
            for (int i = 1; i < 10; i++) {
                int nextRow = r + i * dir[0];
                int nextCol = c + i * dir[1];

                // 1. 检查是否越界
                if (!model.isValidPosition(nextRow, nextCol)) {
                    break;
                }

                AbstractPiece pieceAtTarget = model.getPieceAt(nextRow, nextCol);

                if (!hasFoundCannonMount) {
                    if (pieceAtTarget == null) {
                        moves.add(new Point(nextCol, nextRow));
                    } else {
                        // 遇到了第一个棋子，它成为了“炮架”
                        hasFoundCannonMount = true;
                    }
                } else {
                    // --- 已经有了一个炮架 ---
                    if (pieceAtTarget != null) {
                        if (pieceAtTarget.isRed() != this.isRed()) {
                            moves.add(new Point(nextCol, nextRow));
                        }
                        break;
                    }
                }
            }
        }
        return moves;
    }

}