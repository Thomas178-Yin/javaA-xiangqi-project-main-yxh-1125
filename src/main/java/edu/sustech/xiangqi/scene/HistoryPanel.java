package edu.sustech.xiangqi.scene;

import com.almasb.fxgl.dsl.FXGL;
import edu.sustech.xiangqi.model.MoveCommand;
import edu.sustech.xiangqi.util.ChessNotationUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ListCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Stack;

public class HistoryPanel extends VBox {

    //走法记录的列表
    private ListView<MoveCommand> listView;

    //初始化历史记录面板
    public HistoryPanel(double width, double height) {
        //基础设置
        setPrefSize(width, height);
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); -fx-background-radius: 10;");
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_CENTER);

        // 标题
        Text title = new Text("棋谱记录");
        title.setFill(Color.WHITE);
        try {
            title.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(24));
        } catch (Exception e) {
            title.setFont(Font.font(24));
        }

        // 列表
        listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS); // 占满剩余空间

        // 设置列表样式 (透明背景)
        listView.setStyle("-fx-control-inner-background: transparent; -fx-background-color: transparent;");

        //红黑方设置颜色
        listView.setCellFactory(lv -> new ListCell<MoveCommand>() {
            @Override
            protected void updateItem(MoveCommand item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // 获取中文描述
                    String notation = ChessNotationUtils.getNotation(item);

                    // 创建文本
                    Text textNode = new Text(notation);
                    try {
                        textNode.setFont(FXGL.getAssetLoader().loadFont("HYPixel11pxU-2.ttf").newFont(18));
                    } catch (Exception e) {
                        textNode.setFont(Font.font(18));
                    }

                    // 设置颜色
                    if (item.getMovedPiece().isRed()) {
                        textNode.setFill(Color.web("#FF6666")); // 红方淡红
                    } else {
                        textNode.setFill(Color.web("#AAAAAA")); // 黑方淡灰 (纯黑在深色背景看不清)
                    }

                    //文本设置为图形内容
                    setGraphic(textNode);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });

        // 关闭
        PixelatedButton btnClose = new PixelatedButton("关闭", "Button1", () -> this.setVisible(false));
        btnClose.setScaleX(0.8);
        btnClose.setScaleY(0.8);

        getChildren().addAll(title, listView, btnClose);
    }

    //更新列表数据。
    //当游戏中有新落子或悔棋发生时调用此方法，刷新界面显示。
    public void updateHistory(Stack<MoveCommand> history) {
        //清空/添加
        listView.getItems().clear();
        listView.getItems().addAll(history);
        // 自动滚动到底部
        listView.scrollTo(listView.getItems().size() - 1);
    }
}