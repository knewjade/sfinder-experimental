package main.second;

import utils.bin.SolutionByteBinary;
import utils.pieces.PieceNumber;

import java.util.List;

interface BinaryOutput {
    void output(PieceNumberStep pieceNumberStep);

    SolutionByteBinary get();

    default boolean[] countHolds(PieceNumber[] numbers, List<Integer> afterIndexes, boolean holdEmpty) {
        boolean[] holds = new boolean[numbers.length];

        // ホールドに何かあり、それが最後まで持っているものならホールドは発生しない
        if (!holdEmpty && afterIndexes.get(0) == null) {
            return holds;
        }

        int hold = holdEmpty ? -1 : afterIndexes.get(0);
        int expect = 0;  // ホールドなしで置けるはずのindex
        for (int i = holdEmpty ? 0 : 1; i < afterIndexes.size(); i++) {
            Integer element = afterIndexes.get(i);

            if (element == null) {
                // ホールドする

                if (hold != -1) {
                    // ホールドに入っているミノはホールドありになる
                    assert hold == expect;
                    holds[hold] = true;
                } else {
                    // 次のミノはホールドありになる
                    i += 1;
                    if (i < afterIndexes.size()) {
                        Integer element2 = afterIndexes.get(i);
                        assert element2 == expect;
                        holds[element2] = true;
                    }
                }

                expect += 1;

                // このあとはホールドは使わない
                for (int j = i + 1; j < afterIndexes.size(); j++) {
                    Integer element2 = afterIndexes.get(j);
                    assert element2 == expect;
                    holds[element2] = false;
                    expect += 1;
                }

                break;
            }

            if (element == expect) {
                // ホールドなしで置く
                holds[element] = false;
                expect += 1;
            } else if (hold == expect) {
                // ホールドを取り出して置く
                // ホールドにあったミノにマークする
                holds[hold] = true;
                hold = element;
                expect += 1;
            } else if (hold == -1) {
                // ホールドする
                hold = element;

                i += 1;

                if (i < afterIndexes.size()) {
                    // 次にツモるミノにマークする
                    Integer element2 = afterIndexes.get(i);
                    assert element2 == expect;
                    holds[element2] = true;
                }

                expect += 1;
            } else {
                throw new IllegalStateException();
            }
        }

        return holds;
    }
}