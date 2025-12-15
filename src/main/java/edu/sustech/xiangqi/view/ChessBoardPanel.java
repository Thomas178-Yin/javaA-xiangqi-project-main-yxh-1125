package edu.sustech.xiangqi.view;

import edu.sustech.xiangqi.model.ChessBoardModel;
import edu.sustech.xiangqi.model.AbstractPiece;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChessBoardPanel extends JPanel {
    private final ChessBoardModel model;


    //单个棋盘格子的尺寸（px）
    private static final int CELL_SIZE = 64;
    //棋盘边界与窗口边界的边距
    private static final int MARGIN = 40;
    //棋子半径
    private static final int PIECE_RADIUS = 25;

    private AbstractPiece selectedPiece = null;

    public ChessBoardPanel(ChessBoardModel model) {
        this.model = model;
        setPreferredSize(new Dimension(
                CELL_SIZE * (ChessBoardModel.getCols() - 1) + MARGIN * 2,
                CELL_SIZE * (ChessBoardModel.getRows() - 1) + MARGIN * 2
        ));
        //背景
        setBackground(new Color(220, 179, 92));

        //鼠标
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    //鼠标点击
    private void handleMouseClick(int x, int y) {
        //像素——棋盘
        int col = Math.round((float)(x - MARGIN) / CELL_SIZE);
        int row = Math.round((float)(y - MARGIN) / CELL_SIZE);

        //判断
        if (!model.isValidPosition(row, col)) {
            return;
        }

        //选择棋子/尝试移动
        if (selectedPiece == null) {
            selectedPiece = model.getPieceAt(row, col);
        } else {
            model.movePiece(selectedPiece, row, col);
            selectedPiece = null;
        }

        //repaint
        repaint();
    }

    //自定义绘制
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBoard(g2d);         //棋盘
        drawPieces(g2d);        //棋子
        if (model.isGameOver()) {
            drawGameOverScreen(g2d);
        }

        drawTurnIndicator(g2d);
    }


    //当前回合的指示
    private void drawTurnIndicator(Graphics2D g) {
        // if end ,不再显示回合信息
        if (model.isGameOver()) {
            return;
        }

        String turnText;
        if (model.isRedTurn()) {
            g.setColor(new Color(200, 0, 0)); // 红色
            turnText = "轮到 红方 走棋";
        } else {
            g.setColor(Color.BLACK); //黑色
            turnText = "轮到 黑方 走棋";
        }

//        字体设置
        g.setFont(new Font("楷体", Font.BOLD, 20));

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(turnText);
        int x = (getWidth() - textWidth) / 2;
        int y = MARGIN - 20; // 棋盘边距-?

        g.drawString(turnText, x, y);
    }


    //游戏结束界面
    private void drawGameOverScreen(Graphics2D g) {
        //半透明的黑色遮罩
        g.setColor(new Color(0, 0, 0, 120)); // 最后一个参数 120 是透明度
        g.fillRect(0, 0, getWidth(), getHeight());

        //颜色和字体
        g.setColor(Color.WHITE);
        g.setFont(new Font("楷体", Font.BOLD, 50));

        //胜利信息
        String message = model.getWinner() + " 胜利!";

        //计算位置
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(message);
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

        g.drawString(message, x, y);
    }

    //绘制
    private void drawBoard(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));

        // 绘制横线
        for (int i = 0; i < ChessBoardModel.getRows(); i++) {
            int y = MARGIN + i * CELL_SIZE;
            g.drawLine(MARGIN, y, MARGIN + (ChessBoardModel.getCols() - 1) * CELL_SIZE, y);
        }

        // 绘制竖线
        for (int i = 0; i < ChessBoardModel.getCols(); i++) {
            int x = MARGIN + i * CELL_SIZE;
            if (i == 0 || i == ChessBoardModel.getCols() - 1) {
                // 两边的竖线贯通整个棋盘
                g.drawLine(x, MARGIN, x, MARGIN + (ChessBoardModel.getRows() - 1) * CELL_SIZE);
            } else {
                // 中间的竖线分为上下两段（楚河汉界断开）
                g.drawLine(x, MARGIN, x, MARGIN + 4 * CELL_SIZE);
                g.drawLine(x, MARGIN + 5 * CELL_SIZE, x, MARGIN + (ChessBoardModel.getRows() - 1) * CELL_SIZE);
            }
        }

        // 绘制“楚河”和“汉界”这两个文字
        g.setColor(Color.BLACK);
        g.setFont(new Font("楷体", Font.BOLD, 24));

        int riverY = MARGIN + 4 * CELL_SIZE + CELL_SIZE / 2;

        String chuHeText = "楚河";
        FontMetrics fm = g.getFontMetrics();
        int chuHeWidth = fm.stringWidth(chuHeText);
        g.drawString(chuHeText, MARGIN + CELL_SIZE * 2 - chuHeWidth / 2, riverY + 8);

        String hanJieText = "汉界";
        int hanJieWidth = fm.stringWidth(hanJieText);
        g.drawString(hanJieText, MARGIN + CELL_SIZE * 6 - hanJieWidth / 2, riverY + 8);
    }

    //棋子绘制
    private void drawPieces(Graphics2D g) {
        // 遍历棋子循环绘制该棋子
        for (AbstractPiece piece : model.getPieces()) {
            // 计算坐标
            int x = MARGIN + piece.getCol() * CELL_SIZE;
            int y = MARGIN + piece.getRow() * CELL_SIZE;

            boolean isSelected = (piece == selectedPiece);

            // circle
            g.setColor(new Color(245, 222, 179));
            g.fillOval(x - PIECE_RADIUS, y - PIECE_RADIUS, PIECE_RADIUS * 2, PIECE_RADIUS * 2);
            // 黑色边框
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - PIECE_RADIUS, y - PIECE_RADIUS, PIECE_RADIUS * 2, PIECE_RADIUS * 2);

            if (isSelected) {
                drawCornerBorders(g, x, y);
            }

            // +棋子名字
            if (piece.isRed()) {
                g.setColor(new Color(200, 0, 0));
            } else {
                g.setColor(Color.BLACK);
            }
            g.setFont(new Font("楷体", Font.BOLD, 22));
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(piece.getName());
            int textHeight = fm.getAscent();
            g.drawString(piece.getName(), x - textWidth / 2, y + textHeight / 2 - 2);
        }
    }

    //选中边框
    private void drawCornerBorders(Graphics2D g, int centerX, int centerY) {
        g.setColor(new Color(0, 100, 255));
        g.setStroke(new BasicStroke(3));

        int cornerSize = 32;
        int lineLength = 12;


        // 左上角的边框
        g.drawLine(centerX - cornerSize, centerY - cornerSize,
                centerX - cornerSize + lineLength, centerY - cornerSize);
        g.drawLine(centerX - cornerSize, centerY - cornerSize,
                centerX - cornerSize, centerY - cornerSize + lineLength);

        // 右上角的边框
        g.drawLine(centerX + cornerSize, centerY - cornerSize,
                centerX + cornerSize - lineLength, centerY - cornerSize);
        g.drawLine(centerX + cornerSize, centerY - cornerSize,
                centerX + cornerSize, centerY - cornerSize + lineLength);

        // 左下角的边框
        g.drawLine(centerX - cornerSize, centerY + cornerSize,
                centerX - cornerSize + lineLength, centerY + cornerSize);
        g.drawLine(centerX - cornerSize, centerY + cornerSize,
                centerX - cornerSize, centerY + cornerSize - lineLength);

        // 右下角的边框
        g.drawLine(centerX + cornerSize, centerY + cornerSize,
                centerX + cornerSize - lineLength, centerY + cornerSize);
        g.drawLine(centerX + cornerSize, centerY + cornerSize,
                centerX + cornerSize, centerY + cornerSize - lineLength);
    }



}
