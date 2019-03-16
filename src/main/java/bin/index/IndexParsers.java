package bin.index;

import core.mino.Piece;

import java.util.Arrays;
import java.util.EnumMap;

import static core.mino.Piece.*;

public class IndexParsers {
    public static IndexParser createDefaultParser(Integer... maxIndexes) {
        EnumMap<Piece, Integer> pieceToNumber = new EnumMap<>(Piece.class);
        pieceToNumber.put(S, 0);
        pieceToNumber.put(Z, 1);
        pieceToNumber.put(J, 2);
        pieceToNumber.put(L, 3);
        pieceToNumber.put(T, 4);
        pieceToNumber.put(O, 5);
        pieceToNumber.put(I, 6);
        return new IndexParser(pieceToNumber, Arrays.asList(maxIndexes));
    }
}
