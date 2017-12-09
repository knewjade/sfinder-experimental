package _experimental.putter;

import common.SyntaxException;
import common.datastore.Pair;
import common.datastore.Result;
import common.datastore.action.Action;
import common.datastore.blocks.Pieces;
import common.datastore.order.Order;
import common.pattern.LoadedPatternGenerator;
import common.pattern.PatternGenerator;
import common.tree.AnalyzeTree;
import concurrent.LockedCandidateThreadLocal;
import concurrent.LockedReachableThreadLocal;
import concurrent.checker.CheckerUsingHoldThreadLocal;
import concurrent.checker.invoker.CheckerCommonObj;
import concurrent.checker.invoker.using_hold.ConcurrentCheckerUsingHoldInvoker;
import core.action.candidate.LockedCandidate;
import core.field.Field;
import core.field.FieldFactory;
import core.field.FieldView;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import searcher.checkmate.CheckmateNoHold;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PutterMain2 {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException, SyntaxException {
        List<String> setupPattern = Arrays.asList(
//                "I,LAST_OPERATION,L,J",
//                "I,LAST_OPERATION,S,Z",
//                "I,LAST_OPERATION,L,S",
//                "I,LAST_OPERATION,J,Z",
//                "I,LAST_OPERATION,J,S",
//                "I,LAST_OPERATION,L,Z",

//                "I,L,J,S",
//                "I,L,J,Z",
//                "I,L,S,Z",
//                "I,J,S,Z",

//                "LAST_OPERATION,J,L,Z",
//                "LAST_OPERATION,J,L,S",

//                "LAST_OPERATION,L,S,Z",
//                "LAST_OPERATION,J,S,Z",

//                "L,J,S,Z"

//                "I,LAST_OPERATION,S,Z",
//                "L,J,S,Z",
                "T,I,LAST_OPERATION,S",
                "T,I,LAST_OPERATION,Z",
                "I,T,S,Z",
                "LAST_OPERATION,T,L,J",
                "LAST_OPERATION,T,S,Z"
        );
        int maxClearLine = 4;

        // Piece generator
        Field initField = FieldFactory.createField("");
        PatternGenerator setupGenerator = new LoadedPatternGenerator(setupPattern);
        int setupMaxDepth = setupGenerator.getDepth();
        int emptyDepth = 10 - ((initField.getNumOfAllBlocks() / 4) + setupMaxDepth);  // 10 - (field + setupPattern)
        SetupValidator validator = new SetupValidator(4 * emptyDepth);

        // Initialize
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();
        int core = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(core);

        LoadedPatternGenerator generator = new LoadedPatternGenerator(setupPattern);
        List<Pair<String, Set<Field>>> collect = generator.blockCountersStream().parallel()
                .map(blockCounter -> blockCounter.getBlockStream().map(Piece::getName).collect(Collectors.joining()))
                .map(names -> String.format("[%s]!", names))
                .map(s -> {
                    System.out.println(s);
                    LoadedPatternGenerator patternGenerator = null;
                    try {
                        patternGenerator = new LoadedPatternGenerator(s);
                    } catch (SyntaxException e) {
                        e.printStackTrace();
                    }
                    return new Pair<>(s, patternGenerator.blocksStream().parallel()
                            .flatMap(blocks -> {
                                LockedCandidate candidate = new LockedCandidate(minoFactory, minoShifter, minoRotation, maxClearLine);
                                CheckmateNoHold<Action> checkmateNoHold = new CheckmateNoHold<>(minoFactory, validator);
                                List<Result> search = checkmateNoHold.search(initField, blocks.getPieces(), candidate, maxClearLine, setupMaxDepth);
                                return search.stream()
                                        .map(result -> {
                                            Order order = result.getOrder();
                                            int lastMaxClearLine = result.getOrder().getMaxClearLine();
                                            Field field = order.getField().freeze(lastMaxClearLine);
                                            Action action = result.getLastAction();
                                            Mino mino = minoFactory.create(result.getLastPiece(), action.getRotate());
                                            field.put(mino, action.getX(), action.getY());
                                            return field;
                                        });
                            })
                            .collect(Collectors.toSet()));
                })
                .collect(Collectors.toList());

        Set<Field> allFields = new HashSet<>();
        for (Pair<String, Set<Field>> pair : collect)
            allFields.addAll(pair.getValue());

        System.out.println(allFields.size());

        ArrayList<Pair<Field, Integer>> results = new ArrayList<>();
        for (Field field : allFields) {
            int count = 0;
            for (Pair<String, Set<Field>> pair : collect)
                if (pair.getValue().contains(field))
                    count++;
            results.add(new Pair<>(field, count));
        }

        Comparator<Pair<Field, Integer>> comparator = Comparator.comparingInt(Pair::getValue);
        results.sort(comparator.reversed());

        LockedCandidateThreadLocal candidateThreadLocal = new LockedCandidateThreadLocal(maxClearLine);
        CheckerUsingHoldThreadLocal<Action> checkerThreadLocal = new CheckerUsingHoldThreadLocal<>();
        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxClearLine);
        CheckerCommonObj commonObj = new CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal);
        PatternGenerator verifyGenerator = new LoadedPatternGenerator("*!");
        List<Pieces> searchingBlocks = verifyGenerator.blocksStream().collect(Collectors.toList());

        ConcurrentCheckerUsingHoldInvoker invoker = new ConcurrentCheckerUsingHoldInvoker(executorService, commonObj, verifyGenerator.getDepth());

        for (Pair<Field, Integer> pair : results) {
            if (pair.getValue() <= 1)
                continue;

            Field field = pair.getKey();
            int verifyMaxClearLine = (field.getNumOfAllBlocks() + emptyDepth * 4) / 10;
            List<Pair<Pieces, Boolean>> search = invoker.search(field, searchingBlocks, verifyMaxClearLine, emptyDepth);
            AnalyzeTree tree = new AnalyzeTree();
            for (Pair<Pieces, Boolean> pair2 : search) {
                tree.set(pair2.getValue(), pair2.getKey());
            }
            double successPercent = tree.getSuccessPercent() * 100;

            if (85.0 < successPercent) {
                System.out.println(FieldView.toString(field));
                System.out.println(pair.getValue());
                System.out.println(successPercent);
                System.out.println();
            }
        }

        System.exit(0);

        // Search setup field
        Set<Field> allFields2 = setupGenerator.blocksStream().parallel()
                .flatMap(blocks -> {
                    LockedCandidate candidate = new LockedCandidate(minoFactory, minoShifter, minoRotation, maxClearLine);
                    CheckmateNoHold<Action> checkmateNoHold = new CheckmateNoHold<>(minoFactory, validator);
                    List<Result> search = checkmateNoHold.search(initField, blocks.getPieces(), candidate, maxClearLine, setupMaxDepth);
                    return search.stream()
                            .map(result -> {
                                Order order = result.getOrder();
                                int lastMaxClearLine = result.getOrder().getMaxClearLine();
                                Field field = order.getField().freeze(lastMaxClearLine);
                                Action action = result.getLastAction();
                                Mino mino = minoFactory.create(result.getLastPiece(), action.getRotate());
                                field.put(mino, action.getX(), action.getY());
                                return field;
                            });
                })
                .collect(Collectors.toSet());


        System.out.println(allFields2.size());
        System.out.println();

        // Sort by percent
        Comparator<Pair<Field, Double>> naturalComparator = Comparator.comparingDouble(Pair::getValue);
        List<Pair<Field, Double>> results2 = allFields2.stream()
                .map(field -> {
                    try {
                        int verifyMaxClearLine = (field.getNumOfAllBlocks() + emptyDepth * 4) / 10;
                        List<Pair<Pieces, Boolean>> search = invoker.search(field, searchingBlocks, verifyMaxClearLine, emptyDepth);
                        AnalyzeTree tree = new AnalyzeTree();
                        for (Pair<Pieces, Boolean> pair : search) {
                            tree.set(pair.getValue(), pair.getKey());
                        }
                        double successPercent = tree.getSuccessPercent() * 100;
                        return new Pair<>(field, successPercent);
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    return null;
                })
                .sorted(naturalComparator.reversed())
                .collect(Collectors.toList());

        // Show
        for (Pair<Field, Double> result : results2.subList(0, Math.min(results2.size(), 10))) {
            System.out.println(result.getValue());
            System.out.println(FieldView.toString(result.getKey()));
            System.out.println();
        }

        executorService.shutdown();
    }
}
