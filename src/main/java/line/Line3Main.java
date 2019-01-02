package line;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import common.datastore.SimpleOperation;
import common.parser.OperationInterpreter;
import core.action.reachable.LockedReachable;
import core.action.reachable.RotateReachable;
import core.field.Field;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import core.neighbor.OriginalPieceFactory;
import core.srs.MinoRotation;
import core.srs.Rotate;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import line.commons.LineCommons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Line3Main {



}
/*

class Line3Runner {
    private static final PieceCounter ALL_PIECE_COUNTER = new PieceCounter(Piece.valueList());

    private final MinoFactory minoFactory;
    private final SpinChecker spinChecker;
    private final int maxHeight;
    private final List<OriginalPieceWrapper> allPieces;
    private final EnumMap<Piece, PieceCounter> pieceCounters;

    Line3Runner(MinoFactory minoFactory, MinoShifter minoShifter, SpinChecker spinChecker, int maxHeight) {
        this.minoFactory = minoFactory;
        this.spinChecker = spinChecker;
        this.maxHeight = maxHeight;

        OriginalPieceFactory pieceFactory = new OriginalPieceFactory(maxHeight);
        this.allPieces = new ArrayList<>();
        int index = 0;
        for (OriginalPiece originalPiece : pieceFactory.create()) {
            Set<Rotate> uniqueRotates = minoShifter.getUniqueRotates(originalPiece.getPiece());
            if (!uniqueRotates.contains(originalPiece.getRotate())) {
                continue;
            }
            allPieces.add(new OriginalPieceWrapper(originalPiece, index));
            index += 1;
        }

        EnumMap<Piece, PieceCounter> pieceCounters = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.valueList()) {
            pieceCounters.put(piece, new PieceCounter(Stream.of(piece)));
        }
        this.pieceCounters = pieceCounters;
    }

    public Stream<Operations> run(Operations operations) {
        boolean exists = existsAllOnGround(operations.getOperations());
        if (exists) {
            return Stream.of(operations);
        }

        // 使用していないミノ
        PieceCounter usingPieceCounter = new PieceCounter(operations.getOperations().stream().map(Operation::getPiece));
        PieceCounter restPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        if (restPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        return this.run(operations, restPieceCounter);
    }

    private boolean existsAllOnGround(List<? extends Operation> operationList) {
        int minY = spinChecker.getMinY(operationList);

        List<Operation> slideOperations = operationList.stream()
                .map(op -> new SimpleOperation(op.getPiece(), op.getRotate(), op.getX(), op.getY() - minY))
                .collect(Collectors.toList());

        List<Operation> slideOperationsWithoutT = operationList.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .map(op -> new SimpleOperation(op.getPiece(), op.getRotate(), op.getX(), op.getY() - minY))
                .collect(Collectors.toList());

        Field field = LineCommons.toField(minoFactory, slideOperationsWithoutT, maxHeight);
        return slideOperations.stream()
                .allMatch(operation -> {
                    Field freeze = field.freeze();
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    int x = operation.getX();
                    int y = operation.getY();
                    freeze.remove(mino, x, y);
                    return freeze.isOnGround(mino, x, y);
                });
    }

    private Stream<Operations> run(Operations operations, PieceCounter restPieceCounter) {
        List<? extends Operation> operationList = operations.getOperations();
        Field field = LineCommons.toField(minoFactory, operationList, maxHeight);

        // 最も低いところがy=12になるようにスライドする
        int lowerY = field.getLowerY();
        if (lowerY < 12) {
            int slide = 12 - lowerY;
            operationList = operationList.stream()
                    .map(operation -> new SimpleOperation(operation.getPiece(), operation.getRotate(), operation.getX(), operation.getY() + slide))
                    .collect(Collectors.toList());
        }

        Field freeze = LineCommons.toField(minoFactory, operationList, maxHeight);

        return run2(
                new LinkedList<>(operationList), restPieceCounter, freeze, 0, new LinkedList<>(), new HashSet<>()
        );
    }

    private Stream<Operations> run2(
            LinkedList<Operation> operations, PieceCounter restPieceCounter, Field field, int startIndex,
            LinkedList<OriginalPieceWrapper> added, Set<Set<Integer>> sets
    ) {
        Stream.Builder<Integer> candidates = Stream.builder();
        Stream.Builder<Operations> results = Stream.builder();

        PIECE_LOOP:
        for (int index = startIndex; index < allPieces.size(); index++) {
            OriginalPieceWrapper wrapper = allPieces.get(index);
            OriginalPiece originalPiece = wrapper.getPiece();
            PieceCounter pieceCounter = pieceCounters.get(originalPiece.getPiece());
            int keyIndex = wrapper.getIndex();

            // そのミノが既に使われている
            if (!restPieceCounter.containsAll(pieceCounter)) {
                continue;
            }

            // フィールドに置くスペースがない
            if (!field.canPut(originalPiece)) {
                continue;
            }

            // 部分的な組み合わせに既に解が見つかっている
            Set<Integer> keySet = added.stream().map(OriginalPieceWrapper::getIndex).collect(Collectors.toSet());
            keySet.add(keyIndex);

            for (OriginalPieceWrapper w : added) {
                keySet.remove(w.getIndex());

                if (sets.contains(keySet)) {
                    continue PIECE_LOOP;
                }

                keySet.add(w.getIndex());
            }

            keySet.remove(keyIndex);

            // 解であるか
            operations.addLast(originalPiece);
            boolean exists = existsAllOnGround(operations);
            if (exists) {
                HashSet<Integer> newSets = new HashSet<>(keySet);
                newSets.add(keyIndex);
                sets.add(newSets);
                results.accept(new Operations(operations.stream()));
                operations.removeLast();
                continue;
            }
            operations.removeLast();

            // まだ未使用のミノが残っている
            PieceCounter nextPieceCounter = restPieceCounter.removeAndReturnNew(pieceCounter);
            if (nextPieceCounter.getCounter() != 0L) {
                candidates.accept(index);
            }
        }

        Stream<Operations> nextResults = candidates.build().sequential()
                .flatMap(index -> {
                    OriginalPieceWrapper wrapper = allPieces.get(index);
                    OriginalPiece originalPiece = wrapper.getPiece();
                    PieceCounter pieceCounter = pieceCounters.get(originalPiece.getPiece());

                    Field freeze = field.freeze();
                    freeze.put(originalPiece);

                    PieceCounter nextPieceCounter = restPieceCounter.removeAndReturnNew(pieceCounter);
                    operations.addLast(originalPiece);
                    added.addLast(wrapper);

                    Stream<Operations> stream = run2(
                            operations, nextPieceCounter, freeze, index + 1,
                            added, sets
                    );

                    operations.removeLast();
                    added.removeLast();

                    return stream;
                });

        return Stream.concat(results.build(), nextResults);
    }
}

class OriginalPieceWrapper {
    private final OriginalPiece piece;
    private final int index;

    OriginalPieceWrapper(OriginalPiece piece, int index) {
        this.piece = piece;
        this.index = index;
    }

    OriginalPiece getPiece() {
        return piece;
    }

    int getIndex() {
        return index;
    }
}
*/