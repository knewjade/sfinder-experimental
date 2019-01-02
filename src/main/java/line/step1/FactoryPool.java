package line.step1;

import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import core.srs.Rotate;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class FactoryPool {
    private final int maxHeight;
    private final int lineY;
    private final MinoFactory minoFactory;
    private final MinoShifter minoShifter;

    FactoryPool(int maxHeight, int lineY) {
        this.maxHeight = maxHeight;
        this.lineY = lineY;
        this.minoFactory = new MinoFactory();
        this.minoShifter = new MinoShifter();
    }

    // 指定したブロックを埋めるすべてのミノを取得する
    // Long=埋めたい1ブロック -> Piece=置くミノの種類 -> List=指定したブロックを埋めるミノ一覧
    HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> getBlockMaskMap() {
        OriginalPieceFactory pieceFactory = new OriginalPieceFactory(maxHeight);
        Set<OriginalPiece> pieces = pieceFactory.create();

        HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> maps = new HashMap<>();

        for (int dy = 0; dy < 3; dy++) {
            int y = lineY + dy;
            for (int x = 0; x < 10; x++) {
                Field field = FieldFactory.createField(maxHeight);
                field.setBlock(x, y);

                EnumMap<Piece, List<OriginalPiece>> enumMap = new EnumMap<>(Piece.class);
                for (Piece piece : Piece.valueList()) {
                    Set<Rotate> uniqueRotates = minoShifter.getUniqueRotates(piece);

                    List<OriginalPiece> piecesList = pieces.stream()
                            .filter(originalPiece -> originalPiece.getPiece() == piece)
                            .filter(originalPiece -> uniqueRotates.contains(originalPiece.getRotate()))
                            .filter(originalPiece -> !field.canPut(originalPiece))
                            .collect(Collectors.toList());

                    enumMap.put(piece, piecesList);
                }

                maps.put(1L << (x + dy * 10), enumMap);
            }
        }

        return maps;
    }
}
