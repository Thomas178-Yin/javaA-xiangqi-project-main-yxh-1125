package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class AdvisorPiece extends AbstractPiece{
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

        // 四个方向
        int[][] potentialOffsets = {
                {-1, -1}, {-1, 1},
                {1, -1},  {1, 1}
        };

        // 定义九宫格的边界
        int minRow, maxRow;
        final int minCol = 3;
        final int maxCol = 5;

        if (isRed()) {
            minRow = 7; maxRow = 9;
        } else {
            minRow = 0; maxRow = 2;
        }

        for (int[] offset : potentialOffsets) {
            int targetRow = r + offset[0];
            int targetCol = c + offset[1];

            if (targetRow < minRow || targetRow > maxRow || targetCol < minCol || targetCol > maxCol) {
                continue;
            }
            AbstractPiece pieceAtTarget = model.getPieceAt(targetRow, targetCol);
            if (pieceAtTarget != null && pieceAtTarget.isRed() == this.isRed()) {
                continue;
            }
            moves.add(new Point(targetCol, targetRow));
        }

        return moves;
    }


    public AdvisorPiece(String name, int row, int col, boolean isRed) {
        super(name, row, col, isRed);
    }

    @Override
    public AbstractPiece copy() {
        return new AdvisorPiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
    }

}
