package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 兵/卒
 */
public class SoldierPiece extends AbstractPiece {

    public SoldierPiece(String name, int row, int col, boolean isRed) {
        super(name, row, col, isRed);
    }

    @Override
    public AbstractPiece copy() {
        return new SoldierPiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
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

        boolean hasCrossedRiver;
        int forwardRow;

        if (isRed()) {
            forwardRow = r - 1;
            hasCrossedRiver = (r <= 4);
        } else {
            forwardRow = r + 1;
            hasCrossedRiver = (r >= 5);
        }

        checkAndAddMove(forwardRow, c, model, moves);

        if (hasCrossedRiver) {
            checkAndAddMove(r, c - 1, model, moves); // 向左
            checkAndAddMove(r, c + 1, model, moves); // 向右
        }

        return moves;
    }

    private void checkAndAddMove(int targetRow, int targetCol, ChessBoardModel model, List<Point> moves) {
        if (!model.isValidPosition(targetRow, targetCol)) {
            return;
        }

        AbstractPiece pieceAtTarget = model.getPieceAt(targetRow, targetCol);
        if (pieceAtTarget != null && pieceAtTarget.isRed() == this.isRed()) {
            return;
        }

        moves.add(new Point(targetCol, targetRow));
    }
}
