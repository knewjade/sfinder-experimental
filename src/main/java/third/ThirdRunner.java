package third;

import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.PieceCounter;
import common.parser.OperationWithKeyInterpreter;
import commons.CandidateObj;
import commons.Commons;
import commons.RotateReachableThreadLocal;
import concurrent.LockedReachableThreadLocal;
import core.action.reachable.LockedReachable;
import core.action.reachable.RotateReachable;
import core.field.Field;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.srs.MinoRotation;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class ThirdRunner {
    private static final PieceCounter PIECE_COUNTER_WITH_ALL = new PieceCounter(Piece.valueList());

    private final int maxHeight;
    private final Field initField;
    private final Map<Piece, List<OriginalPiece>> roofsMap;
    private final MinoFactory minoFactory;
    private final RotateReachableThreadLocal rotateReachableThreadLocal;
    private final LockedReachableThreadLocal lockedReachableThreadLocal;

    ThirdRunner(MinoShifter minoShifter, Field initField, Map<Piece, List<OriginalPiece>> roofsMap, int maxHeight) {
        this.initField = initField;
        this.roofsMap = roofsMap;
        this.maxHeight = maxHeight;
        this.minoFactory = new MinoFactory();

        MinoRotation minoRotation = new MinoRotation();

        this.rotateReachableThreadLocal = new RotateReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);
        this.lockedReachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);
    }

    void run(String base) throws IOException {
        MyFile myFile = new MyFile(base + "_solutions");

        AtomicInteger counter = new AtomicInteger();
        try (AsyncBufferedFileWriter writer = myFile.newAsyncWriter()) {
            Files.lines(Paths.get(base + "_expand"))
                    .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                    .peek(e -> {
                        int i = counter.incrementAndGet();
                        if (i % 10000 == 0) System.out.println(i);
                    })
                    .filter(operationWithKeys -> {  // Tが絡まないライン消去が発生しない
                        Field field = Commons.toField(operationWithKeys, initField, maxHeight);
                        Field freeze = field.freeze(maxHeight);
                        Optional<MinoOperationWithKey> first = operationWithKeys.stream().filter(operation -> operation.getPiece() == Piece.T).findFirst();
                        MinoOperationWithKey key = first.get();
                        freeze.remove(key.getMino(), key.getX(), key.getY());
                        int line2 = freeze.clearLine();
                        return line2 == 0;
                    })
                    .map(operationWithKeys -> new CandidateObj(operationWithKeys, Collections.emptyList()))
                    .flatMap(this::test)
                    .filter(candidate -> {
                        // Tを含めて組むことができるとき
                        List<MinoOperationWithKey> allOperations = candidate.getAllOperations();
                        LockedReachable reachable = lockedReachableThreadLocal.get();
                        return BuildUp.existsValidBuildPattern(initField, allOperations, maxHeight, reachable);
                    })
                    .forEach(candidate -> writer.writeAndNewLine(OperationWithKeyInterpreter.parseToString(candidate.getAllOperations())));
        }
    }

    private Stream<CandidateObj> test(CandidateObj candidate) {
        // 条件を満たしているとき
        if (checks(candidate)) {
            return Stream.of(candidate);
        }

        // すでに7ミノ使用している
        if (candidate.size() == 7) {
            return Stream.empty();
        }

        return expand(candidate);
    }

    private boolean checks(CandidateObj candidate) {
        List<MinoOperationWithKey> withoutT = candidate.getWithoutTOperations();
        Field field = Commons.toField(withoutT, initField, maxHeight);

        // Tを使わずに組むことができるとき
        LockedReachable lockedReachable = lockedReachableThreadLocal.get();
        if (!BuildUp.existsValidBuildPattern(initField, withoutT, maxHeight, lockedReachable)) {
            return false;
        }

        MinoOperationWithKey operation = candidate.getTOperation();

        // 回転動作で終了できる
        RotateReachable rotateReachable = rotateReachableThreadLocal.get();
        if (!rotateReachable.checks(field, operation.getMino(), operation.getX(), operation.getY(), maxHeight)) {
            return false;
        }

        // Tスピンかどうか
        return Commons.isTSpin(field, operation.getX(), operation.getY());
    }

    private Stream<CandidateObj> expand(CandidateObj candidate) {
        // 未使用のミノを求める
        PieceCounter counter = new PieceCounter(candidate.getAllOperations().stream().map(Operation::getPiece));
        PieceCounter noUsed = PIECE_COUNTER_WITH_ALL.removeAndReturnNew(counter);

        // すべての操作を含む地形
        Field field = Commons.toField(candidate.getAllOperations(), initField, maxHeight);

        // 次におくことができるミノを求める
        Stream<OriginalPiece> nextPiecesStream = noUsed.getBlockStream().flatMap(piece -> {  // 足場を列挙する
            return roofsMap.get(piece)
                    .stream()
                    .filter(field::canPut)
                    .filter(next -> field.isOnGround(next.getMino(), next.getX(), next.getY()));
        });

        // 現在の操作に加える
        return nextPiecesStream
                .filter(originalPiece -> {
                    // 他の追加ミノを抜いたら組み立てられない（すべて必要なミノである）
                    return candidate.getMinusOneOperationsStream()
                            .allMatch(operationWithKeys -> {
                                LockedReachable lockedReachable = lockedReachableThreadLocal.get();
                                return !BuildUp.existsValidBuildPattern(initField, operationWithKeys, maxHeight, lockedReachable);
                            });
                })
                .map(candidate::addNextPiece)
                .flatMap(this::test);
    }
}
