package _experimental;

import common.SyntaxException;
import common.comparator.PiecesNameComparator;
import common.datastore.action.Action;
import common.datastore.blocks.Pieces;
import common.pattern.LoadedPatternGenerator;
import common.pattern.PatternGenerator;
import core.action.candidate.Candidate;
import core.action.candidate.LockedCandidate;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import lib.Stopwatch;
import searcher.checker.CheckerUsingHold;
import searcher.common.validator.PerfectValidator;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static core.mino.Piece.*;

public class HeuristicMain {
    public static void main(String[] args) throws SyntaxException {
        start();
    }

    private static void start() throws SyntaxException {
        PatternTree tree = new PatternTree();
//        LoadedPatternGenerator blocksGenerator = new LoadedPatternGenerator("I,I,J,L,LAST_OPERATION,[SZT]p3,*p3");
        PatternGenerator blocksGenerator = new LoadedPatternGenerator("I,I,J,L,LAST_OPERATION,S,Z,T,*p3");
        List<Pieces> piecesList = blocksGenerator.blocksStream().collect(Collectors.toList());
        piecesList.forEach(pieces -> tree.build(pieces.getPieces(), blocks -> new TerminateChecker()));

        System.out.println(piecesList.size());

        Field field = FieldFactory.createField("" +
                        "__________"
//                "XXXX______" +
//                "XXXX______" +
//                "XXXX______"
        );

        int height = 4;
        UsingHoldPerfectTreeVisitor visitor = new UsingHoldPerfectTreeVisitor(field, height, 10);

        Stopwatch stopwatch = Stopwatch.createStartedStopwatch();
        boolean run = tree.run(visitor);
        stopwatch.stop();


        piecesList.sort(new PiecesNameComparator());
        for (Pieces pieces : piecesList) {
            List<Piece> blocks = pieces.getPieces();
            System.out.println(blocks + " " + tree.get(blocks));
        }

        System.out.println(run);
        System.out.println(stopwatch.toMessage(TimeUnit.MILLISECONDS));
        System.out.println(visitor.stopwatch.toMessage(TimeUnit.MICROSECONDS));
    }

    private static void name() {
        List<Piece> pieces = Arrays.asList(I, I, J, L, O, T, Z, S, T, S, Z);

        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();
        PerfectValidator validator = new PerfectValidator();

        // Field
        String marks = "" +
                "__________" +
                "";
        Field field = FieldFactory.createField(marks);
        int maxClearLine = 4;
        int maxDepth = 10;

        // Initialize
        Candidate<Action> candidate = new LockedCandidate(minoFactory, minoShifter, minoRotation, maxClearLine);

        // Assertion
        // Execute
        CheckerUsingHold<Action> checker = new CheckerUsingHold<>(minoFactory, validator);
        boolean isSucceed = checker.check(field, pieces, candidate, maxClearLine, maxDepth);
        System.out.println(isSucceed);
    }

    private static void h() {
        System.out.println(Heuristic.c(FieldFactory.createField("" +
                "XXXX______" +
                ""), 4));

        System.out.println(Heuristic.c(FieldFactory.createField("" +
                "_XXXX_____" +
                ""), 4));

        System.out.println(Heuristic.c(FieldFactory.createField("" +
                "X_________" +
                "_XXX______" +
                ""), 4));
        System.out.println(Heuristic.c(FieldFactory.createField("" +
                "X_________" +
                "X_________" +
                "_XX_______" +
                ""), 4));
    }
}
