package utils.index;

import utils.pieces.PieceNumber;

import java.util.List;

public class IndexParser {
    private final int[] startIndexes;
    private final int[] endIndexes;
    private final int[] scales;
    private final int max;

    public IndexParser(List<Integer> maxIndexList) {
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
        this.max = scales[0] * 7;

        assert scales[scales.length - 1] * 7 <= Integer.MAX_VALUE;
    }

    public int parse(PieceNumber[] pieces) {
        int key = 0;
        for (int rangeIndex = 0, max = startIndexes.length; rangeIndex < max; rangeIndex++) {
            int startIndex = startIndexes[rangeIndex];
            int endIndex = endIndexes[rangeIndex];

            for (int index = startIndex; index < endIndex; index++) {
                int current = pieces[index].getNumber();
                key += current * scales[index];

                for (int j = index + 1; j < endIndex; j++) {
                    if (current < pieces[j].getNumber()) {
                        key -= scales[j];
                    }
                }
            }
        }

        return key;
    }

    // Max index (exclude)  ex) 7 = used 0-6
    public int getMax() {
        return max;
    }
}
