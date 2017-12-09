package _experimental.unused;

import common.SyntaxException;
import common.comparator.OperationWithKeyComparator;
import common.datastore.PieceCounter;
import common.datastore.OperationWithKey;
import common.datastore.blocks.Pieces;
import common.iterable.CombinationIterable;
import common.parser.OperationWithKeyInterpreter;
import common.pattern.LoadedPatternGenerator;
import common.pattern.PatternGenerator;
import concurrent.LockedReachableThreadLocal;
import core.column_field.ColumnField;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import lib.Stopwatch;
import searcher.pack.InOutPairField;
import searcher.pack.SeparableMinos;
import searcher.pack.SizedBit;
import searcher.pack.calculator.BasicSolutions;
import searcher.pack.memento.SolutionFilter;
import searcher.pack.memento.UsingBlockAndValidKeySolutionFilter;
import searcher.pack.mino_fields.RecursiveMinoFields;
import searcher.pack.solutions.BasicSolutionsCalculator;
import searcher.pack.solutions.MappedBasicSolutions;
import searcher.pack.task.Field4x10MinoPackingHelper;
import searcher.pack.task.PerfectPackSearcher;
import searcher.pack.task.TaskResultHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PackMain {
    private static final int WIDTH = 3;
    private static final int HEIGHT = 4;

    public static void main(String[] args) throws ExecutionException, InterruptedException, SyntaxException {
        List<String> allOnHold = Arrays.asList(
                "*p7, *p4",
                "*, *p3, *p7",
                "*, *p7, *p3",
                "*, *p4, *p6",
                "*, *, *p7, *p2",
                "*, *p5, *p5",
                "*, *p2, *p7, *",
                "*, *p6, *p4"
        );

        PatternGenerator pieces = new LoadedPatternGenerator(allOnHold);
        HashSet<PieceCounter> counters = pieces.blocksStream().parallel()
                .map(Pieces::getPieces)
                .map(PieceCounter::new)
                .collect(Collectors.toCollection(HashSet::new));

        System.out.println(counters.size());

        Field initField = FieldFactory.createField("" +
                "XXXXXX____" +
                "XXXXXXX___" +
                "XXXXXXXX__" +
                "XXXXXXX___" +
                ""
        );

        // ミノのリストを作成する
        SizedBit sizedBit = new SizedBit(WIDTH, HEIGHT);
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        SeparableMinos separableMinos = SeparableMinos.createSeparableMinos(minoFactory, minoShifter, sizedBit);

        // 検索条件を決める
        SolutionFilter solutionFilter = createUsingBlockAndValidKeyMementoFilter(initField, sizedBit, counters);

        Stopwatch stopwatch1 = Stopwatch.createStartedStopwatch();

        // 基本パターンを計算
        BasicSolutionsCalculator calculator = new BasicSolutionsCalculator(separableMinos, sizedBit);
        Map<ColumnField, RecursiveMinoFields> calculate = calculator.calculate();
        BasicSolutions solutions = new MappedBasicSolutions(calculate, solutionFilter);

        // 基本パターン作成にかかった時間を表示
        stopwatch1.stop();
        System.out.println(stopwatch1.toMessage(TimeUnit.MILLISECONDS));

        System.out.println("========");

        Stopwatch stopwatch2 = Stopwatch.createStartedStopwatch();

        // 探索フィールドを3x4の範囲に変換する
        List<InOutPairField> inOutPairFields = InOutPairField.createInOutPairFields(WIDTH, HEIGHT, initField);
        TaskResultHelper taskResultHelper = new Field4x10MinoPackingHelper();
        PerfectPackSearcher searcher = new PerfectPackSearcher(inOutPairFields, solutions, sizedBit, solutionFilter, taskResultHelper);

        // ファイルに書き出すとき
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

        File outputFile = new File("./output/pack_result");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), StandardCharsets.UTF_8))) {
            searcher.forEach(result -> {
                List<OperationWithKey> operations = result.getMemento().getOperationsStream(sizedBit.getWidth())
                        .sorted(OperationWithKeyComparator::compareOperationWithKey)
                        .collect(Collectors.toList());
                String operationString = OperationWithKeyInterpreter.parseToString(operations);

                singleThreadExecutor.submit(() -> {
                    try {
                        writer.write(operationString);
                        writer.newLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        singleThreadExecutor.shutdown();
        singleThreadExecutor.awaitTermination(1L, TimeUnit.HOURS);  // 十分に長い時間待つ

        // 探索にかかった時間を表示
        stopwatch2.stop();
        System.out.println(stopwatch2.toMessage(TimeUnit.MILLISECONDS));
    }

    private static SolutionFilter createUsingBlockAndValidKeyMementoFilter(Field initField, SizedBit sizedBit, HashSet<PieceCounter> counters) {
        HashSet<Long> validBlockCounters = new HashSet<>();

        for (PieceCounter counter : counters) {
            List<Piece> usingPieces = counter.getBlocks();
            for (int size = 1; size <= usingPieces.size(); size++) {
                CombinationIterable<Piece> combinationIterable = new CombinationIterable<>(usingPieces, size);
                for (List<Piece> pieces : combinationIterable) {
                    PieceCounter newCounter = new PieceCounter(pieces);
                    validBlockCounters.add(newCounter.getCounter());
                }
            }
        }

        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(sizedBit.getHeight());
        return new UsingBlockAndValidKeySolutionFilter(initField, validBlockCounters, reachableThreadLocal, sizedBit);
    }
}