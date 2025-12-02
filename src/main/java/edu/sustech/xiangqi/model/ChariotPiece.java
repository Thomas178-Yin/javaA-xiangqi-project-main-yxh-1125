package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class ChariotPiece extends AbstractPiece {


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
        List<Point> legalMoves = new ArrayList<>();
        int r = getRow();
        int c = getCol();
        int[][] directions = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] direction : directions) {
            for (int i = 1; i < 10; i++) {
                int nextRow = r + i * direction[0];
                int nextCol = c + i * direction[1];

                if (!model.isValidPosition(nextRow, nextCol)) {
                    break;
                }

                AbstractPiece pieceAtTarget = model.getPieceAt(nextRow, nextCol);
                if (pieceAtTarget == null) {

                    legalMoves.add(new Point(nextCol, nextRow));
                } else {
                    if (pieceAtTarget.isRed() != this.isRed()) {
                        legalMoves.add(new Point(nextCol, nextRow));
                    }
                    break;
                }
            }
        }
        return legalMoves;
        }



    public ChariotPiece(String name, int row, int col, boolean isRed){
            super(name, row, col, isRed);
        }

    @Override
    public AbstractPiece copy() {
        return new ChariotPiece(this.getName(), this.getRow(), this.getCol(), this.isRed());
    }

}

