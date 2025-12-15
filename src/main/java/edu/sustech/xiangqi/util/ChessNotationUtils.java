package edu.sustech.xiangqi.util;

import edu.sustech.xiangqi.model.AbstractPiece;
import edu.sustech.xiangqi.model.MoveCommand;

//术语转换
public class ChessNotationUtils {

    private static final String[] NUM_RED = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] NUM_BLACK = {"", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static String getNotation(MoveCommand cmd) {
        AbstractPiece piece = cmd.getMovedPiece();
        boolean isRed = piece.isRed();
        int startCol = piece.getCol();
        int startRow = piece.getRow();
        int endCol = cmd.getEndCol();
        int endRow = cmd.getEndRow();

        //棋子名字
        String name = piece.getName();

        // 2. 计算起始列号
        // 红方视角：Col 0 是 "九"，Col 8 是 "一"
        // 黑方视角：Col 0 是 "1"，Col 8 是 "9"
        int srcColNum = isRed ? (9 - startCol) : (startCol + 1);

        // 3. 计算方向
        String dir = "";
        int dist = Math.abs(endRow - startRow);

        if (startRow == endRow) {
            dir = "平";
        } else {
            // 红方变小是进
            // 黑方变大是进
            boolean isAdvance = isRed ? (endRow < startRow) : (endRow > startRow);
            dir = isAdvance ? "进" : "退";
        }

        //计算落点
        String target = "";

        // 直线走子 (车、炮、兵、将)：进退按步数，平按列号
        boolean isLinear = isLinearPiece(name);

        if (dir.equals("平")) {
            // 平
            int destColNum = isRed ? (9 - endCol) : (endCol + 1);
            target = getNumStr(destColNum, isRed);
        } else {
            // 进/退
            if (isLinear) {
                // 直线子：显示走的步数
                target = getNumStr(dist, isRed);
            } else {
                // 斜线子 (马、象、士)：显示落点的列号
                int destColNum = isRed ? (9 - endCol) : (endCol + 1);
                target = getNumStr(destColNum, isRed);
            }
        }

        //组合
        return name + getNumStr(srcColNum, isRed) + dir + target;
    }

    private static boolean isLinearPiece(String name) {
        return name.equals("车") || name.equals("炮") || name.equals("兵") || name.equals("卒") || name.equals("帅") || name.equals("将");
    }

    private static String getNumStr(int num, boolean isRed) {
        if (num < 1 || num > 9) return ""; // 防御
        return isRed ? NUM_RED[num] : NUM_BLACK[num];
    }
}