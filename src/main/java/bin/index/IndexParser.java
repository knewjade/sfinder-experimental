package bin.index;

import core.mino.Piece;

import java.util.EnumMap;
import java.util.List;

public class IndexParser {
    private final EnumMap<Piece, Integer> pieceToNumber;
    private final int[] startIndexes;
    private final int[] endIndexes;
    private final int[] scales;

    public IndexParser(EnumMap<Piece, Integer> pieceToNumber, List<Integer> maxIndexList) {
        this.pieceToNumber = pieceToNumber;

        int sum = maxIndexList.stream().mapToInt(i -> i).sum();

        int[] maxPieceNumbers = new int[sum];
        int[] startIndexes = new int[maxIndexList.size()];
        int[] endIndexes = new int[maxIndexList.size()];

        int current = 0;
        for (int index = 0; index < maxIndexList.size(); index++) {
            int maxIndex = maxIndexList.get(index);
            startIndexes[index] = current;
            current += maxIndex;
            endIndexes[index] = current;

            int max = 7;
            for (int j = startIndexes[index]; j < endIndexes[index]; j++) {
                maxPieceNumbers[j] = max;
                max -= 1;
            }
        }

        assert current == sum;

        this.startIndexes = startIndexes;
        this.endIndexes = endIndexes;

        // scale
        int[] scales = new int[sum];
        int scale = 1;
        for (int index = sum - 1; 0 <= index; index -= 1) {
            scales[index] = scale;
            scale *= maxPieceNumbers[index];
        }

        this.scales = scales;
    }

    public int parse(Piece[] pieces) {
        int[] ints = toInts(pieces);

        int key = 0;
        for (int rangeIndex = 0, max = startIndexes.length; rangeIndex < max; rangeIndex++) {
            int startIndex = startIndexes[rangeIndex];
            int endIndex = endIndexes[rangeIndex];

            for (int index = startIndex; index < endIndex; index++) {
                int current = ints[index];
                key += current * scales[index];

                for (int j = index + 1; j < endIndex; j++) {
                    if (current < ints[j]) {
                        key -= scales[j];
                    }
                }
            }
        }

        return key;
    }

    private int[] toInts(Piece[] pieces) {
        int length = pieces.length;
        int[] ints = new int[length];
        for (int index = 0; index < length; index++) {
            ints[index] = getPieceNumber(pieces[index]);
        }
        return ints;
    }

    private int getPieceNumber(Piece piece) {
        return pieceToNumber.get(piece);
    }
}
