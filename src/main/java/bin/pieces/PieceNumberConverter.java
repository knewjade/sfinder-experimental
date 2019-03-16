package bin.pieces;

import core.mino.Piece;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static core.mino.Piece.*;

public class PieceNumberConverter {
    public static PieceNumberConverter createDefaultConverter() {
        List<Piece> pieces = Arrays.asList(S, Z, J, L, T, O, I);

        EnumMap<Piece, PieceNumber> pieceToNumber = new EnumMap<>(Piece.class);
        for (int index = 0; index < pieces.size(); index++) {
            Piece piece = pieces.get(index);
            pieceToNumber.put(piece, new PieceNumber(piece, index));
        }

        return new PieceNumberConverter(pieceToNumber);
    }

    private final EnumMap<Piece, PieceNumber> pieceToNumber;

    public PieceNumberConverter(EnumMap<Piece, PieceNumber> pieceToNumber) {
        this.pieceToNumber = pieceToNumber;
    }

    public PieceNumber get(Piece piece) {
        return pieceToNumber.get(piece);
    }
}
