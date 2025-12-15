package edu.sustech.xiangqi.ai;

import edu.sustech.xiangqi.model.ChessBoardModel;
import edu.sustech.xiangqi.model.MoveCommand;
import java.util.Collections;
import java.util.List;

public class AIService {

    private final AIEvaluator evaluator = new AIEvaluator();

    public static class MoveResult {
        public int score;
        public MoveCommand move;
        public MoveResult(int score, MoveCommand move) {
            this.score = score;
            this.move = move;
        }
    }

    // 入口
    public MoveResult search(ChessBoardModel realModel, int depth, boolean isRed) {
        // 以后所有的计算都在 cloneModel 上进行，防止污染 realModel
        ChessBoardModel cloneModel = realModel.deepClone();

        List<MoveCommand> rootMoves = cloneModel.getAllLegalMoves(isRed);


        //克隆对象上计算
        MoveResult result = minimax(cloneModel, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, true, isRed);

        // 注意：result 里返回的 MoveCommand 包含的是 cloneModel 里的棋子对象
        // 传回 Controller 后，不能直接用来操作 UI，需要提取坐标
        /// ////////////////////////////////////////////////
        if (result == null || result.move == null) {
            // 随便选一步
            result = new MoveResult(-10000, rootMoves.get(0));
        }
        return result;
    }

    /**
     * Alpha-Beta 剪枝核心
     * @param alpha 目前找到的【最好的】下界
     * @param beta  目前找到的【最差的】上界
     */
    private MoveResult minimax(ChessBoardModel model, int depth, int alpha, int beta, boolean isMax, boolean aiIsRed) {
        //判断结束条件
        if (depth == 0 || model.isGameOver()) {
            return new MoveResult(evaluator.evaluate(model, aiIsRed), null);
        }

        //获取走法
        // 如果是 Max 层，轮到 AI 走 (aiIsRed)
        // 如果是 Min 层，轮到对手走 (!aiIsRed)
        boolean currentMoverIsRed = isMax ? aiIsRed : !aiIsRed;
        List<MoveCommand> moves = model.getAllLegalMoves(currentMoverIsRed);

        MoveCommand bestMove = null;

        if (isMax) {
            int maxEval = Integer.MIN_VALUE;
            for (MoveCommand move : moves) {
                model.movePiece(move.getMovedPiece(), move.getEndRow(), move.getEndCol());

                // 递归
                MoveResult res = minimax(model, depth - 1, alpha, beta, false, aiIsRed);

                model.undoMove(); // 回溯

                if (res.score > maxEval) {
                    maxEval = res.score;
                    bestMove = move;
                }
                // 更新 Alpha
                alpha = Math.max(alpha, res.score);
                // 剪枝：if> Beta ，cut
                if (beta <= alpha) {
                    break;
                }
            }
            return new MoveResult(maxEval, bestMove);

        } else {
            // Min 层 (对手回合)
            int minEval = Integer.MAX_VALUE;
            for (MoveCommand move : moves) {
                model.movePiece(move.getMovedPiece(), move.getEndRow(), move.getEndCol());

                MoveResult res = minimax(model, depth - 1, alpha, beta, true, aiIsRed);

                model.undoMove();

                if (res.score < minEval) {
                    minEval = res.score;
                    bestMove = move;
                }
                // 更新 Beta
                beta = Math.min(beta, res.score);
                // 剪枝
                if (beta <= alpha) {
                    break;
                }
            }
            return new MoveResult(minEval, bestMove);
        }
    }
}