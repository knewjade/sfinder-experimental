package line.step1;

import core.mino.Piece;
import core.neighbor.OriginalPiece;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

class Runner {
    private final HashMap<Long, EnumMap<Piece, List<OriginalPiece>>> maps;

    Runner(FactoryPool pool) {
        this.maps = pool.getBlockMaskMap();
    }

    void run(Result lineObj, Consumer<Result> callback) {
        this.run(Stream.of(lineObj), callback);
    }

    private void run(Stream<Result> stream, Consumer<Result> callback) {
        stream.parallel()
                .forEach(lineObj -> {
                    if (lineObj.isSolution()) {
                        callback.accept(lineObj);
                        return;
                    }

                    if (!lineObj.isCandidate()) {
                        return;
                    }

                    Stream<Result> next = this.next(lineObj);
                    this.run(next, callback);
                });
    }

    private Stream<Result> next(Result lineObj) {
        long minBoard = lineObj.getMinBoard();
        EnumMap<Piece, List<OriginalPiece>> enumMap = maps.get(minBoard);

        return lineObj.getPieceCounter().getBlockStream()
                .flatMap(piece -> {
                    List<OriginalPiece> pieces = enumMap.get(piece);
                    return pieces.stream()
                            .filter(lineObj::canPut)
                            .map(lineObj::next);
                });
    }
}
