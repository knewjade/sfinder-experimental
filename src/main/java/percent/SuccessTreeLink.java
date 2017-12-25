package percent;

import core.mino.Piece;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

class SuccessTreeLink {
    private static final SuccessTreeLink TERMINATION = new SuccessTreeLink();
    private final EnumMap<Piece, SuccessTreeLink> children = new EnumMap<>(Piece.class);

    void register(LinkedList<Piece> piecesList) {
        Piece key = piecesList.pollFirst();
        if (piecesList.isEmpty()) {
            children.computeIfAbsent(key, this::createTail);
        } else {
            SuccessTreeLink link = children.computeIfAbsent(key, this::createLink);
            link.register(piecesList);
        }
    }

    private SuccessTreeLink createTail(Piece key) {
        return TERMINATION;
    }

    @NotNull
    private SuccessTreeLink createLink(Piece key) {
        return new SuccessTreeLink();
    }

    public boolean checksWithoutHold(LinkedList<Piece> piecesList) {
        Piece first = piecesList.pollFirst();
        if (children.containsKey(first)) {
            SuccessTreeLink successTreeLink = children.get(first);
            boolean result = successTreeLink == TERMINATION || successTreeLink.checksWithoutHold(piecesList);
            if (result)
                return true;
        }
        piecesList.addFirst(first);
        return false;
    }

    public void merge(SuccessTreeLink other) {
        Set<Map.Entry<Piece, SuccessTreeLink>> entries = other.children.entrySet();
        for (Map.Entry<Piece, SuccessTreeLink> entry : entries) {
            SuccessTreeLink value = entry.getValue();
            Piece key = entry.getKey();
            if (value == TERMINATION) {
                children.computeIfAbsent(key, this::createTail);
            } else {
                SuccessTreeLink link = children.computeIfAbsent(key, this::createLink);
                link.merge(value);
            }
        }
    }

    public boolean checksWithHold(LinkedList<Piece> piecesList) {
        Piece first = piecesList.pollFirst();
        if (children.containsKey(first)) {
            SuccessTreeLink successTreeLink = children.get(first);
            boolean result = successTreeLink == TERMINATION || successTreeLink.checksWithHold(piecesList);
            if (result)
                return true;
        }

        Piece second = piecesList.pollFirst();
        piecesList.addFirst(first);

        if (children.containsKey(second)) {
            SuccessTreeLink successTreeLink = children.get(second);
            boolean result = successTreeLink == TERMINATION || successTreeLink.checksWithHold(piecesList);
            if (result)
                return true;
        }

        piecesList.add(1, second);

        return false;
    }

    public boolean checksMixHold(LinkedList<Piece> piecesList, int lastHoldDepth, int depth) {
        if (depth == lastHoldDepth) {
            Piece hold = piecesList.pollFirst();

            boolean result = checksWithoutHold(piecesList);
            if (result)
                return true;

            piecesList.addFirst(hold);
            return false;
        } else if (depth == lastHoldDepth - 1) {
            Piece first = piecesList.pollFirst();
            if (children.containsKey(first)) {
                SuccessTreeLink successTreeLink = children.get(first);
                boolean result = successTreeLink == TERMINATION || successTreeLink.checksMixHold(piecesList, lastHoldDepth, depth + 1);
                if (result)
                    return true;
            }

            piecesList.addFirst(first);

            return false;
        } else {
            Piece first = piecesList.pollFirst();
            if (children.containsKey(first)) {
                SuccessTreeLink successTreeLink = children.get(first);
                boolean result = successTreeLink == TERMINATION || successTreeLink.checksMixHold(piecesList, lastHoldDepth, depth + 1);
                if (result)
                    return true;
            }

            Piece second = piecesList.pollFirst();
            piecesList.addFirst(first);

            if (children.containsKey(second)) {
                SuccessTreeLink successTreeLink = children.get(second);
                boolean result = successTreeLink == TERMINATION || successTreeLink.checksMixHold(piecesList, lastHoldDepth, depth + 1);
                if (result)
                    return true;
            }

            piecesList.add(1, second);

            return false;
        }
    }
}
