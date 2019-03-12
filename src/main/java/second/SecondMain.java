package second;

import common.datastore.Pair;
import common.datastore.SimpleOperation;
import concurrent.LockedReachableThreadLocal;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import core.srs.MinoRotation;
import core.srs.Rotate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 2.
 * ライン消去にTミノが絡まないケースを取り除く
 * 入力地形のうち、T以外のミノだけでライン消去が発生するケースを取り除く
 * <p>
 * ライン消去できるパターンを、足場を含めて列挙する
 * T以外のミノだけで地形を組み立てることができる
 * 足場を追加することによってライン消去が発生しているかは確認しない
 */

public class SecondMain {
    public static void main(String[] args) throws IOException {
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();
        int maxHeight = 20;
        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);

        OriginalPieceFactory factory = new OriginalPieceFactory(maxHeight);
        Set<OriginalPiece> originalPieces = factory.createPieces().stream()
                .filter(originalPiece -> {
                    Piece piece = originalPiece.getPiece();
                    Rotate rotate = originalPiece.getRotate();
                    return minoShifter.createTransformedRotate(piece, rotate) == rotate;
                })
                .collect(Collectors.toSet());

        Map<SimpleOperation, Scaffolds> scaffoldsMap = originalPieces.stream()
                .map(originalPiece -> {
                    Field minoField = originalPiece.getMinoField();
                    List<OriginalPiece> scaffolds = originalPieces.stream()
                            .filter(minoField::canPut)
                            .filter(scaffold -> scaffold.getMinoField().isOnGround(
                                    originalPiece.getMino(), originalPiece.getX(), originalPiece.getY()
                            ))
                            .collect(Collectors.toList());
                    SimpleOperation operation = new SimpleOperation(originalPiece.getPiece(), originalPiece.getRotate(), originalPiece.getX(), originalPiece.getY());
                    return new Pair<>(operation, new Scaffolds(scaffolds));
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        Field initField = FieldFactory.createField(maxHeight);

        String base = "output/line34";
        new SecondRunner(reachableThreadLocal, scaffoldsMap, initField, maxHeight).run(base);
    }
}

