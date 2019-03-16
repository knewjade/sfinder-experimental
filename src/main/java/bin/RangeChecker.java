package bin;

import core.mino.Piece;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RangeChecker {
    private final EnumMap<Piece, Byte> pieceToFlag;
    private final int[] startIndexes;
    private final int[] endIndexes;

    public RangeChecker(EnumMap<Piece, Integer> pieceToNumber, List<Integer> maxIndexList) {
        EnumMap<Piece, Byte> pieceToFlag = new EnumMap<>(Piece.class);
        for (Map.Entry<Piece, Integer> entry : pieceToNumber.entrySet()) {
            pieceToFlag.put(entry.getKey(), (byte) (1 << entry.getValue()));
        }
        this.pieceToFlag = pieceToFlag;

        int sum = maxIndexList.stream().mapToInt(i -> i).sum();

        int[] startIndexes = new int[maxIndexList.size()];
        int[] endIndexes = new int[maxIndexList.size()];

        int current = 0;
        for (int index = 0; index < maxIndexList.size(); index++) {
            int maxIndex = maxIndexList.get(index);
            startIndexes[index] = current;
            current += maxIndex;
            endIndexes[index] = current;
        }

        assert current == sum;

        this.startIndexes = startIndexes;
        this.endIndexes = endIndexes;
    }

    public boolean check(Piece[] pieces) {
        for (int rangeIndex = 0, max = startIndexes.length; rangeIndex < max; rangeIndex++) {
            int startIndex = startIndexes[rangeIndex];
            int endIndex = endIndexes[rangeIndex];

            byte flags = 0;
            for (int index = startIndex; index < endIndex; index++) {
                Piece piece = pieces[index];
                byte flag = getPieceFlag(piece);

                if ((flags & flag) != 0) {
                    return false;
                }

                flags |= flag;
            }
        }

        return true;
    }

    private byte getPieceFlag(Piece piece) {
        return pieceToFlag.get(piece);
    }
}
