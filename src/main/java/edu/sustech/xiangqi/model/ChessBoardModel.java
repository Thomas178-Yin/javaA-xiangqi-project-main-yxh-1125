package edu.sustech.xiangqi.model;
import java.awt.Point;

import java.util.ArrayList;
import java.io.Serializable;
import java.util.List;
import java.util.Stack; // 需要 import
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


public class ChessBoardModel implements Serializable{


    // 储存棋盘上所有的棋子，要实现吃子的话，直接通过pieces.remove(被吃掉的棋子)删除就可以
    private final List<AbstractPiece> pieces;
    private static final int ROWS = 10;
    private static final int COLS = 9;
    private  boolean isRedTurn = true;
    private boolean isGameOver = false;
    private String winner;
    private final Stack<MoveCommand> moveHistory = new Stack<>();
    private transient ObservableList<String> moveHistoryStrings = FXCollections.observableArrayList();
    public boolean aiMode = false;



    //让视图检查是否结束
    public boolean isGameOver() {
        return isGameOver;
    }

    public String getWinner() {
        return winner;
    }

    public boolean isRedTurn() {
        return isRedTurn;
    }

    public ChessBoardModel() {
        pieces = new ArrayList<>();
        initializePieces();
    }

    public void clearBoard() {
        pieces.clear();
        moveHistory.clear();
        moveHistoryStrings.clear();
        isGameOver = false;
        winner = null;
        isRedTurn = true; // 默认红先，后续可调
    }

    public void addPiece(AbstractPiece piece) {
        // 先移除该位置已有的棋子（如果有）
        AbstractPiece existing = getPieceAt(piece.getRow(), piece.getCol());
        if (existing != null) {
            pieces.remove(existing);
        }
        pieces.add(piece);
    }

    public void setRedTurn(boolean isRed) {
        this.isRedTurn = isRed;
    }

    private void initializePieces() {
        // 黑方棋子

            // 黑方棋子 (isRed = false, 位于棋盘上半部分, row 0-4)
            pieces.add(new ChariotPiece("车", 0, 0, false));
            pieces.add(new HorsePiece("马", 0, 1, false));
            pieces.add(new ElephantPiece("象", 0, 2, false));
            pieces.add(new AdvisorPiece("士", 0, 3, false));
            pieces.add(new GeneralPiece("将", 0, 4, false));
            pieces.add(new AdvisorPiece("士", 0, 5, false));
            pieces.add(new ElephantPiece("象", 0, 6, false));
            pieces.add(new HorsePiece("马", 0, 7, false));
            pieces.add(new ChariotPiece("车", 0, 8, false));

            pieces.add(new CannonPiece("炮", 2, 1, false));
            pieces.add(new CannonPiece("炮", 2, 7, false));

            pieces.add(new SoldierPiece("卒", 3, 0, false));
            pieces.add(new SoldierPiece("卒", 3, 2, false));
            pieces.add(new SoldierPiece("卒", 3, 4, false));
            pieces.add(new SoldierPiece("卒", 3, 6, false));
            pieces.add(new SoldierPiece("卒", 3, 8, false));

            // 红方棋子 (isRed = true, 位于棋盘下半部分, row 5-9)
            pieces.add(new SoldierPiece("兵", 6, 0, true));
            pieces.add(new SoldierPiece("兵", 6, 2, true));
            pieces.add(new SoldierPiece("兵", 6, 4, true));
            pieces.add(new SoldierPiece("兵", 6, 6, true));
            pieces.add(new SoldierPiece("兵", 6, 8, true));

            pieces.add(new CannonPiece("炮", 7, 1, true));
            pieces.add(new CannonPiece("炮", 7, 7, true));

            pieces.add(new ChariotPiece("车", 9, 0, true));
            pieces.add(new HorsePiece("马", 9, 1, true));
            pieces.add(new ElephantPiece("相", 9, 2, true)); // 注意红方的象叫“相”
            pieces.add(new AdvisorPiece("仕", 9, 3, true)); // 注意红方的士叫“仕”
            pieces.add(new GeneralPiece("帅", 9, 4, true));
            pieces.add(new AdvisorPiece("仕", 9, 5, true));
            pieces.add(new ElephantPiece("相", 9, 6, true));
            pieces.add(new HorsePiece("马", 9, 7, true));
            pieces.add(new ChariotPiece("车", 9, 8, true));

    }

    public List<AbstractPiece> getPieces() {
        return pieces;
    }

    public AbstractPiece getPieceAt(int row, int col) {
        for (AbstractPiece piece : pieces) {
            if (piece.getRow() == row && piece.getCol() == col) {
                return piece;
            }
        }
        return null;
    }

    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < ROWS && col >= 0 && col < COLS;
    }

    //关于结束提示，之后在写一些gui
    public boolean movePiece(AbstractPiece piece, int newRow, int newCol) {

        if (isGameOver) {
            return false;
        }
        if (piece.isRed() != isRedTurn) {
            return false;
        }

        if (!isValidPosition(newRow, newCol)) {
            return false;
        }

        if (!piece.canMoveTo(newRow, newCol, this)) {
            return false;
        }
        AbstractPiece targetPiece = getPieceAt(newRow, newCol);
        MoveCommand command = new MoveCommand(piece, newRow, newCol, targetPiece);


        if (getPieceAt(newRow, newCol) != null) {
             if(getPieceAt(newRow, newCol) instanceof GeneralPiece){
                     this.isGameOver = true;
                     this.winner = isRedTurn ? "红方" : "黑方";
                 pieces.remove(getPieceAt(newRow, newCol));
                 piece.moveTo(newRow, newCol);
                 return true;
             }
             pieces.remove(getPieceAt(newRow, newCol));
         }
        piece.moveTo(newRow, newCol);
        if (isGameOver) {
            return false;
        }
        isRedTurn = !isRedTurn;

        // 先检查有没有把自己害死
        if (isCheckMate(!isRedTurn)) {
            this.isGameOver = true;
            this.winner = !isRedTurn ? "黑方" : "红方";

        }
        else if (isGeneraInCheck(isRedTurn)) {
            // 顺便处理“将军”的提示
        }


        //在检查另一方
        if (isCheckMate(isRedTurn)) {
            this.isGameOver = true;
            this.winner = isRedTurn ? "黑方" : "红方";

            System.out.println("游戏结束!。胜利者是: " + this.winner);
        }
        else if (isGeneraInCheck(isRedTurn)) {
            // 顺便处理“将军”的提示
        }
        moveHistory.push(command);

            updateHistoryStrings();


        return true;
    }

    /**
     * 【新增】悔棋方法
     * @return true 如果悔棋成功, false 如果没有棋可悔
     */
    public boolean undoMove() {
        if (moveHistory.isEmpty()) {
            return false;
        }

        // 1. 从历史记录中弹出上一步的命令
        MoveCommand lastMove = moveHistory.pop();

        // 2. 执行逆向操作
        // a. 将移动的棋子移回原位
        AbstractPiece pieceToUndo = lastMove.getMovedPiece();
        pieceToUndo.moveTo(lastMove.getStartRow(), lastMove.getStartCol());

        // b. 如果上一步有吃子，将被吃的棋子“复活”并放回棋盘
        AbstractPiece capturedPiece = lastMove.getCapturedPiece();
        if (capturedPiece != null) {
            pieces.add(capturedPiece);
        }

        // 3. 切换回上一回合
        isRedTurn = !isRedTurn;

        // 4. (重要) 撤销游戏结束状态
        // 如果上一步导致了游戏结束，悔棋后游戏应该继续
        if (isGameOver) {
            isGameOver = false;
            winner = null;
        }

        updateHistoryStrings();

        return true;
    }
    //将军检测
    public  boolean isGeneraInCheck(Boolean isGeneraRed){
        AbstractPiece king = FindKing(isGeneraRed);
        AbstractPiece enemyKing = FindKing(!isGeneraRed);

        if (king == null) {
            return false;
        }

        if (enemyKing != null && king.getCol() == enemyKing.getCol()) {

            // 如果在同一列，则检查它们之间是否有其他棋子
            int startRow = Math.min(king.getRow(), enemyKing.getRow()) + 1;
            int endRow = Math.max(king.getRow(), enemyKing.getRow());
            boolean hasPieceInBetween = false;
            for (int r = startRow; r < endRow; r++) {
                if (getPieceAt(r, king.getCol()) != null) {
                    hasPieceInBetween = true;
                    break; // 找到了一个子，就可以停止检查了
                }
            }

            // 如果中间没有棋子，则构成“王对王”将军！
            if (!hasPieceInBetween) {
                return true;
            }
        }


        for (AbstractPiece piece : getPieces()) {
            if(piece.isRed() != isGeneraRed) {
                if (piece.canMoveTo(king.getRow(), king.getCol(), this)) {
                    return true;
                }
            }
        }

        return false;
    }
    //将死检测
    public Boolean isCheckMate(Boolean isPlayerRed) {
        AbstractPiece king = FindKing(isPlayerRed);
        AbstractPiece enemyKing = FindKing(!isPlayerRed);

        if (king == null) {
            return false;
        }
        if (!isGeneraInCheck(isPlayerRed)) {
            return false;
        }
        // 检查是否满足王对王的条件
        if (king != null && enemyKing != null && king.getCol() == enemyKing.getCol()) {
            boolean hasPieceInBetween = false;
            int startRow = Math.min(king.getRow(), enemyKing.getRow()) + 1;
            int endRow = Math.max(king.getRow(), enemyKing.getRow());
            for (int r = startRow; r < endRow; r++) {
                if (getPieceAt(r, king.getCol()) != null) {
                    hasPieceInBetween = true;
                    break;
                }
            }

            // 如果确实是王对王将军（中间无子）
            if (!hasPieceInBetween) {
                if (isRedTurn == !isPlayerRed) {
                    return true;
                }
            }
        }
        for (AbstractPiece piece : getPieces()) {
            if (piece.isRed() ==  isPlayerRed ){
                List<Point> legalMoves = piece.getLegalMoves(this);
                for (Point Move : legalMoves) {
                    int OriginalRow = piece.getRow();
                    int OriginalCol = piece.getCol();
                    int TargetRow = Move.y;
                    int TargetCol = Move.x;

                    piece.moveTo(TargetRow, TargetCol);
                    boolean stillInCheck = isGeneraInCheck(isPlayerRed);
                    piece.moveTo(OriginalRow, OriginalCol);
                    if (!stillInCheck) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public AbstractPiece FindKing(boolean isKingRed){
        for (AbstractPiece piece : getPieces()) {
            if (piece instanceof GeneralPiece && piece.isRed() == isKingRed)
                return piece;
        }
        return null;
    }

    /**
     * 【新增】一个公共方法，用于从外部强制结束游戏并设置胜利者
     * @param winnerName "红方" 或 "黑方"
     */
    public void endGame(String winnerName) {
        this.isGameOver = true;
        this.winner = winnerName;
    }




    private String formatMove(MoveCommand command, int moveNumber) {
        String pieceName = command.getMovedPiece().getName();
        // 中文棋谱列是从右到左数的（对红方而言）
        String startColStr = formatCol(command.getStartCol(), command.getMovedPiece().isRed());
        String endColStr = formatCol(command.getEndCol(), command.getMovedPiece().isRed());

        // 这是一个非常简化的表示法，例如 "炮 (8) -> (5)"
        // 完整的棋谱表示法（如“炮二平五”）非常复杂，暂时先用简化的
        String simpleNotation = String.format("%d. %s: (%d, %s) -> (%d, %s)",
                moveNumber,
                pieceName,
                command.getStartRow(), startColStr,
                command.getEndRow(), endColStr
        );

        return simpleNotation;
    }

    // --- 【新增】反序列化后的修复方法 ---
    /**
     * 当从文件读取存档后，因为 moveHistoryStrings 是 transient (空的)，
     * 我们需要根据 moveHistory 栈重新生成它。
     */
    public void rebuildAfterLoad() {
        // 1. 重新初始化列表（因为它是 null）
        moveHistoryStrings = FXCollections.observableArrayList();
        // 2. 重新填充数据
        updateHistoryStrings();
    }

    private String formatCol(int col, boolean isRed) {
        // 简单的数字转换
        String[] redNums = {"九", "八", "七", "六", "五", "四", "三", "二", "一"};
        String[] blackNums = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
        return isRed ? redNums[col] : blackNums[col];
    }

    private void updateHistoryStrings() {
        moveHistoryStrings.clear(); // 先清空

        // 遍历栈中的每一步
        for (int i = 0; i < moveHistory.size(); i++) {
            MoveCommand cmd = moveHistory.get(i);
            // 生成类似 "1. 红方 车: (9,0) -> (8,0)" 的简单格式
            // 暂时先不用复杂的中文棋谱，先把流程跑通
            String record = String.format("第%d步: %s %s (%d,%d) -> (%d,%d)",
                    i + 1,
                    cmd.getMovedPiece().isRed() ? "红" : "黑",
                    cmd.getMovedPiece().getName(),
                    cmd.getStartRow(), cmd.getStartCol(),
                    cmd.getEndRow(), cmd.getEndCol()
            );
            moveHistoryStrings.add(record);
        }
    }


    public ObservableList<String> getMoveHistoryAsObservableList() {
        return moveHistoryStrings;
    }


    public static int getRows() {
        return ROWS;
    }

    public static int getCols() {
        return COLS;
    }

    /**
     * 获取指定阵营目前所有的合法走法
     * @param isRed true获取红方走法，false获取黑方
     * @return 封装好的 MoveCommand 列表
     */
    public List<MoveCommand> getAllLegalMoves(boolean isRed) {
        List<MoveCommand> moves = new ArrayList<>();

        for (AbstractPiece piece : pieces) {
            // 只处理指定阵营的棋子
            if (piece.isRed() == isRed) {
                // 获取该棋子所有落点
                List<Point> legalPoints = piece.getLegalMoves(this);

                for (Point p : legalPoints) {
                    int targetRow = p.y;
                    int targetCol = p.x;
                    AbstractPiece targetPiece = getPieceAt(targetRow, targetCol);
                    moves.add(new MoveCommand(piece, targetRow, targetCol, targetPiece));
                }
            }
        }
        return moves;
    }

    public ChessBoardModel deepClone() {
        ChessBoardModel newModel = new ChessBoardModel();

        // 1. 清空新棋盘默认初始化的棋子（因为构造函数里可能自带了初始化）
        newModel.pieces.clear();
        newModel.moveHistory.clear(); // 历史记录不用克隆，AI 不需要知道以前发生了什么

        // 2. 复制基本状态
        newModel.isRedTurn = this.isRedTurn;
        newModel.isGameOver = this.isGameOver;
        newModel.winner = this.winner;
        newModel.aiMode = true; // 克隆出来的棋盘，默认就是 AI 模式（静音模式）

        // 3. 【核心】深拷贝每一个棋子
        for (AbstractPiece piece : this.pieces) {
            AbstractPiece clonedPiece = piece.copy();
            newModel.pieces.add(clonedPiece);
        }

        return newModel;
    }
}
