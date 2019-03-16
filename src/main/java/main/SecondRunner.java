package main;

import bin.SolutionBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import core.mino.Piece;

import java.util.function.Consumer;

class SecondRunner {
    private final PieceNumberConverter converter;
    private final IndexParser indexParser;
    private final SolutionBinary binary;

    SecondRunner(PieceNumberConverter converter, IndexParser indexParser, SolutionBinary binary) {
        this.converter = converter;
        this.indexParser = indexParser;
        this.binary = binary;
    }

    void run(Consumer<PieceNumber[]> callback) {
        run(new PieceNumber[9], 0, callback);
    }

    private void run(PieceNumber[] numbers, int depth, Consumer<PieceNumber[]> callback) {
        assert numbers.length == 9;
        for (Piece piece : PieceNumberConverter.PPT_PIECES) {
            numbers[depth] = converter.get(piece);
            if (depth == 8) {
                int index = indexParser.parse(numbers);
                if (binary.at(index) != 0) {
                    callback.accept(numbers);
                }
            } else {
                run(numbers, depth + 1, callback);
            }
        }
    }
}
