package edu.sustech.xiangqi.model;
import java.awt.Point;
import java.util.List;
import java.io.Serializable;

public abstract class AbstractPiece implements Serializable{
    private static final long serialVersionUID = 1L;

    private final String name;
    private final boolean isRed;
    private int row;
    private int col;

    public AbstractPiece(String name, int row, int col, boolean isRed) {
        this.name = name;
        this.row = row;
        this.col = col;
        this.isRed = isRed;
    }

    public abstract AbstractPiece copy();


    public String getName() {
        return name;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public boolean isRed() {
        return isRed;
    }

    public void moveTo(int newRow, int newCol) {
        this.row = newRow;
        this.col = newCol;
    }

    /**
     * 判断棋子是否可以移动到目标位置
     * @return 是否可以移动
     */
    public abstract boolean canMoveTo(int targetRow, int targetCol, ChessBoardModel model);


    /**
     * 【新方法】生成该棋子所有符合基本规则的走法列表。
     * “基本规则”指不考虑移动后是否会导致自己被将军。
     *
     * @param model 棋盘模型的引用，用于获取其他棋子的位置。
     * @return 一个包含所有合法落点坐标 (Point对象) 的列表。
     */
    public abstract List<Point> getLegalMoves(ChessBoardModel model);
}


