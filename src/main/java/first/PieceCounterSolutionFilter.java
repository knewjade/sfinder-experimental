package first;

import common.datastore.PieceCounter;
import core.mino.Piece;
import searcher.pack.memento.MinoFieldMemento;
import searcher.pack.memento.SolutionFilter;
import searcher.pack.mino_field.MinoField;

class PieceCounterSolutionFilter implements SolutionFilter {
    private final PieceCounter pieceCounter = new PieceCounter(Piece.valueList());

    @Override
    public boolean test(MinoFieldMemento memento) {
        return checksPieceCounter(memento.getSumBlockCounter());
    }

    @Override
    public boolean testLast(MinoFieldMemento memento) {
        return checksPieceCounter(memento.getSumBlockCounter());
    }

    @Override
    public boolean testMinoField(MinoField minoField) {
        return checksPieceCounter(minoField.getPieceCounter());
    }

    private boolean checksPieceCounter(PieceCounter counter) {
        return pieceCounter.containsAll(counter);
    }
}
