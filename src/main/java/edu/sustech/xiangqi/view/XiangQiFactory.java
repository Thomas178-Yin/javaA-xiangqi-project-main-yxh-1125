package edu.sustech.xiangqi.view;

import com.almasb.fxgl.texture.Texture;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.texture.Texture;
import edu.sustech.xiangqi.EntityType;
import edu.sustech.xiangqi.XiangQiApp;
import edu.sustech.xiangqi.view.PieceComponent;
import edu.sustech.xiangqi.view.VisualStateComponent;
import edu.sustech.xiangqi.model.*;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;
import static edu.sustech.xiangqi.XiangQiApp.*;

public class XiangQiFactory implements EntityFactory {

    //Spawn
    @Spawns("RedChariot") public Entity newRedChariot(SpawnData data) { return newPiece(data); }
    @Spawns("BlackChariot") public Entity newBlackChariot(SpawnData data) { return newPiece(data); }
    @Spawns("RedHorse") public Entity newRedHorse(SpawnData data) { return newPiece(data); }
    @Spawns("BlackHorse") public Entity newBlackHorse(SpawnData data) { return newPiece(data); }
    @Spawns("RedElephant") public Entity newRedElephant(SpawnData data) { return newPiece(data); }
    @Spawns("BlackElephant") public Entity newBlackElephant(SpawnData data) { return newPiece(data); }
    @Spawns("RedAdvisor") public Entity newRedAdvisor(SpawnData data) { return newPiece(data); }
    @Spawns("BlackAdvisor") public Entity newBlackAdvisor(SpawnData data) { return newPiece(data); }
    @Spawns("RedGeneral") public Entity newRedGeneral(SpawnData data) { return newPiece(data); }
    @Spawns("BlackGeneral") public Entity newBlackGeneral(SpawnData data) { return newPiece(data); }
    @Spawns("RedCannon") public Entity newRedCannon(SpawnData data) { return newPiece(data); }
    @Spawns("BlackCannon") public Entity newBlackCannon(SpawnData data) { return newPiece(data); }
    @Spawns("RedSoldier") public Entity newRedSoldier(SpawnData data) { return newPiece(data); }
    @Spawns("BlackSoldier") public Entity newBlackSoldier(SpawnData data) { return newPiece(data); }


    @Spawns("board")
    public Entity newBoard(SpawnData data) {
        return entityBuilder(data)
                .type(EntityType.BOARD)
                .view("ChessBoard.png")
                .zIndex(-1)
                .build();
    }

    //合法位置显示
    @Spawns("MoveIndicator")
    public Entity newMoveIndicator(SpawnData data) {
        Texture image = FXGL.getAssetLoader().loadTexture("选中.png");
        int padding = 8;
        image.setFitWidth(CELL_SIZE - padding);
        image.setFitHeight(CELL_SIZE - padding);

        return entityBuilder(data)
                .type(EntityType.MOVE_INDICATOR)
                // 居中显示
                .viewWithBBox(image)
                .zIndex(100) //显示在棋子上方
                .build();
    }

    //背景
    @Spawns("background")
    public Entity newBackground(SpawnData data) {
        //背景图片
        Texture bgView = FXGL.getAssetLoader().loadTexture("背景.jpg");



        //拉伸整个窗口一样大
        bgView.setFitWidth(APP_WIDTH);
        bgView.setFitHeight(APP_HEIGHT);

        return entityBuilder(data)
                .type(EntityType.BACKGROUND)
                .view(bgView)
                .zIndex(-2)
                .build();
    }


//      加载棋子图片，匹配
    private Entity newPiece(SpawnData data) {
        //从 SpawnData 中获取传递过来的逻辑棋子对象
        AbstractPiece pieceLogic = data.get("pieceLogic");

        //加载图片？
        String textureName = getTextureName(pieceLogic);

        //Texture 对象
        Texture pieceView = FXGL.getAssetLoader().loadTexture(textureName);

        //边距
        int padding = 8;
        pieceView.setFitWidth(CELL_SIZE - padding);
        pieceView.setFitHeight(CELL_SIZE - padding);

        pieceView.setPreserveRatio(true);

        StackPane view = new StackPane(pieceView);
        view.setPrefSize(CELL_SIZE - 8, CELL_SIZE - 8);


        //使用 entityBuilder 构建实体
        return entityBuilder(data)
                .type(EntityType.PIECE)

                .viewWithBBox(view)
                .with(new PieceComponent(pieceLogic))
                .with(new VisualStateComponent())
                .collidable()
                .build();
    }


//     将逻辑棋子对象映射到它的图片文件名。
//     生成的名字将与你的文件名完全匹配

    private String getTextureName(AbstractPiece pieceLogic) {
        String colorPrefix = pieceLogic.isRed() ? "Red" : "Black";
        String pieceTypeName = pieceLogic.getClass().getSimpleName().replace("Piece", "");

        // 拼接成最终的文件名
        return colorPrefix + pieceTypeName + ".png";
    }
}