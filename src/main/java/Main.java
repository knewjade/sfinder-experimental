import bin.IndexParser;
import bin.IndexParserOld;
import common.SyntaxException;
import common.pattern.LoadedPatternGenerator;
import core.mino.Piece;
import lib.Stopwatch;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

import static core.mino.Piece.*;

public class Main {
    private static IndexParserOld createDefaultParserOld(Integer... maxIndexes) {
        EnumMap<Piece, Byte> pieceToNumber = new EnumMap<>(Piece.class);
        pieceToNumber.put(S, (byte) 0);
        pieceToNumber.put(Z, (byte) 1);
        pieceToNumber.put(J, (byte) 2);
        pieceToNumber.put(L, (byte) 3);
        pieceToNumber.put(T, (byte) 4);
        pieceToNumber.put(O, (byte) 5);
        pieceToNumber.put(I, (byte) 6);
        return new IndexParserOld(pieceToNumber, Arrays.asList(maxIndexes));
    }

    private static IndexParser createDefaultParser(Integer... maxIndexes) {
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

    public static void main(String[] args) throws SyntaxException {
        for (int count = 0; count < 5; count++) {
            {
                IndexParserOld indexParser = createDefaultParserOld(1, 5, 5);
                LoadedPatternGenerator generator = new LoadedPatternGenerator("*,*p5,*p5");
                Stopwatch stopwatch = Stopwatch.createStoppedStopwatch();

                generator.blocksStream()
                        .forEach(pieces -> {
                            Piece[] pieceArray = pieces.getPieceArray();
                            stopwatch.start();
                            indexParser.parse(pieceArray);
                            stopwatch.stop();
                        });

                System.out.println(stopwatch.toMessage(TimeUnit.NANOSECONDS));
            }

            {
                IndexParser indexParser = createDefaultParser(1, 5, 5);
                LoadedPatternGenerator generator = new LoadedPatternGenerator("*,*p5,*p5");
                Stopwatch stopwatch = Stopwatch.createStoppedStopwatch();

                generator.blocksStream()
                        .forEach(pieces -> {
                            Piece[] pieceArray = pieces.getPieceArray();
                            stopwatch.start();
                            indexParser.parse(pieceArray);
                            stopwatch.stop();
                        });

                System.out.println(stopwatch.toMessage(TimeUnit.NANOSECONDS));
            }
        }
    }
}
