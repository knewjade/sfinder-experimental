package bin;

import bin.pieces.PieceNumber;

import java.util.List;

public class RangeChecker {
    private final int[] startIndexes;
    private final int[] endIndexes;

    public RangeChecker(List<Integer> maxIndexList) {
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

    public boolean check(PieceNumber[] pieces) {
        for (int rangeIndex = 0, max = startIndexes.length; rangeIndex < max; rangeIndex++) {
            int startIndex = startIndexes[rangeIndex];
            int endIndex = endIndexes[rangeIndex];

            byte flags = 0;
            for (int index = startIndex; index < endIndex; index++) {
                PieceNumber piece = pieces[index];
                byte flag = piece.getBitByte();

                if ((flags & flag) != 0) {
                    return false;
                }

                flags |= flag;
            }
        }

        return true;
    }
}
