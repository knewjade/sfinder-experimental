package main.verify;

import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.PieceCounter;
import utils.frame.Frames;
import utils.index.IndexPiecePair;
import utils.step.Step;
import utils.step.Steps;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class WithTetris {
    private final List<MinoOperationWithKey> allOperations;
    private final List<MinoOperationWithKey> operations9;
    private final PieceCounter pieceCounter9;
    private final int stepCount;

    WithTetris(List<IndexPiecePair> operations, IndexPiecePair last, Step step) {
        List<MinoOperationWithKey> operations9 = operations.stream()
                .map(IndexPiecePair::getSimpleOriginalPiece)
                .collect(Collectors.toList());
        this.operations9 = operations9;

        List<MinoOperationWithKey> allOperations = Stream.concat(operations.stream(), Stream.of(last))
                .map(IndexPiecePair::getSimpleOriginalPiece)
                .collect(Collectors.toList());
        this.allOperations = allOperations;

        this.pieceCounter9 = new PieceCounter(operations9.stream().map(Operation::getPiece));

        short steps = step.calcMinSteps(allOperations);
        this.stepCount = Steps.getStepCount(steps);
    }

    PieceCounter get9PieceCounter() {
        return pieceCounter9;
    }

    List<MinoOperationWithKey> getAllOperations() {
        return allOperations;
    }

    List<MinoOperationWithKey> get9Operations() {
        return operations9;
    }

    byte frame(int holdCount) {
        return Frames.possible(this.stepCount, holdCount);
    }
}