package edu.sustech.xiangqi.util;

import edu.sustech.xiangqi.model.AbstractPiece;
import edu.sustech.xiangqi.model.MoveCommand;

/**
 * [类作用]: 将移动指令转换为中国象棋传统棋谱术语。
 * [输入]: MoveCommand
 * [输出]: String (例如 "炮二平五")
 */
public class ChessNotationUtils {

    private static final String[] NUM_RED = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static final String[] NUM_BLACK = {"", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

    public static String getNotation(MoveCommand cmd) {
        AbstractPiece piece = cmd.getMovedPiece();
        boolean isRed = piece.isRed();
        int startCol = piece.getCol();
        int startRow = piece.getRow(); // 注意：这是移动后的位置，Command里存的是startRow吗？
        // 修正：MoveCommand构造时存的是startRow/Col
        int endCol = cmd.getEndCol();
        int endRow = cmd.getEndRow();

        // 1. 获取棋子名称
        String name = piece.getName();

        // 2. 计算起始列号 (红方从右向左 1-9，黑方也是从他的右向左 1-9)
        // 棋盘 col 0 在左边。
        // 红方视角：Col 0 是 "九"，Col 8 是 "一"
        // 黑方视角：Col 0 是 "1"，Col 8 是 "9"
        int srcColNum = isRed ? (9 - startCol) : (startCol + 1);

        // 3. 计算方向 (进、退、平)
        String dir = "";
        // 纵向位移绝对值
        int dist = Math.abs(endRow - startRow);

        if (startRow == endRow) {
            dir = "平";
        } else {
            // 红方 row 9 -> 0, 变小是进
            // 黑方 row 0 -> 9, 变大是进
            boolean isAdvance = isRed ? (endRow < startRow) : (endRow > startRow);
            dir = isAdvance ? "进" : "退";
        }

        // 4. 计算落点/距离
        String target = "";

        // 直线走子 (车、炮、兵、将)：进退按步数，平按列号
        boolean isLinear = isLinearPiece(name);

        if (dir.equals("平")) {
            // 平：也是去往哪一列
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

        // 5. 组合字符串
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