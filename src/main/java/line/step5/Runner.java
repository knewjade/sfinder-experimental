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
import line.commons.*;

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
    private final AroundBlocks aroundBlock;

    Runner(FactoryPool factoryPool) {
        this.minoFactory = factoryPool.getMinoFactory();
        this.roofs = new Roofs(factoryPool);
        this.maxHeight = factoryPool.getMaxHeight();
        this.factoryPool = factoryPool;
        this.allPieceCounters = LineCommons.getAllPieceCounters();
        this.aroundBlock = new AroundBlocks(maxHeight);
    }

    Stream<Operations> run(Operations operations) {
        Candidate candidate = EmptyCandidate.create(minoFactory, maxHeight, new ArrayList<>(operations.getOperations()));

        RotateReachable rotateReachable = factoryPool.createRotateReachable();
        if (isSolution(candidate.getOperationList(), rotateReachable)) {
            return Stream.of(operations);
        }

        OperationsWithT operationsWithT = new OperationsWithT(candidate.getOperationList(), minoFactory, maxHeight);
        Operation tOperation = operationsWithT.getT();

        Field field = candidate.newField();
        PieceCounter usingPieceCounter = candidate.newPieceCounter();
        PieceCounter reminderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        if (reminderPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        List<KeyOriginalPiece> pieces = roofs.getPieces(tOperation, field, reminderPieceCounter);
        Solutions solutions = new Solutions();

        int clearLine = candidate.newField().clearLine();

        PriorityQueue<Candidate> candidates = new PriorityQueue<>(
                (Comparator.comparingInt(o -> o.getOperationList().size()))
        );
        candidates.add(candidate);

        Stream.Builder<Operations> builder = Stream.builder();

        while (!candidates.isEmpty()) {
            Candidate poll = candidates.poll();
            List<Candidate> results = search(pieces, poll, solutions, clearLine, builder);

            candidates.addAll(results);
        }

        return builder.build();
    }

    private List<Candidate> search(
            List<KeyOriginalPiece> pieces, Candidate candidate, Solutions solutions, int clearLine,
            Stream.Builder<Operations> builder
    ) {
        List<Candidate> candidates = new ArrayList<>();

        RotateReachable rotateReachable = factoryPool.createRotateReachable();

        PieceCounter usingPieceCounter = candidate.newPieceCounter();
        PieceCounter reminderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        OperationsWithT operationsWithT = new OperationsWithT(candidate.getOperationList(), minoFactory, maxHeight);
        Field field = operationsWithT.newField();
        Operation operationT = operationsWithT.getT();

        Field notAllowedBlock = aroundBlock.get(operationT.getX(), operationT.getY());

        for (KeyOriginalPiece keyOriginalPiece : pieces) {
            OriginalPiece originalPiece = keyOriginalPiece.getOriginalPiece();

            // ミノを置くスペースがある
            if (!field.canPut(originalPiece)) {
                continue;
            }

            // 置いてはいけないスペースではない
            if (!notAllowedBlock.canPut(originalPiece)) {
                continue;
            }

            // まだ未使用のミノである
            PieceCounter currentPieceCounter = allPieceCounters.get(keyOriginalPiece.getPiece());
            if (!reminderPieceCounter.containsAll(currentPieceCounter)) {
                continue;
            }

            RecursiveCandidate nextCandidate = new RecursiveCandidate(candidate, keyOriginalPiece);

            // 消去されるライン数が変わらない
            int nextClearLine = nextCandidate.newField().clearLine();
            if (nextClearLine != clearLine) {
                continue;
            }

            if (isUniqueSolution(candidate, nextCandidate, rotateReachable, solutions)) {
                solutions.add(nextCandidate);
                builder.accept(new Operations(nextCandidate.getOperationList()));
            } else {
                // まだ使用していないミノが残っている
                PieceCounter nextPieceCounter = reminderPieceCounter.removeAndReturnNew(currentPieceCounter);
                if (nextPieceCounter.getCounter() == 0L) {
                    continue;
                }

                candidates.add(nextCandidate);
            }
        }

        return candidates;
    }

    private boolean isUniqueSolution(Candidate prevCandidate, RecursiveCandidate nextCandidate, RotateReachable rotateReachable, Solutions solutions) {
        if (!isSolution(nextCandidate.getOperationList(), rotateReachable)) {
            return false;
        }

        // 現在の組み合わせでまだ解が見つかっていない
        return !solutions.partialContains(prevCandidate, nextCandidate.getKeyOriginalPiece());
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
