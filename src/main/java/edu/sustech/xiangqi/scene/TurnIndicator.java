package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.dsl.FXGL;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;

public class TurnIndicator extends StackPane {

    private Text text;

    public TurnIndicator() {
        //加载背景图片
        var bg = new ImageView(FXGL.getAssetLoader().loadTexture("Button1.png").getImage());

        //文本
        text = new Text();
        text.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(28));

        //文本居中
        setAlignment(Pos.CENTER);
        getChildren().addAll(bg, text);
    }

    /**
     * 更新指示器的状态。
     * @param isRedTurn true表示轮到红方，false表示轮到黑方
     * @param isGameOver true表示游戏结束
     */
    public void update(boolean isRedTurn, boolean isGameOver) {
        if (isGameOver) {
            text.setText("游戏结束");
            text.setFill(Color.GRAY);
        } else if (isRedTurn) {
            text.setText("轮到 红方");
            text.setFill(Color.RED);
        } else {
            text.setText("轮到 黑方");
            text.setFill(Color.BLACK);
        }
    }
}