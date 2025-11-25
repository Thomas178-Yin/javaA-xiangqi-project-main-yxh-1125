// edu.sustech.xiangqi.model.PlaceholderPiece.java (新增文件)

package edu.sustech.xiangqi.model;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

public class PlaceholderPiece extends AbstractPiece {

    public PlaceholderPiece() {
        // 构造函数使用占位符值
        super("UI_PLACEHOLDER", -1, -1, false);
    }

    @Override
    public AbstractPiece copy() {
        return new PlaceholderPiece();
    }

    @Override
    public boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model) {
        return false; // 不能移动
    }

    @Override
    public List<Point> getLegalMoves(ChessBoardModel model) {
        return Collections.emptyList(); // 没有合法走法
    }
}