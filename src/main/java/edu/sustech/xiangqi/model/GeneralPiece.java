package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 帅/将
 */
public class GeneralPiece extends AbstractPiece {

    public GeneralPiece(String name, int row, int col, boolean isRed) {
        super(name, row, col, isRed);
    }

    @Override
    public AbstractPiece copy() {
        return new GeneralPiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
    }

    @Override
    public boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model) {
        List<Point> legalMoves = getLegalMoves(model);
        Point targetPoint = new Point(targetCol, targetRow);
        return legalMoves.contains(targetPoint);
    }

    @Override
    public List<Point> getLegalMoves(ChessBoardModel model) {
        List<Point> moves = new ArrayList<>();
        int r = getRow();
        int c = getCol();

        // 四个移动方向
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        // 定义九宫格边界
        int minRow, maxRow;
        final int minCol = 3;
        final int maxCol = 5;
        if (isRed()) {
            minRow = 7; maxRow = 9;
        } else {
            minRow = 0; maxRow = 2;
        }

        // 找到对方的王
        AbstractPiece enemyKing = model.FindKing(!this.isRed());

        for (int[] dir : directions) {
            int targetRow = r + dir[0];
            int targetCol = c + dir[1];


            if (targetRow < minRow || targetRow > maxRow || targetCol < minCol || targetCol > maxCol) {
                continue;
            }


            AbstractPiece pieceAtTarget = model.getPieceAt(targetRow, targetCol);
            if (pieceAtTarget != null && pieceAtTarget.isRed() == this.isRed()) {
                continue;
            }


            if (enemyKing != null && targetCol == enemyKing.getCol()) {
                // 如果移动后和对方王在同一列，就需要检查中间是否有障碍
                int pieceCount = 0;
                int startRow = Math.min(targetRow, enemyKing.getRow()) + 1;
                int endRow = Math.max(targetRow, enemyKing.getRow());

                for (int row = startRow; row < endRow; row++) {
                    if (row == this.getRow() && enemyKing.getCol() == this.getCol()) {
                        continue;
                    }
                    if (model.getPieceAt(row, targetCol) != null) {
                        pieceCount++;
                    }
                }

                if (pieceCount == 0) {
                    continue;
                }
            }

            moves.add(new Point(targetCol, targetRow));
        }

        return moves;
    }
}
