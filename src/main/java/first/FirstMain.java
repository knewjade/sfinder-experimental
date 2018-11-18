package first;

import common.datastore.OperationWithKey;
import common.parser.OperationWithKeyInterpreter;
import core.column_field.ColumnField;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import searcher.pack.InOutPairField;
import searcher.pack.SeparableMinos;
import searcher.pack.SizedBit;
import searcher.pack.calculator.BasicSolutions;
import searcher.pack.solutions.OnDemandBasicSolutions;
import searcher.pack.task.BasicMinoPackingHelper;
import searcher.pack.task.SetupPackSearcher;
import searcher.pack.task.TaskResultHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 1.
 * 異なる7種のミノでラインが揃う組み合わせを列挙する
 * 指定したラインが揃っている組み合わせを列挙する
 * そのほかのラインが揃っているかは不定
 */
public class FirstMain {
    private static final int MAX_WIDTH = 10;

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        {
            SizedBit sizedBit = new SizedBit(3, 7);
            Field line = FieldFactory.createField(sizedBit.getHeight());
            for (int x = 0; x < MAX_WIDTH; x++) {
                line.setBlock(x, 3);
            }

            SpinRunner runner = new SpinRunner(line, sizedBit, "output/line3");
            runner.run();
        }

        {
            SizedBit sizedBit = new SizedBit(2, 8);
            Field line = FieldFactory.createField(sizedBit.getHeight());
            for (int x = 0; x < MAX_WIDTH; x++) {
                line.setBlock(x, 3);
                line.setBlock(x, 4);
            }

            SpinRunner runner = new SpinRunner(line, sizedBit, "output/line34");
            runner.run();
        }

        {
            SizedBit sizedBit = new SizedBit(2, 9);
            Field line = FieldFactory.createField(sizedBit.getHeight());
            for (int x = 0; x < MAX_WIDTH; x++) {
                line.setBlock(x, 3);
                line.setBlock(x, 5);
            }

            SpinRunner runner = new SpinRunner(line, sizedBit, "output/line35");
            runner.run();
        }

        // no solution
        {
            SizedBit sizedBit = new SizedBit(2, 9);
            Field line = FieldFactory.createField(sizedBit.getHeight());
            for (int x = 0; x < MAX_WIDTH; x++) {
                line.setBlock(x, 3);
                line.setBlock(x, 4);
                line.setBlock(x, 5);
            }

            SpinRunner runner = new SpinRunner(line, sizedBit, "output/line345");
            runner.run();
        }
    }

    private static class SpinRunner {
        private final Field centerLine;
        private final SizedBit sizedBit;
        private final String outputPath;

        private SpinRunner(Field centerLine, SizedBit sizedBit, String outputPath) {
            this.centerLine = centerLine;
            this.sizedBit = sizedBit;
            this.outputPath = outputPath;
        }

        void run() throws ExecutionException, InterruptedException, IOException {
            System.out.println(outputPath);

            // Initialize
            MinoFactory minoFactory = new MinoFactory();
            MinoShifter minoShifter = new MinoShifter();

            // ミノリストの作成
            SeparableMinos separableMinosAll = SeparableMinos.createSeparableMinos(minoFactory, minoShifter, sizedBit, 0L);
            SeparableMinos separableMinos = new SeparableMinos(separableMinosAll.getMinos().stream()
                    .filter(separableMino -> !separableMino.getField().canMerge(centerLine))
                    .collect(Collectors.toList()));

            // 絶対に置く必要があるブロック
            ArrayList<BasicSolutions> basicSolutions = new ArrayList<>();
            List<ColumnField> needFillFields = InOutPairField.createInnerFields(sizedBit, centerLine);

            for (ColumnField innerField : needFillFields) {
                OnDemandBasicSolutions solutions = new OnDemandBasicSolutions(separableMinos, sizedBit, innerField.getBoard(0));
                basicSolutions.add(solutions);
            }

            // 絶対に置かないブロック
            int maxHeight = sizedBit.getHeight();
            List<InOutPairField> inOutPairFields = InOutPairField.createInOutPairFields(sizedBit, FieldFactory.createField(maxHeight));

            // 探索
            TaskResultHelper taskResultHelper = new BasicMinoPackingHelper();
            PieceCounterSolutionFilter solutionFilter = new PieceCounterSolutionFilter();
            SetupPackSearcher searcher = new SetupPackSearcher(inOutPairFields, basicSolutions, sizedBit, solutionFilter, taskResultHelper, needFillFields, separableMinos, centerLine);

            // 出力
            int width = sizedBit.getWidth();
            MyFile file = new MyFile(outputPath);
            try (AsyncBufferedFileWriter bufferedWriter = file.newAsyncWriter()) {
                searcher.forEach(result -> {
                    Stream<OperationWithKey> stream = result.getMemento().getOperationsStream(width);
                    String str = OperationWithKeyInterpreter.parseToString(stream);
                    bufferedWriter.writeAndNewLine(str);
                });
            }
        }
    }
}
