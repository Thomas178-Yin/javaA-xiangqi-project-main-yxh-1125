package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class ElephantPiece extends AbstractPiece {
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

        // 象有四个移动方向
        int[][] potentialOffsets = {
                {-2, -2}, {-2, 2},
                {2, -2},  {2, 2}
        };

        for (int[] offset : potentialOffsets) {
            int targetRow = r + offset[0];
            int targetCol = c + offset[1];
            if (!model.isValidPosition(targetRow, targetCol)) {
                continue;
            }

            //不能过河
            if (isRed()) {
                if (targetRow < 5) {
                    continue;
                }
            } else {
                if (targetRow > 4) {
                    continue;
                }
            }

            //不能被堵象眼
            int eyeRow = r + offset[0] / 2;
            int eyeCol = c + offset[1] / 2;
            if (model.getPieceAt(eyeRow, eyeCol) != null) {
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

    public ElephantPiece(String name, int row, int col, boolean isRed) {
        super(name, row, col, isRed);
    }

    @Override
    public AbstractPiece copy() {
        return new  ElephantPiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
    }
}
