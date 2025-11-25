package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import javafx.animation.FillTransition;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.List;

public class MainMenuScene extends FXGLMenu {

    public MainMenuScene() {
        super(MenuType.MAIN_MENU);

        // --- 1. 背景 (保持不变) ---
        var bgStops = List.of(new Stop(0, Color.web("#D3B08C")), new Stop(1, Color.web("#4A2C12")));
        var bgGradient = new RadialGradient(0.5, 0.5, 0.5, 0.5, 1, true, CycleMethod.NO_CYCLE, bgStops);
        getContentRoot().setBackground(new Background(new BackgroundFill(bgGradient, null, null)));

        var rect = new Rectangle(getAppWidth(), getAppHeight(), Color.web("000", 0.0));
        rect.setMouseTransparent(true);
        FillTransition ft = new FillTransition(Duration.seconds(3), rect, Color.TRANSPARENT, Color.web("000", 0.2));
        ft.setCycleCount(-1);
        ft.setAutoReverse(true);
        ft.play();

        // --- 2. 游戏标题 (保持不变) ---
        var title = new Text("中国象棋");
        title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(120));
        title.setFill(Color.web("#F0E68C"));
        title.setStroke(Color.web("#5C3A1A"));
        title.setStrokeWidth(3);
        title.setEffect(new DropShadow(15, Color.BLACK));

        // --- 3. 按钮区域 (【修改】增加自定义局按钮，调整间距) ---

        // 普通开始游戏
        // 修改“标准对战”按钮的逻辑
        var btnNewGame = new PixelatedButton("标准对战", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();

            // 【修改】确保两个标记都是 false
            app.setCustomMode(false);
            app.setLoadedGame(false); // 关键：告诉 initGame 我要新的

            fireNewGame();
        });

        // 【新增】自定义模式按钮
        var btnCustomGame = new PixelatedButton("排局模式", "Button1", () -> {
            // 获取 App 实例并调用自定义开始方法
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.setCustomMode(true); // 标记为自定义模式
            fireNewGame(); // 触发游戏开始流程（会在 initGame 中进行分支处理）
        });

        // 【修改】连接读取存档功能
        var btnLoadGame = new PixelatedButton("读取存档", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.openLoadDialog();
        });
        var btnOnline = new PixelatedButton("联网对战", "Button1", () -> {
            System.out.println("联网对战功能待实现...");
        });

        var btnExit = new PixelatedButton("退出游戏", "Button1", this::fireExit);

        // --- 4. 整体布局 (【修改】间距从 15 调整为 10，防止按钮过多超出屏幕) ---
        var titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER);

        var menuBox = new VBox(10, btnNewGame, btnCustomGame, btnLoadGame, btnOnline, btnExit);
        menuBox.setAlignment(Pos.CENTER);

        var mainLayout = new VBox(30, titleBox, menuBox); // 标题和菜单之间的间距也稍微调小
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setTranslateX((getAppWidth() - mainLayout.getBoundsInLocal().getWidth()) / 2);
        // 稍微往上提一点，保证下面放得下
        mainLayout.setTranslateY((getAppHeight() - mainLayout.getBoundsInLocal().getHeight()) / 2 - 250);

        getContentRoot().getChildren().addAll(rect, mainLayout);
    }
}