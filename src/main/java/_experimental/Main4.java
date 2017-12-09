package _experimental;

import common.SyntaxException;
import common.datastore.PieceCounter;
import common.datastore.blocks.Pieces;
import common.pattern.LoadedPatternGenerator;

import java.util.Collections;

public class Main4 {
    public static void main(String[] args) throws SyntaxException {
//        sixPiece();
        triplePiece();
    }

    private static void sixPiece() throws SyntaxException {
        LoadedPatternGenerator generator = new LoadedPatternGenerator("*p6,*p5");
        long allCount = generator.blocksStream()
                .map(Pieces::blockStream)
                .map(PieceCounter::new)
                .count();
        long sixPieceCount = generator.blocksStream()
                .map(Pieces::blockStream)
                .map(PieceCounter::new)
                .filter(pieceCounter -> pieceCounter.getEnumMap().keySet().size() < 7)
                .count();
        double percent = (double) sixPieceCount / allCount * 100;
        System.out.println(String.format("6piece: %.2f %% (%d/%d)", percent, sixPieceCount, allCount));
    }

    private static void triplePiece() throws SyntaxException {
        LoadedPatternGenerator generator = new LoadedPatternGenerator("*p3,*p7,*p1");
        long allCount = generator.blocksStream()
                .map(Pieces::blockStream)
                .map(PieceCounter::new)
                .count();
        long sixPieceCount = generator.blocksStream()
                .map(Pieces::blockStream)
                .map(PieceCounter::new)
                .filter(pieceCounter -> {
                    Integer max = Collections.max(pieceCounter.getEnumMap().values());
                    return 3 <= max;
                })
                .count();
        double percent = (double) sixPieceCount / allCount * 100;
        System.out.println(String.format("triple: %.2f %% (%d/%d)", percent, sixPieceCount, allCount));
    }
}
