package bin;

import core.mino.Piece;

import java.util.List;

// Unsupported multi threads
public class IndexParser {
    private static final byte[] PIECE_NUMBERS;

    static {
        Piece[] pieces = Piece.values();
        byte[] bytes = new byte[pieces.length];
        for (Piece piece : pieces) {
            bytes[piece.getNumber()] = (byte) piece.getNumber();
        }
        PIECE_NUMBERS = bytes;
    }

    private final int[] maxPieceNumbers;
    private final int[] startIndexes;
    private final int[] endIndexes;

    public IndexParser(List<Integer> maxIndexList) {
        int sum = maxIndexList.stream().mapToInt(i -> i).sum();

        int[] maxPieceNumbers = new int[sum];
        int[] startIndexes = new int[maxIndexList.size()];
        int[] endIndexes = new int[maxIndexList.size()];

        int current = 0;
        for (int i = 0; i < maxIndexList.size(); i++) {
            Integer maxIndex = maxIndexList.get(i);
            startIndexes[i] = current;
            current += maxIndex;
            endIndexes[i] = current;

            int max = 7;
            for (int j = startIndexes[i]; j < endIndexes[i]; j++) {
                maxPieceNumbers[j] = max;
                max -= 1;
            }
        }

        assert current == sum;

        this.startIndexes = startIndexes;
        this.endIndexes = endIndexes;
        this.maxPieceNumbers = maxPieceNumbers;
    }

    public int parse(Piece[] pieces) {
        int[] buffer = new int[maxPieceNumbers.length];
        for (int rangeIndex = 0, max = startIndexes.length; rangeIndex < max; rangeIndex++) {
            int startIndex = startIndexes[rangeIndex];
            int endIndex = endIndexes[rangeIndex];

            for (int index = startIndex; index < endIndex; index++) {
                byte pieceNumber = getPieceNumber(pieces[index]);
                buffer[index] = pieceNumber;
            }

            for (int index = startIndex; index < endIndex - 1; index++) {
                int b = buffer[index];
                for (int i = index + 1; i < endIndex; i++) {
                    if (b < buffer[i]) {
                        buffer[i] -= 1;
                    }
                }
            }
        }

        int key = buffer[0];
        for (int index = 1; index < buffer.length; index++) {
            key = key * maxPieceNumbers[index] + buffer[index];
        }

        return key;
    }

    private byte getPieceNumber(Piece piece) {
        return PIECE_NUMBERS[piece.getNumber()];
    }
}
