package mainv2;

import bin.Movements;
import bin.SolutionShortBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import core.mino.Piece;

import java.util.function.BiConsumer;

class SecondRunner {
    private final PieceNumberConverter converter;
    private final IndexParser indexParser;
    private final SolutionShortBinary binary;

    SecondRunner(PieceNumberConverter converter, IndexParser indexParser, SolutionShortBinary binary) {
        this.converter = converter;
        this.indexParser = indexParser;
        this.binary = binary;
    }

    void run(BiConsumer<PieceNumber[], Short> callback) {
        run(new PieceNumber[9], 0, callback);
    }

    private void run(PieceNumber[] numbers, int depth, BiConsumer<PieceNumber[], Short> callback) {
        assert numbers.length == 9;
        for (Piece piece : PieceNumberConverter.PPT_PIECES) {
            numbers[depth] = converter.get(piece);
            if (depth == 8) {
                int index = indexParser.parse(numbers);
                short step = binary.at(index);
                if (Movements.isPossible(step)) {
                    callback.accept(numbers, step);
                }
            } else {
                run(numbers, depth + 1, callback);
            }
        }
    }
}
