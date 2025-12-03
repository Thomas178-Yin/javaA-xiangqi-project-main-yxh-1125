package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.app.scene.FXGLMenu;
import com.almasb.fxgl.app.scene.MenuType;
import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.model.ChessBoardModel;
import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.List;

import static com.almasb.fxgl.dsl.FXGL.*;

public class InGameMenuScene extends FXGLMenu {

    private ListView<String> historyListView;

    public InGameMenuScene() {
        super(MenuType.GAME_MENU);

        // 1. 半透明黑色背景遮罩
        var bg = new Rectangle(getAppWidth(), getAppHeight(), Color.web("000", 0.7));

        // 2. 标题
        var title = new Text("游戏暂停");
        title.setFill(Color.WHITE);
        try {
            title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(50));
        } catch (Exception e) {
            title.setFont(javafx.scene.text.Font.font(50));
        }

        // 3. 按钮组 (使用统一的像素风格按钮)

        // [返回游戏]
        var btnResume = new PixelatedButton("返回游戏", "Button1", this::fireResume);

        // [保存游戏] - 调用 App 的保存弹窗
        var btnSave = new PixelatedButton("保存游戏", "Button1", () -> {
            XiangQiApp app = (XiangQiApp) FXGL.getApp();
            app.openSaveDialog();
        });

        // [退出到主菜单]
        var btnExit = new PixelatedButton("退出到主菜单", "Button1", this::fireExitToMainMenu);

        // 4. 布局容器
        var menuBox = new VBox(20, title, btnResume, btnSave, btnExit);
        menuBox.setAlignment(Pos.CENTER);

        // 居中显示
        menuBox.setTranslateX(getAppWidth() / 2.0 - 100); // 稍微修正X轴，因为按钮有宽度
        menuBox.setTranslateY(getAppHeight() / 2.0 - 150);

        // 5. 添加到场景
        getContentRoot().getChildren().addAll(bg, menuBox);
    }

//
//    @Override
//    protected void onUpdate(double tpf) {
//        super.onUpdate(tpf);
//
//        XiangQiApp app = (XiangQiApp) FXGL.getApp();
//
//        // 1. 检查 Model 是否已经初始化
//        if (app.getModel() != null) {
//
//            // 2. 检查 ListView 当前显示的数据，是否就是 Model 里的那份数据
//            // 如果不是（比如刚启动，或者开启了新的一局游戏 Model 换了），就重新绑定
//            if (historyListView.getItems() != app.getModel().getMoveHistoryAsObservableList()) {
//
//                historyListView.setItems(app.getModel().getMoveHistoryAsObservableList());
//                System.out.println("历史记录已同步！当前步数：" + historyListView.getItems().size());
//            }
//        }
//    }


}