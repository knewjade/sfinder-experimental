package mainv2;

import bin.Movements;
import bin.SolutionIntBinary;
import bin.SolutionShortBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import core.mino.Piece;

import java.util.Objects;
import java.util.function.BiConsumer;

@FunctionalInterface
interface TriConsumer<T, U, V> {
    void accept(T t, U u, V v);
}

class SecondRunner {
    private final PieceNumberConverter converter;
    private final IndexParser indexParser;
    private final SolutionShortBinary binary;
    private final SolutionIntBinary solutionsBinary;

    SecondRunner(
            PieceNumberConverter converter, IndexParser indexParser,
            SolutionShortBinary binary, SolutionIntBinary solutionsBinary
    ) {
        this.converter = converter;
        this.indexParser = indexParser;
        this.binary = binary;
        this.solutionsBinary = solutionsBinary;
    }

    void run(TriConsumer<PieceNumber[], Short, Integer> callback) {
        run(new PieceNumber[9], 0, callback);
    }

    private void run(PieceNumber[] numbers, int depth, TriConsumer<PieceNumber[], Short, Integer> callback) {
        assert numbers.length == 9;
        for (Piece piece : PieceNumberConverter.PPT_PIECES) {
            numbers[depth] = converter.get(piece);
            if (depth == 8) {
                int index = indexParser.parse(numbers);
                short step = binary.at(index);
                if (Movements.isPossible(step)) {
                    int solutionIndex = solutionsBinary.at(index);
                    callback.accept(numbers, step, solutionIndex);
                }
            } else {
                run(numbers, depth + 1, callback);
            }
        }
    }
}
