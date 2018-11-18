package second;

import core.mino.Piece;
import core.neighbor.OriginalPiece;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class Scaffolds {
    private final Map<Piece, List<OriginalPiece>> scaffolds;

    Scaffolds(List<OriginalPiece> scaffolds) {
        this.scaffolds = scaffolds.stream().collect(Collectors.groupingBy(OriginalPiece::getPiece));
        for (Piece piece : Piece.values()) {
            if (!this.scaffolds.containsKey(piece)) {
                this.scaffolds.put(piece, Collections.emptyList());
            }
        }
    }

    List<OriginalPiece> get(Piece piece) {
        return scaffolds.get(piece);
    }

    @Override
    public String toString() {
        return "second.Scaffolds{" +
                "scaffolds=" + scaffolds +
                '}';
    }
}
