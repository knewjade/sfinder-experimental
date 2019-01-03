package line.step5;

import common.buildup.BuildUp;
import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import core.action.reachable.LockedReachable;
import core.action.reachable.RotateReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import line.commons.FactoryPool;
import line.commons.KeyOriginalPiece;
import line.commons.LineCommons;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Runner {
    private static final PieceCounter ALL_PIECE_COUNTER = new PieceCounter(Piece.valueList());

    private final MinoFactory minoFactory;
    private final Roofs roofs;
    private final int maxHeight;
    private final FactoryPool factoryPool;
    private final EnumMap<Piece, PieceCounter> allPieceCounters;

    Runner(FactoryPool factoryPool) {
        this.minoFactory = factoryPool.getMinoFactory();
        this.roofs = new Roofs(factoryPool);
        this.maxHeight = factoryPool.getMaxHeight();
        this.factoryPool = factoryPool;
        this.allPieceCounters = LineCommons.getAllPieceCounters();
    }

    Stream<Operations> run(Operations operations) {
        List<Operation> operationList = new ArrayList<>(operations.getOperations());

        RotateReachable rotateReachable = factoryPool.createRotateReachable();
        if (isSolution(operationList, rotateReachable)) {
            return Stream.of(operations);
        }

        Optional<Operation> optional = operationList.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();

        assert optional.isPresent();

        Operation tOperation = optional.get();

        Field field = LineCommons.toField(minoFactory, operationList, maxHeight);
        PieceCounter usingPieceCounter = LineCommons.toPieceCounter(operationList);
        PieceCounter reminderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        if (reminderPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        List<KeyOriginalPiece> pieces = roofs.getPieces(tOperation, field, reminderPieceCounter);

        return search(new ArrayList<>(operationList), pieces, reminderPieceCounter, new HashSet<>(), new HashSet<>());
    }

    private Stream<Operations> search(
            List<Operation> operationList, List<KeyOriginalPiece> pieces, PieceCounter remainderPieceCounter,
            Set<Integer> currentKeySet, Set<Set<Integer>> solutionKeySet
    ) {
        Stream.Builder<KeyOriginalPiece> candidates = Stream.builder();
        Stream.Builder<Operations> results = Stream.builder();

        Field field = LineCommons.toField(minoFactory, operationList, maxHeight);
        RotateReachable rotateReachable = factoryPool.createRotateReachable();

        for (KeyOriginalPiece keyOriginalPiece : pieces) {
            OriginalPiece originalPiece = keyOriginalPiece.getOriginalPiece();

            // ミノを置くスペースがある
            if (!field.canPut(originalPiece)) {
                continue;
            }

            // まだ未使用のミノである
            PieceCounter currentPieceCounter = allPieceCounters.get(keyOriginalPiece.getPiece());
            if (!remainderPieceCounter.containsAll(currentPieceCounter)) {
                continue;
            }

            Field freeze = field.freeze();
            freeze.put(originalPiece);

            List<Operation> operations = new ArrayList<>(operationList);
            operations.add(originalPiece);

            if (isUniqueSolution(operations, keyOriginalPiece, rotateReachable, currentKeySet, solutionKeySet)) {
                Set<Integer> keySet = new HashSet<>(currentKeySet);
                keySet.add(keyOriginalPiece.getIndex());

                solutionKeySet.add(keySet);
                results.accept(new Operations(operations));
            } else {
                // まだ使用していないミノが残っている
                PieceCounter nextPieceCounter = remainderPieceCounter.removeAndReturnNew(currentPieceCounter);
                if (nextPieceCounter.getCounter() == 0L) {
                    continue;
                }

                candidates.accept(keyOriginalPiece);
            }
        }

        Stream<Operations> stream = candidates.build().sequential()
                .flatMap(keyOriginalPiece -> {
                    List<Operation> operations = new ArrayList<>(operationList);
                    operations.add(keyOriginalPiece.getOriginalPiece());

                    Set<Integer> keySet = new HashSet<>(currentKeySet);
                    keySet.add(keyOriginalPiece.getIndex());

                    PieceCounter currentPieceCounter = allPieceCounters.get(keyOriginalPiece.getPiece());
                    PieceCounter nextPieceCounter = remainderPieceCounter.removeAndReturnNew(currentPieceCounter);

                    return search(operations, pieces, nextPieceCounter, keySet, solutionKeySet);
                });

        return Stream.concat(results.build(), stream);
    }

    private boolean isUniqueSolution(
            List<Operation> operations, KeyOriginalPiece originalPiece, RotateReachable rotateReachable,
            Set<Integer> currentKeySet, Set<Set<Integer>> solutionKeySet
    ) {
        if (!isSolution(operations, rotateReachable)) {
            return false;
        }

        Set<Integer> keySet = new HashSet<>(currentKeySet);

        keySet.add(originalPiece.getIndex());

        // 現在の組み合わせでまだ解が見つかっていない
        if (solutionKeySet.contains(keySet)) {
            return false;
        }

        // 部分的な組み合わせでまだ解が見つかっていない
        for (int prevKey : currentKeySet) {
            keySet.remove(prevKey);

            if (solutionKeySet.contains(keySet)) {
                return false;
            }

            keySet.add(prevKey);
        }

        return true;
    }

    private boolean isSolution(List<Operation> operations, RotateReachable rotateReachable) {
        // 空中に浮いているミノがない
        if (!LineCommons.existsAllOnGround(minoFactory, operations, maxHeight)) {
            return false;
        }

        // Tを回転入れで置くことができる
        List<Operation> operationsWithoutT = operations.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());

        Optional<Operation> optional = operations.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();

        assert optional.isPresent();

        Operation tOperation = optional.get();

        Field fieldWithoutT = LineCommons.toField(minoFactory, operationsWithoutT, maxHeight);

        Mino mino = minoFactory.create(tOperation.getPiece(), tOperation.getRotate());
        if (!rotateReachable.checks(fieldWithoutT, mino, tOperation.getX(), tOperation.getY(), maxHeight)) {
            return false;
        }

        // Tミノを接着できる
        if (!fieldWithoutT.isOnGround(mino, tOperation.getX(), tOperation.getY())) {
            return false;
        }

        // T以外のミノを実際に組む手順が存在する
        Field emptyField = FieldFactory.createField(maxHeight);
        LockedReachable lockedReachable = factoryPool.createLockedReachable();
        return BuildUp.existsValidBuildPattern(
                emptyField, LineCommons.toOperationWithKeys(minoFactory, operationsWithoutT), maxHeight, lockedReachable
        );
    }
}
