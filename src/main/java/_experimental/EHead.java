package _experimental;

import common.datastore.blocks.Pieces;
import core.mino.Piece;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 成功のみ記録する前提
public class EHead {
    private final ELink head = new ELink();

    public void register(Pieces pieces) {
        register(pieces.blockStream());
    }

    public void register(Stream<Piece> pieces) {
        LinkedList<Piece> piecesList = pieces.collect(Collectors.toCollection(LinkedList::new));
        assert 1 < piecesList.size();
        head.register(piecesList);
    }

    public void merge(EHead other) {
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
}

class ELink {
    private static final ELink TERMINATION = new ELink();
    private final EnumMap<Piece, ELink> children = new EnumMap<>(Piece.class);

    void register(LinkedList<Piece> piecesList) {
        Piece key = piecesList.pollFirst();
        if (piecesList.isEmpty()) {
            children.computeIfAbsent(key, this::createTail);
        } else {
            ELink link = children.computeIfAbsent(key, this::createLink);
            link.register(piecesList);
        }
    }

    private ELink createTail(Piece key) {
        return TERMINATION;
    }

    @NotNull
    private ELink createLink(Piece key) {
        return new ELink();
    }

    public boolean checksWithoutHold(LinkedList<Piece> piecesList) {
        Piece first = piecesList.pollFirst();
        if (!children.containsKey(first))
            return false;
        ELink eLink = children.get(first);
        return eLink == TERMINATION || eLink.checksWithHold(piecesList);
    }

    public void merge(ELink other) {
        Set<Map.Entry<Piece, ELink>> entries = other.children.entrySet();
        for (Map.Entry<Piece, ELink> entry : entries) {
            ELink value = entry.getValue();
            Piece key = entry.getKey();
            if (value == TERMINATION) {
                children.computeIfAbsent(key, this::createTail);
            } else {
                ELink link = children.computeIfAbsent(key, this::createLink);
                link.merge(value);
            }
        }
    }

    public boolean checksWithHold(LinkedList<Piece> piecesList) {
        Piece first = piecesList.pollFirst();
        if (children.containsKey(first)) {
            ELink eLink = children.get(first);
            boolean result = eLink == TERMINATION || eLink.checksWithHold(piecesList);
            if (result)
                return true;
        }

        Piece second = piecesList.pollFirst();
        piecesList.addFirst(first);

        if (children.containsKey(second)) {
            ELink eLink = children.get(second);
            boolean result = eLink == TERMINATION || eLink.checksWithHold(piecesList);
            if (result)
                return true;
        }

        piecesList.add(1, second);

        return false;
    }
}