package edu.sustech.xiangqi.view;

import com.almasb.fxgl.entity.component.Component;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;


//控制棋子颜色
public class VisualStateComponent extends Component {

    private ColorAdjust colorAdjust = new ColorAdjust();
    private boolean isActive = true;

    @Override
    public void onAdded() {
        entity.getViewComponent().getParent().setEffect(colorAdjust);

        //初始化
        setNormal();
    }

    //标记选中棋子
    public void setInactive() {
        colorAdjust.setBrightness(-0.5); // Lower brightness, -1.0 is black
        isActive = false;
    }

    //恢复
    public void setNormal() {
        colorAdjust.setBrightness(0.0); // 0.0 is the default, normal brightness
        isActive = true;
    }

    //判断
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void onRemoved() {
        //清理
        entity.getViewComponent().getParent().setEffect(null);
    }
}