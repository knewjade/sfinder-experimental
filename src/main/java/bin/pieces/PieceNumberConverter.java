package bin.pieces;

import core.mino.Piece;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

import static core.mino.Piece.*;

public class PieceNumberConverter {
    public static final List<Piece> PPT_PIECES = Arrays.asList(S, Z, J, L, T, O, I);

    public static PieceNumberConverter createDefaultConverter() {
        return createConverter(PPT_PIECES);
    }

    private static PieceNumberConverter createConverter(List<Piece> pieces) {
        EnumMap<Piece, PieceNumber> pieceToNumber = new EnumMap<>(Piece.class);
        for (int index = 0; index < pieces.size(); index++) {
            Piece piece = pieces.get(index);
            pieceToNumber.put(piece, new PieceNumber(piece, index));
        }

        return create(pieceToNumber);
    }

    private static PieceNumberConverter create(EnumMap<Piece, PieceNumber> pieceToNumber) {
        int size = pieceToNumber.keySet().size();
        PieceNumber[] numberToPieceNumber = new PieceNumber[size];
        for (PieceNumber pieceNumber : pieceToNumber.values()) {
            numberToPieceNumber[pieceNumber.getNumber()] = pieceNumber;
        }
        return new PieceNumberConverter(pieceToNumber, numberToPieceNumber);
    }

    private final EnumMap<Piece, PieceNumber> pieceToNumber;
    private final PieceNumber[] numberToPieceNumber;

    private PieceNumberConverter(EnumMap<Piece, PieceNumber> pieceToNumber, PieceNumber... numberToPieceNumber) {
        this.pieceToNumber = pieceToNumber;
        this.numberToPieceNumber = numberToPieceNumber;
    }

    public PieceNumber get(Piece piece) {
        return pieceToNumber.get(piece);
    }

    public PieceNumber get(int value) {
        return numberToPieceNumber[value];
    }
}
