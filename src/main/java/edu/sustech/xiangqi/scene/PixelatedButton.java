package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.dsl.FXGL;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;

/**
 * 修改说明：
 * 1. 继承 StackPane 而不是 Pane，这样可以直接实现内容自动居中。
 * 2. 强制设置最大/最小宽高，锁定为图片尺寸，防止点击时因为图片切换导致布局跳动。
 */
public class PixelatedButton extends StackPane {

    private ImageView background;
    private Text text;
    private Runnable action;

    public PixelatedButton(String label, String imageName, Runnable action) {
        this.action = action;

        // 1. 预加载图片
        // 确保 "Press.png" 存在于你的 assets/textures 文件夹中，如果没有，请改回 "Button1"
        Image normalImage = FXGL.getAssetLoader().loadTexture(imageName + ".png").getImage();
        Image pressImage = null;
        try {
            pressImage = FXGL.getAssetLoader().loadTexture("Press.png").getImage();
        } catch (Exception e) {
            // 如果没有按下效果图，就用原图，防止报错
            pressImage = normalImage;
        }

        // 2. 初始化背景图
        background = new ImageView(normalImage);
        background.setPreserveRatio(true);

        // 3. 初始化文字
        text = new Text(label);
        // 稍微调整字号，太大可能会撑出去
        text.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(30));
        text.setFill(Color.WHITE);
        // 禁用文字的鼠标事件，确保点击都能点在按钮上
        text.setMouseTransparent(true);

        // 4. 添加到 StackPane (自动居中)
        getChildren().addAll(background, text);

        // 5. 【关键】锁定按钮尺寸
        // 这一步防止了点击时的“画面抖动”和 VBox 布局错乱
        // 使用图片的原始尺寸作为按钮的固定尺寸
        double width = normalImage.getWidth();
        double height = normalImage.getHeight();

        // 如果图片太小或没加载到，给一个默认值防止看不见
        if (width == 0) width = 190;
        if (height == 0) height = 49;

        setMinWidth(width);
        setMinHeight(height);
        setMaxWidth(width);
        setMaxHeight(height);
        setPrefSize(width, height);

        // 6. 交互事件
        Image finalPressImage = pressImage;

        setOnMouseEntered(e -> FXGL.play("按钮音效1.mp3")); // 鼠标悬停音效

        setOnMousePressed(e -> {
            background.setImage(finalPressImage);
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
}