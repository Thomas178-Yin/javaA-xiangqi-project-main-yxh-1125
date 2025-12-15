package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import com.almasb.fxgl.audio.Sound;


public class PixelatedButton extends StackPane {

    private ImageView background;
    private Text text;
    private Runnable action;
    private static Sound clickSound = null;

    public PixelatedButton(String label, String imageName, Runnable action) {
        this.action = action;

        // 1. 预加载图片
        Image normalImage = FXGL.getAssetLoader().loadTexture(imageName + ".png").getImage();
        Image pressImage = null;
        try {
            pressImage = FXGL.getAssetLoader().loadTexture("Press.png").getImage();
        } catch (Exception e) {
            pressImage = normalImage;
        }

        if (clickSound == null) {
                clickSound = FXGL.getAssetLoader().loadSound("按钮音效1.mp3");
        }

        //初始化背景
        background = new ImageView(normalImage);
        background.setPreserveRatio(true);

        //初始化文字
        text = new Text(label);
        //调整字号
        text.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(30));
        text.setFill(Color.WHITE);

        // 禁用文字的鼠标事件，确保点击都能点在按钮上
        text.setMouseTransparent(true);

        getChildren().addAll(background, text);

        // 锁定尺寸
        double width = normalImage.getWidth();
        double height = normalImage.getHeight();

        //以防万一
        if (width == 0) width = 190;
        if (height == 0) height = 49;

        setMinWidth(width);
        setMinHeight(height);
        setMaxWidth(width);
        setMaxHeight(height);
        setPrefSize(width, height);

        //交互事件
        Image finalPressImage = pressImage;

//        setOnMouseEntered(e -> FXGL.play("按钮音效1.mp3")); // 鼠标悬停音效

        setOnMousePressed(e -> {
            background.setImage(finalPressImage);
            if (clickSound != null) {
                FXGL.getAudioPlayer().playSound(clickSound);
            }
            // 按下时文字稍微下沉一点点，增加立体感
            text.setTranslateY(2);
        });

        setOnMouseReleased(e -> {
            background.setImage(normalImage);
            text.setTranslateY(0);
            if (this.action != null) {
                this.action.run();
            }
        });
    }
    //修改文字内容
    public void setText(String content) {
        this.text.setText(content);
    }
    //修改字体大小
    public void setFontSize(double size) {
            this.text.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(size));
    }
    //修改文字颜色
    public void setTextColor(Color color) {
        this.text.setFill(color);
    }
    //Y
    public void setTextY(double y) {
        this.text.setTranslateY(y);
    }
    //X
    public void setTextX(double x) {
        this.text.setTranslateX(x);
    }
}