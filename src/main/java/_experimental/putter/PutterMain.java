package _experimental.putter;

import common.SyntaxException;
import common.datastore.Pair;
import common.datastore.PieceCounter;
import common.datastore.action.Action;
import common.datastore.blocks.Pieces;
import common.datastore.order.Order;
import common.iterable.PermutationIterable;
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
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import lib.MyFiles;
import searcher.common.validator.PerfectValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class PutterMain {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException, SyntaxException {
        MinoFactory minoFactory = new MinoFactory();
        PerfectValidator validator = new PerfectValidator();
        PutterUsingHold<Action> putter = new PutterUsingHold<>(minoFactory, validator);

        PatternGenerator generator = new LoadedPatternGenerator("*p4");
        Set<PieceCounter> pieceCounters = generator.blocksStream()
                .map(pieces -> new PieceCounter(pieces.blockStream()))
                .collect(Collectors.toSet());

        int maxClearLine = 4;
        int maxDepth = 10;
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();
        LockedCandidate candidate = new LockedCandidate(minoFactory, minoShifter, minoRotation, maxClearLine);

        int core = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(core);
        LockedCandidateThreadLocal candidateThreadLocal = new LockedCandidateThreadLocal(maxClearLine);
        CheckerUsingHoldThreadLocal<Action> checkerThreadLocal = new CheckerUsingHoldThreadLocal<>();
        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxClearLine);
        CheckerCommonObj commonObj = new CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal);

        PatternGenerator blocksGenerator = new LoadedPatternGenerator("*p7");
        List<Pieces> searchingPieces = blocksGenerator.blocksStream()
                .collect(Collectors.toList());

        int fromDepth = blocksGenerator.getDepth();
        ConcurrentCheckerUsingHoldInvoker invoker = new ConcurrentCheckerUsingHoldInvoker(executorService, commonObj, fromDepth);

        HashMap<Field, Connect> map = new HashMap<>();
        Comparator<Connect> connectComparator = Comparator.<Connect>comparingDouble(o -> o.percent).reversed();

        Path outputDirectoryPath = Paths.get("output/cycle2");
        if (!Files.exists(outputDirectoryPath)) {
            Files.createDirectories(outputDirectoryPath);
        }

        for (PieceCounter counter : pieceCounters) {
            List<Piece> pieces = counter.getBlocks();
            System.out.println(pieces);

            TreeSet<Order> orders = new TreeSet<>();
            PermutationIterable<Piece> iterable = new PermutationIterable<>(pieces, pieces.size());
            for (List<Piece> permutation : iterable) {
                Field initField = FieldFactory.createField("");
                orders.addAll(putter.search(initField, permutation, candidate, maxClearLine, maxDepth));
            }

            System.out.println(orders.size());

            ArrayList<Connect> results = new ArrayList<>();

            for (Order order : orders) {
                if (order.getMaxClearLine() < maxClearLine)
                    continue;

                Field field = order.getField();

                Connect connect = map.getOrDefault(field, null);
                if (connect != null) {
                    connect.add();
                    results.add(connect);
                    continue;
                }

//                System.out.println(i);

                List<Pair<Pieces, Boolean>> search = invoker.search(field, searchingPieces, maxClearLine, maxDepth);
                AnalyzeTree tree = new AnalyzeTree();
                for (Pair<Pieces, Boolean> pair : search) {
                    tree.set(pair.getValue(), pair.getKey());
                }

//                System.out.println(FieldView.toString(field));
                double percent = tree.getSuccessPercent();
//                System.out.println(percent);
//                System.out.println("===");

                Connect value = new Connect(field, percent);
                map.put(field, value);
                results.add(value);
            }

            results.sort(connectComparator);

            List<String> lines = results.stream()
                    .filter(connect -> 0.0 < connect.percent)
                    .map(connect -> String.format("%d,%.5f", connect.field.getBoard(0), connect.percent))
                    .collect(Collectors.toList());

            String name = pieces.stream().map(Piece::getName).collect(Collectors.joining());
            MyFiles.write("output/cycle2/" + name + ".csv", lines);
        }

        List<Connect> values = new ArrayList<>(map.values());
        values.sort(connectComparator);
        List<String> lines = values.stream()
                .filter(connect -> 0.0 < connect.percent)
                .map(connect -> String.format("%d,%.5f,%d", connect.field.getBoard(0), connect.percent, connect.count))
                .collect(Collectors.toList());

        MyFiles.write("output/cycle2/all.csv", lines);

        executorService.shutdown();
    }

    static class Connect {
        private final Field field;
        private final double percent;
        private int count = 1;

        Connect(Field field, double percent) {
            this.field = field;
            this.percent = percent;
        }

        public void add() {
            count += 1;
        }
    }
}
