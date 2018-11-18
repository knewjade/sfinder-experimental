package second;

import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.PieceCounter;
import common.datastore.SimpleOperation;
import common.parser.OperationWithKeyInterpreter;
import commons.CandidateObj;
import commons.Commons;
import concurrent.LockedReachableThreadLocal;
import core.action.reachable.LockedReachable;
import core.field.Field;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SecondRunner {
    private static final PieceCounter PIECE_COUNTER_WITH_ALL = new PieceCounter(Piece.valueList());

    private final LockedReachableThreadLocal reachableThreadLocal;
    private final Map<SimpleOperation, Scaffolds> scaffoldsMap;
    private final Field initField;
    private final int maxHeight;

    SecondRunner(LockedReachableThreadLocal reachableThreadLocal, Map<SimpleOperation, Scaffolds> scaffoldsMap, Field initField, int maxHeight) {
        this.reachableThreadLocal = reachableThreadLocal;
        this.scaffoldsMap = scaffoldsMap;
        this.initField = initField;
        this.maxHeight = maxHeight;
    }

    void run(String base) throws IOException {
        MinoFactory minoFactory = new MinoFactory();
        MyFile myFile = new MyFile(base + "_expand");

        AtomicInteger counter = new AtomicInteger();
        try (AsyncBufferedFileWriter writer = myFile.newAsyncWriter()) {
            Files.lines(Paths.get(base + "_all")).parallel()
                    .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                    .peek(e -> {
                        int i = counter.incrementAndGet();
                        if (i % 10000 == 0) System.out.println(i);
                    })
                    .filter(operations -> operations.stream().anyMatch(operation -> operation.getPiece() == Piece.T))
                    .filter(operations -> {
                        // Tを抜いたらライン消去が発生しないこと
                        List<MinoOperationWithKey> minoOperationWithKeys = operations.stream()
                                .filter(operation -> operation.getPiece() != Piece.T)
                                .collect(Collectors.toList());
                        Field field = Commons.toField(minoOperationWithKeys, initField, maxHeight);
                        return field.clearLine() == 0;
                    })
                    .map(operations -> Commons.slideDownToGround(operations, initField, maxHeight))
                    .map(operationWithKeys -> new CandidateObj(operationWithKeys, Collections.emptyList()))
                    .flatMap(this::test)
                    .filter(candidate -> {
                        // Tを含めて組むことができるとき
                        List<MinoOperationWithKey> allOperations = candidate.getAllOperations();
                        LockedReachable reachable = reachableThreadLocal.get();
                        return BuildUp.existsValidBuildPattern(initField, allOperations, maxHeight, reachable);
                    })
                    .forEach(candidate -> writer.writeAndNewLine(OperationWithKeyInterpreter.parseToString(candidate.getAllOperations())));
        }
    }

    private Stream<CandidateObj> test(CandidateObj candidate) {
        List<MinoOperationWithKey> withoutTOperations = candidate.getWithoutTOperations();

        Stream.Builder<CandidateObj> builder = Stream.builder();

        // Tを使わずに組むことができるとき
        LockedReachable reachable = reachableThreadLocal.get();
        if (BuildUp.existsValidBuildPattern(initField, withoutTOperations, maxHeight, reachable)) {
            // 地形が確定しても、足場をさらに追加できるケースがあるため、探索は続ける
            builder.accept(candidate);
        }

        // これ以上おくことができないとき
        if (withoutTOperations.size() == 6) {
            return builder.build();
        }

        // 上にスライドして足場をおくスペースを確保する
        CandidateObj slidedCandidate = Commons.slideUpCandidateTo4(candidate, initField, maxHeight);
        List<MinoOperationWithKey> airOperations = slidedCandidate.getAllOperations();

        // 未使用のミノを求める
        PieceCounter counter = slidedCandidate.getUsedPieceCounter();
        PieceCounter noUsed = PIECE_COUNTER_WITH_ALL.removeAndReturnNew(counter);

        // すべての操作を含む地形
        Field field = Commons.toField(airOperations, initField, maxHeight);

        // 次におくことができるミノを求める
        Set<OriginalPiece> nextPieces = airOperations.stream()
                .filter(operation -> {  // 空中に浮いているミノを列挙する
                    Field freeze = field.freeze(maxHeight);
                    Mino mino = operation.getMino();
                    int x = operation.getX();
                    int y = operation.getY();
                    freeze.remove(mino, x, y);
                    return !freeze.isOnGround(mino, x, y);
                })
                .flatMap(operation -> noUsed.getBlockStream().flatMap(piece -> {  // 足場を列挙する
                    SimpleOperation key = new SimpleOperation(
                            operation.getPiece(), operation.getRotate(), operation.getX(), operation.getY()
                    );

                    return scaffoldsMap.get(key).get(piece)
                            .stream()
                            .filter(field::canPut);
                }))
                .collect(Collectors.toSet());

        // 現在の操作に加える
        return Stream.concat(builder.build(),
                nextPieces.stream()
                        .filter(originalPiece -> {
                            // 他の足場のミノを抜いたら組み立てられない（すべて必要なミノである）
                            return candidate.addNextPiece(originalPiece).getMinusOneOperationsStream()
                                    .allMatch(operationWithKeys -> {
                                        List<MinoOperationWithKey> groundOperations = Commons.slideDownToGround(operationWithKeys, initField, maxHeight);
                                        LockedReachable lockedReachable = reachableThreadLocal.get();
                                        return !BuildUp.existsValidBuildPattern(initField, groundOperations, maxHeight, lockedReachable);
                                    });
                        })
                        .map(originalPiece -> {
                            CandidateObj nextCandidate = slidedCandidate.addNextPiece(originalPiece);

                            Field freeze = field.freeze(maxHeight);
                            freeze.put(originalPiece);

                            // 一番下が埋まるようにスライドする
                            int lowerY = freeze.getLowerY();
                            return nextCandidate.slideDown(lowerY);
                        })
                        .flatMap(this::test)
        );
    }
}