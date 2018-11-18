package commons;

import common.datastore.MinoOperationWithKey;
import common.datastore.PieceCounter;
import common.iterable.CombinationIterable;
import core.mino.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CandidateObj {
    private final List<MinoOperationWithKey> operations;
    private final List<MinoOperationWithKey> scaffolds;

    public CandidateObj(List<MinoOperationWithKey> operations, List<MinoOperationWithKey> scaffolds) {
        this.operations = operations;
        this.scaffolds = scaffolds;
    }

    public List<MinoOperationWithKey> getAllOperations() {
        ArrayList<MinoOperationWithKey> nextOperations = new ArrayList<>(operations);
        nextOperations.addAll(scaffolds);
        return nextOperations;
    }

    public List<MinoOperationWithKey> getWithoutTOperations() {
        List<MinoOperationWithKey> nextOperations = operations.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());
        nextOperations.addAll(scaffolds);
        return nextOperations;
    }

    public CandidateObj slideDown(int slide) {
        if (slide == 0) {
            return this;
        }
        List<MinoOperationWithKey> nextOperations = Commons.slideDownList(operations, slide);
        List<MinoOperationWithKey> nextScaffolds = Commons.slideDownList(scaffolds, slide);
        return new CandidateObj(nextOperations, nextScaffolds);
    }

    public Stream<List<MinoOperationWithKey>> getMinusOneOperationsStream() {
        int size = scaffolds.size();
        if (size == 0) {
            return Stream.empty();
        }

        CombinationIterable<MinoOperationWithKey> combination = new CombinationIterable<>(scaffolds, size - 1);
        Stream.Builder<List<MinoOperationWithKey>> builder = Stream.builder();
        for (List<MinoOperationWithKey> pieces : combination) {
            ArrayList<MinoOperationWithKey> nextOperations = new ArrayList<>(operations);
            nextOperations.addAll(pieces);
            builder.accept(nextOperations);
        }
        return builder.build();
    }

    public CandidateObj addNextPiece(MinoOperationWithKey originalPiece) {
        ArrayList<MinoOperationWithKey> nextOperations = new ArrayList<>(operations);
        ArrayList<MinoOperationWithKey> nextScaffolds = new ArrayList<>(scaffolds);
        nextScaffolds.add(originalPiece);
        return new CandidateObj(nextOperations, nextScaffolds);
    }

    public PieceCounter getUsedPieceCounter() {
        return new PieceCounter(operations.stream().map(MinoOperationWithKey::getPiece));
    }

    public int size() {
        return operations.size() + scaffolds.size();
    }

    public MinoOperationWithKey getTOperation() {
        for (MinoOperationWithKey operation : operations)
            if (operation.getPiece() == Piece.T)
                return operation;
        throw new IllegalStateException("No T operation");
    }
}
