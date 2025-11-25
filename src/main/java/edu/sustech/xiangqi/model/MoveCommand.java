package edu.sustech.xiangqi.model;
import java.io.Serializable;

public class MoveCommand implements Serializable{
    private static final long serialVersionUID = 1L;

    private final AbstractPiece movedPiece;
    private final int startRow;
    private final int startCol;
    private final int endRow;
    private final int endCol;
    private final AbstractPiece capturedPiece; // 可能为 null

    public MoveCommand(AbstractPiece movedPiece, int endRow, int endCol, AbstractPiece capturedPiece) {
        this.movedPiece = movedPiece;
        this.startRow = movedPiece.getRow(); // 记录移动前的位置
        this.startCol = movedPiece.getCol();
        this.endRow = endRow;
        this.endCol = endCol;
        this.capturedPiece = capturedPiece;
    }

    // --- Getters ---
    public AbstractPiece getMovedPiece() { return movedPiece; }
    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public AbstractPiece getCapturedPiece() { return capturedPiece; }
    public int getEndRow() {
        return endRow;
    }

    public int getEndCol() {
        return endCol;
    }
}