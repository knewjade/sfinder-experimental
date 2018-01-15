package main.percent;

import common.datastore.blocks.Pieces;
import core.mino.Piece;

import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 成功のみ記録する前提
// マルチスレッド非対応
public class SuccessTreeHead {
    private final SuccessTreeLink head = new SuccessTreeLink();

    public void register(Pieces pieces) {
        register(pieces.blockStream());
    }

    public void register(Stream<Piece> pieces) {
        LinkedList<Piece> piecesList = pieces.collect(Collectors.toCollection(LinkedList::new));
        assert 1 < piecesList.size();
        head.register(piecesList);
    }

    public void merge(SuccessTreeHead other) {
        head.merge(other.head);
    }

    public boolean checksWithHold(Pieces pieces) {
        assert 1 < pieces.blockStream().count();
        LinkedList<Piece> piecesList = pieces.blockStream().collect(Collectors.toCollection(LinkedList::new));
        return head.checksWithHold(piecesList);
    }

    public boolean checksWithoutHold(Pieces pieces) {
        assert 1 < pieces.blockStream().count();
        LinkedList<Piece> piecesList = pieces.blockStream().collect(Collectors.toCollection(LinkedList::new));
        return head.checksWithoutHold(piecesList);
    }

    public boolean checksMixHold(Pieces pieces, int lastHoldDepth) {
        assert 1 < pieces.blockStream().count();
        LinkedList<Piece> piecesList = pieces.blockStream().collect(Collectors.toCollection(LinkedList::new));
        return head.checksMixHold(piecesList, lastHoldDepth, 0);
    }
}