package edu.sustech.xiangqi.ai;

import edu.sustech.xiangqi.model.*;

public class AIEvaluator {


    // 1. 基础子力价值
    private static final int BASE_ROOK    = 900;  // 车
    private static final int BASE_HORSE   = 400;  // 马
    private static final int BASE_CANNON  = 450;  // 炮
    private static final int BASE_BISHOP  = 20;   // 相
    private static final int BASE_ADVISOR = 20;   // 仕
    private static final int BASE_PAWN    = 10;   // 兵
    private static final int BASE_KING    = 10000;

    // 2. 位置附加分 (这个是ai从网上抄的，应该能work吧）


    // 【车】的位置分
    private static final int[][] PST_ROOK = {
            { 14, 14, 12, 18, 16, 18, 12, 14, 14 }, // 0: 敌方底线
            { 16, 20, 18, 24, 26, 24, 18, 20, 16 },
            { 12, 12, 12, 18, 18, 18, 12, 12, 12 },
            { 12, 18, 16, 22, 22, 22, 16, 18, 12 },
            { 12, 14, 12, 18, 18, 18, 12, 14, 12 }, // 4: 河界
            { 12, 16, 14, 20, 20, 20, 14, 16, 12 }, // 5: 河界
            {  6, 10,  8, 14, 14, 14,  8, 10,  6 },
            {  4,  8,  6, 14, 12, 14,  6,  8,  4 },
            {  8,  4,  8, 16,  8, 16,  8,  4,  8 },
            { -2, 10,  6, 14, 12, 14,  6, 10, -2 }  // 9: 己方底线
    };

    // 【马】的位置分
    private static final int[][] PST_HORSE = {
            {  4,  8, 16, 12,  4, 12, 16,  8,  4 },
            {  4, 10, 28, 16,  8, 16, 28, 10,  4 },
            { 12, 14, 16, 20, 18, 20, 16, 14, 12 },
            {  8, 24, 18, 24, 20, 24, 18, 24,  8 },
            {  6, 16, 14, 18, 16, 18, 14, 16,  6 },
            {  4, 12, 16, 14, 12, 14, 16, 12,  4 },
            {  2,  6,  8,  6, 10,  6,  8,  6,  2 },
            {  4,  2,  8,  8,  4,  8,  8,  2,  4 },
            {  0,  2,  4,  4, -2,  4,  4,  2,  0 },
            {  0, -4,  0,  0,  0,  0,  0, -4,  0 }
    };

    // 【炮】的位置分
    private static final int[][] PST_CANNON = {
            {  6,  4,  0, -10, -12, -10,  0,  4,  6 },
            {  2,  2,  0, -4, -14, -4,  0,  2,  2 },
            {  2,  2,  0, -10, -8, -10,  0,  2,  2 },
            {  0,  0, -2,  4, 10,  4, -2,  0,  0 },
            {  0,  0,  0,  2,  4,  2,  0,  0,  0 },
            { -2,  0,  4,  2,  6,  2,  4,  0, -2 },
            {  0,  0,  0,  2,  4,  2,  0,  0,  0 },
            {  4,  0,  8,  6, 10,  6,  8,  0,  4 },
            {  0,  2,  4,  6,  6,  6,  4,  2,  0 },
            {  0,  0,  2,  6,  6,  6,  2,  0,  0 }
    };

    // 【兵】的位置分
    private static final int[][] PST_PAWN = {
            {  9,  9,  9, 11, 13, 11,  9,  9,  9 }, // 0: 逼近九宫，价值高
            { 19, 24, 34, 42, 44, 42, 34, 24, 19 },
            { 19, 24, 32, 37, 37, 37, 32, 24, 19 },
            { 19, 23, 27, 29, 30, 29, 27, 23, 19 },
            { 14, 18, 20, 27, 29, 27, 20, 18, 14 },
            {  7,  0, 13,  0, 16,  0, 13,  0,  7 }, // 5: 河界 (红方视角)
            {  7,  0,  7,  0, 15,  0,  7,  0,  7 },
            {  0,  0,  0,  0,  0,  0,  0,  0,  0 },
            {  0,  0,  0,  0,  0,  0,  0,  0,  0 },
            {  0,  0,  0,  0,  0,  0,  0,  0,  0 }  // 9: 没过河
    };

    // 士/象/将
    private static final int[][] PST_ADVISOR = {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 3, 0, 0, 0, 0 }, { 0, 0, 0, 3, 0, 3, 0, 0, 0 }
    };

    private static final int[][] PST_BISHOP = {
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0 }
    };
    // 象在家里好，这只是简单占位，实际上象飞田即可

    // =========================================================
    // 3. 核心评估函数
    // =========================================================

    /**
     * @param isRedAi 如果AI是红方，返回的分数越高越好；如果是黑方，返回负数越大越好
     */
    public int evaluate(ChessBoardModel model, boolean isRedAi) {
        int totalScore = 0;

        for (AbstractPiece piece : model.getPieces()) {
            int score = getPieceBaseValue(piece) + getPiecePositionValue(piece);

            if (piece.isRed()) {
                totalScore += score;
            } else {
                totalScore -= score;
            }
        }

        // 视角转换：返回相对于“当前思考者”的分数
        return isRedAi ? totalScore : -totalScore;
    }

    private int getPieceBaseValue(AbstractPiece piece) {
        if (piece instanceof ChariotPiece) return BASE_ROOK;
        if (piece instanceof HorsePiece)   return BASE_HORSE;
        if (piece instanceof CannonPiece)  return BASE_CANNON;
        if (piece instanceof AdvisorPiece) return BASE_ADVISOR;
        if (piece instanceof ElephantPiece)return BASE_BISHOP;
        if (piece instanceof SoldierPiece) return BASE_PAWN;
        if (piece instanceof GeneralPiece) return BASE_KING;
        return 0;
    }

    private int getPiecePositionValue(AbstractPiece piece) {
        int row = piece.getRow();
        int col = piece.getCol();
        int[][] table = null;

        if (piece instanceof ChariotPiece) table = PST_ROOK;
        else if (piece instanceof HorsePiece)   table = PST_HORSE;
        else if (piece instanceof CannonPiece)  table = PST_CANNON;
        else if (piece instanceof SoldierPiece) table = PST_PAWN;
        else if (piece instanceof AdvisorPiece) table = PST_ADVISOR;
        else if (piece instanceof ElephantPiece)table = PST_BISHOP;

        if (table == null) return 0;

        if (piece.isRed()) {
            return table[row][col];
        } else {

            return table[9 - row][col];
        }
    }
}