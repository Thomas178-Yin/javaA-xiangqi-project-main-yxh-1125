package edu.sustech.xiangqi.view;

import com.almasb.fxgl.entity.component.Component;
import edu.sustech.xiangqi.model.AbstractPiece;


// 该组件的作用是返回这个实体是什么棋子
public class PieceComponent extends Component {

    private AbstractPiece pieceLogic;

    public PieceComponent(AbstractPiece pieceLogic) {
        this.pieceLogic = pieceLogic;
    }

    public AbstractPiece getPieceLogic() {
        return pieceLogic;
    }
}