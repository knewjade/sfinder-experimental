package third;

import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import core.srs.Rotate;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 3.
 * Tスピンできる地形だけを抽出する
 * 入力地形のうち、T以外のミノだけでライン消去が発生するケースを取り除く
 * 反転形のフィルタリングは行わない
 * <p>
 * 置けるミノに余裕がある場合は、屋根となるミノを探す
 * ライン消去の行数はチェックしない
 */

public class ThirdMain {
    public static void main(String[] args) throws IOException {
        MinoShifter minoShifter = new MinoShifter();

        int maxHeight = 20;

        OriginalPieceFactory factory = new OriginalPieceFactory(maxHeight);
        Set<OriginalPiece> originalPieces = factory.create().stream()
                .filter(originalPiece -> {
                    Piece piece = originalPiece.getPiece();
                    Rotate rotate = originalPiece.getRotate();
                    return minoShifter.createTransformedRotate(piece, rotate) == rotate;
                })
                .collect(Collectors.toSet());

        Map<Piece, List<OriginalPiece>> roofsMap = new EnumMap<>(
                originalPieces.stream().collect(Collectors.groupingBy(OriginalPiece::getPiece))
        );

        Field initField = FieldFactory.createField(maxHeight);

        String base = "output/line34";
        new ThirdRunner(minoShifter, initField, roofsMap, maxHeight).run(base);
    }
}