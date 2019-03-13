package square4x10;

import common.datastore.FullOperationWithKey;
import common.parser.OperationTransform;
import common.parser.StringEnumTransform;
import core.field.KeyOperators;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.Rotate;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class IndexPiecePairs {
    static Map<Integer, IndexPiecePair> create(Path path, MinoFactory minoFactory, int fieldHeight) throws IOException {
        return create(Files.lines(path), minoFactory, fieldHeight);
    }

    private static Map<Integer, IndexPiecePair> create(Stream<String> lines, MinoFactory minoFactory, int fieldHeight) {
        return lines
                .map(line -> getIndexPiecePair(minoFactory, line.split(","), fieldHeight))
                .collect(Collectors.toMap(IndexPiecePair::getIndex, it -> it));
    }

    private static IndexPiecePair getIndexPiecePair(MinoFactory minoFactory, String[] split, int fieldHeight) {
        int index = Integer.parseInt(split[0]);
        Piece piece = StringEnumTransform.toPiece(split[1]);
        Rotate rotate = StringEnumTransform.toRotate(split[2]);
        Mino mino = minoFactory.create(piece, rotate);
        int x = Integer.parseInt(split[3]);
        int lowerY = Integer.parseInt(split[4]);
        int y = lowerY - mino.getMinY();

        long needDeletedKey = parseToKey(split[6]);

        FullOperationWithKey operationWithKey = OperationTransform.toFullOperationWithKey(mino, x, y, needDeletedKey);
        SimpleOriginalPiece operation = new SimpleOriginalPiece(operationWithKey, fieldHeight);

        return new IndexPiecePair(index, operation);
    }

    private static long parseToKey(String deletedLine) {
        long needDeletedKey = 0L;
        int height = deletedLine.length();
        for (int index = 0; index < height; index += 1) {
            char ch = deletedLine.charAt(index);
            assert ch == '0' || ch == '1' : ch;
            if (ch == '1') {
                needDeletedKey |= KeyOperators.getBitKey(height - index - 1);
            }
        }
        return needDeletedKey;
    }
}
