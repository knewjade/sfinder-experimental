package _experimental.unused;

import common.datastore.action.Action;
import common.iterable.AllPermutationIterable;
import common.iterable.CombinationIterable;
import common.tree.AnalyzeTree;
import concurrent.LockedCandidateThreadLocal;
import concurrent.checker.CheckerUsingHoldThreadLocal;
import core.action.candidate.Candidate;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import lib.Stopwatch;
import searcher.checker.Checker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static core.mino.Piece.*;

public class ConcurrentMain {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Thread.sleep(1000);

        Stopwatch stopwatch = Stopwatch.createStartedStopwatch();

        // Field
        String marks = "" +
                "XXXX______" +
                "XXXX______" +
                "XXXX______" +
                "XXXX______" +
                "";
        Field field = FieldFactory.createField(marks);
        List<Piece> allPieces = Arrays.asList(I, T, S, Z, J, L, O);
        int popCount = 7;
        int maxDepth = 6;
        int maxClearLine = 4;

        // Executor
        int core = Runtime.getRuntime().availableProcessors();
//        System.out.println(core);
        ExecutorService executorService = Executors.newFixedThreadPool(core);
        ThreadLocal<Checker<Action>> checkerThreadLocal = new CheckerUsingHoldThreadLocal<>();
        LockedCandidateThreadLocal candidateThreadLocal = new LockedCandidateThreadLocal(maxClearLine);

        // enumerate combinations and sort
        ArrayList<Callable<PairObj>> callables = new ArrayList<>();
        Iterable<List<Piece>> permutations = new CombinationIterable<>(allPieces, popCount);
        for (List<Piece> permutation : permutations) {
            Iterable<List<Piece>> combinations = new AllPermutationIterable<>(permutation);
            for (List<Piece> pieces : combinations) {
                callables.add(() -> {
                    Checker<Action> checker = checkerThreadLocal.get();
                    Candidate<Action> candidate = candidateThreadLocal.get();
                    boolean check = checker.check(field, pieces, candidate, maxClearLine, maxDepth);
                    return new PairObj(pieces, check);
                });
            }
        }

        stopwatch.start();

        List<Future<PairObj>> futures = executorService.invokeAll(callables);

        AnalyzeTree tree = new AnalyzeTree();
        for (Future<PairObj> future : futures) {
            PairObj obj = future.get();
            List<Piece> combination = obj.pieces;
//                System.out.print(combination + " => ");
            if (obj.isSucceed) {
//                    System.out.println("success");
                tree.success(combination);
            } else {
//                    System.out.println("fail");
                tree.fail(combination);
//                    treeFail.fail(combination);
            }
        }

        stopwatch.stop();

//        // Show
//        tree.show();
//        System.out.println("---");
//        tree.tree(1);
//        System.out.println("---");
        System.out.println(stopwatch.toMessage(TimeUnit.MILLISECONDS));

        executorService.shutdown();
    }

    private static class PairObj {
        private final List<Piece> pieces;
        private final boolean isSucceed;

        PairObj(List<Piece> pieces, boolean isSucceed) {
            this.pieces = pieces;
            this.isSucceed = isSucceed;
        }
    }
}
