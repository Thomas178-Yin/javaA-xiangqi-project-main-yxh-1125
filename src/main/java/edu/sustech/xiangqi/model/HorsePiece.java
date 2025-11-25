package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class HorsePiece extends AbstractPiece {
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

            // 定义目标点的相对坐标和“马腿”的相对坐标
            // 格式: {目标点row偏移, 目标点col偏移, 马腿点row偏移, 马腿点col偏移}
            int[][] potentialMoves = {
                    {-2, -1, -1, 0}, {-2, 1, -1, 0}, // 向上跳
                    {2, -1, 1, 0},   {2, 1, 1, 0},   // 向下跳
                    {-1, -2, 0, -1}, {1, -2, 0, -1}, // 向左跳
                    {-1, 2, 0, 1},   {1, 2, 0, 1}    // 向右跳
            };

            for (int[] move : potentialMoves) {

                int legRow = r + move[2];
                int legCol = c + move[3];

                //检查是否蹩马腿
                if (model.isValidPosition(legRow, legCol) && model.getPieceAt(legRow, legCol) == null) {

                    int targetRow = r + move[0];
                    int targetCol = c + move[1];


                    if (model.isValidPosition(targetRow, targetCol)) {
                        AbstractPiece pieceAtTarget = model.getPieceAt(targetRow, targetCol);
                        if (pieceAtTarget == null || pieceAtTarget.isRed() != this.isRed()) {
                            moves.add(new Point(targetCol, targetRow));
                        }
                    }
                }
            }
            return moves;
        }



    public HorsePiece(String name, int row, int col, boolean isRed) {
        super(name, row, col, isRed);
    }

    @Override
    public AbstractPiece copy() {
        return new HorsePiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
    }

}
