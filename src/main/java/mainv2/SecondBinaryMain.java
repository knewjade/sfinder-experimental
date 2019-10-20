package mainv2;

import bin.*;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import common.buildup.BuildUpStream;
import common.datastore.MinoOperationWithKey;
import common.datastore.Pair;
import common.order.CountReverseOrderLookUpStartsWithAny;
import common.order.CountReverseOrderLookUpStartsWithEmpty;
import concurrent.HarddropReachableThreadLocal;
import core.action.reachable.HarddropReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.MinoRotation;
import main.BinaryLoader;
import main.CountPrinter;
import main.IndexPiecePair;
import main.IndexPiecePairs;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static mainv2.FirstBinaryMain.getLinesWithSolutionIndex;

public class SecondBinaryMain {
    public static void main(String[] args) throws IOException {
        String postfix = args[0];

        {
            // ホールドが空のとき
            List<List<Integer>> maxIndexesList = Arrays.asList(
                    Arrays.asList(7, 4),
                    Arrays.asList(6, 5),
                    Arrays.asList(5, 6),
                    Arrays.asList(4, 7),
                    Arrays.asList(3, 7, 1),
                    Arrays.asList(2, 7, 2),
                    Arrays.asList(1, 7, 3)
            );

            run(maxIndexesList, postfix, true);
        }

        {
            // すでにホールドがあるとき
            List<List<Integer>> maxIndexesList = Arrays.asList(
                    Arrays.asList(1, 7, 3),
                    Arrays.asList(1, 6, 4),
                    Arrays.asList(1, 5, 5),
                    Arrays.asList(1, 4, 6),
                    Arrays.asList(1, 3, 7),
                    Arrays.asList(1, 2, 7, 1),
                    Arrays.asList(1, 1, 7, 2)
            );

            run(maxIndexesList, postfix, false);
        }
    }

    private static void run(List<List<Integer>> maxIndexesList, String postfix, boolean isFirstHoldEmpty) throws IOException {
        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();

        ArrayList<PieceNumberStep> solutions9Piece = load9Mino(postfix, converter);

        MinoFactory minoFactory = new MinoFactory();
        MinoRotation minoRotation = MinoRotation.create();
        MinoShifter minoShifter = new MinoShifter();

        Movement movement = new Movement(minoFactory, minoRotation, minoShifter);
        MovementComparator comparator = new MovementComparator();

        // Indexを読み込み
        int fieldHeight = 4;
        Path indexPath = Paths.get("resources/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

        // 解をすべてロード
        String solutionFilePath = "resources/tetris_indexed_solutions_" + postfix + ".csv";
        List<Pair<Integer, String>> linesWithSolutionIndex = getLinesWithSolutionIndex(solutionFilePath);
        List<List<IndexPiecePair>> indexPieces = linesWithSolutionIndex.stream()
                .map(lineWithSolutionIndex -> {
                    // ファイルから読みこむ
                    String line = lineWithSolutionIndex.getValue();
                    List<IndexPiecePair> pairs = Arrays.stream(line.split(","))
                            .map(Integer::parseInt)
                            .map(indexes::get)
                            .collect(Collectors.toList());
                    return new Pair<>(lineWithSolutionIndex.getKey(), pairs);
                })
                .sorted(Comparator.comparingInt(Pair::getKey))
                .map(Pair::getValue)
                .collect(Collectors.toList());

        // 実行
        for (List<Integer> maxIndexes : maxIndexesList) {
            System.out.println("Searching: " + maxIndexes);

            // 初期化
            IndexParser indexParser = new IndexParser(maxIndexes);
            SolutionSecondBinary outputBinary = new SolutionSecondBinary(indexParser.getMax() * 8, movement);

            // 出力方式とファイル名
            BinaryOutput output;
            String moveOutputName;
            String solutionsOutputName;
            if (isFirstHoldEmpty) {
                output = new HoldEmpty(outputBinary, converter, maxIndexes, indexParser, comparator, indexPieces);

                String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
                moveOutputName = "output/" + postfix + "/" + prefix + "_mov.bin";
                solutionsOutputName = "output/" + postfix + "/" + prefix + "_indexes.bin";
            } else {
                output = new WithHold(outputBinary, converter, maxIndexes, indexParser, comparator, indexPieces);

                String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
                moveOutputName = "output/" + postfix + "/h" + prefix + "_mov.bin";
                solutionsOutputName = "output/" + postfix + "/h" + prefix + "_indexes.bin";
            }

            run(solutions9Piece, output, moveOutputName, solutionsOutputName);
        }
    }

    private static ArrayList<PieceNumberStep> load9Mino(String postfix, PieceNumberConverter converter) throws IOException {
        // 9ミノのテトリスパフェ結果を読み込み
        SolutionShortBinary binary9Piece = BinaryLoader.loadShortBinary("resources/9pieces_" + postfix + "_mov.bin");
        System.out.println(binary9Piece.max());  // 40353607 * 2 bytes

        // 最小のstepを持つ解を読み込み
        SolutionIntBinary solutionsBinary = BinaryLoader.loadIntBinary("resources/9pieces_" + postfix + "_index.bin");
        System.out.println(solutionsBinary.max());  // 40353607 * 4 bytes

        // Iを加えてホールドありの手順に展開
        // 指定された範囲のミノのみ抽出
        PieceNumber pieceNumberI = converter.get(Piece.I);

        // 9ミノでテトリスパフェできる手順を抽出
        ArrayList<PieceNumberStep> solutions9Piece = new ArrayList<>();
        SecondRunner runner = createRunner(binary9Piece, solutionsBinary, converter);
        runner.run((pieceNumbers, step, solutionIndex) -> {
            PieceNumber[] copy = Arrays.copyOf(pieceNumbers, pieceNumbers.length + 1);
            copy[copy.length - 1] = pieceNumberI;
            solutions9Piece.add(new PieceNumberStep(copy, step, solutionIndex));
        });
        System.out.println(solutions9Piece.size());  // SRS=4095329,SRS7BAG=2206800

        return solutions9Piece;
    }

    private static void run(
            ArrayList<PieceNumberStep> solutions9Piece, BinaryOutput binaryOutput,
            String moveOutputName, String solutionsOutputName
    ) throws IOException {
        CountPrinter printer = new CountPrinter(50000, solutions9Piece.size());
        solutions9Piece.stream().parallel()
                .peek(ignore -> printer.increaseAndShow())
                .forEach(binaryOutput::output);

        SolutionSecondBinary binary = binaryOutput.get();

        // 書き込み
        {
            byte[] solutions = binary.getSolutions();
            try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(moveOutputName)))) {
                for (byte value : solutions) {
                    dataOutStream.writeByte(value);
                }
            }
        }
    }

    private static SecondRunner createRunner(
            SolutionShortBinary binary, SolutionIntBinary solutionsBinary, PieceNumberConverter converter
    ) {
        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        return new SecondRunner(converter, indexParser, binary, solutionsBinary);
    }
}

interface BinaryOutput {
    void output(PieceNumberStep pieceNumberStep);

    SolutionSecondBinary get();

    default boolean[] countHolds(PieceNumber[] numbers, List<Integer> afterIndexes, boolean holdEmpty) {
        boolean[] holds = new boolean[numbers.length];

        // ホールドに何かあり、それが最後まで持っているものならホールドは発生しない
        if (!holdEmpty && afterIndexes.get(0) == null) {
            return holds;
        }

        int hold = holdEmpty ? -1 : afterIndexes.get(0);
        int expect = 0;  // ホールドなしで置けるはずのindex
        for (int i = holdEmpty ? 0 : 1; i < afterIndexes.size(); i++) {
            Integer element = afterIndexes.get(i);

            if (element == null) {
                // ホールドする

                if (hold != -1) {
                    // ホールドに入っているミノはホールドありになる
                    assert hold == expect;
                    holds[hold] = true;
                } else {
                    // 次のミノはホールドありになる
                    i += 1;
                    if (i < afterIndexes.size()) {
                        Integer element2 = afterIndexes.get(i);
                        assert element2 == expect;
                        holds[element2] = true;
                    }
                }

                expect += 1;

                // このあとはホールドは使わない
                for (int j = i + 1; j < afterIndexes.size(); j++) {
                    Integer element2 = afterIndexes.get(j);
                    assert element2 == expect;
                    holds[element2] = false;
                    expect += 1;
                }

                break;
            }

            if (element == expect) {
                // ホールドなしで置く
                holds[element] = false;
                expect += 1;
            } else if (hold == expect) {
                // ホールドを取り出して置く
                // ホールドにあったミノにマークする
                holds[hold] = true;
                hold = element;
                expect += 1;
            } else if (hold == -1) {
                // ホールドする
                hold = element;

                i += 1;

                if (i < afterIndexes.size()) {
                    // 次にツモるミノにマークする
                    Integer element2 = afterIndexes.get(i);
                    assert element2 == expect;
                    holds[element2] = true;
                }

                expect += 1;
            } else {
                throw new IllegalStateException();
            }
        }

        return holds;
    }
}

class HoldEmpty implements BinaryOutput {
    private static final int fieldHeight = 4;

    private final SolutionSecondBinary outputBinary;
    private final IndexParser indexParser;
    private final CountReverseOrderLookUpStartsWithEmpty lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;
    private final MovementComparator comparator;
    private final List<List<IndexPiecePair>> indexPieces;
    private final HarddropReachableThreadLocal harddropReachableThreadLocal;

    HoldEmpty(
            SolutionSecondBinary outputBinary, PieceNumberConverter converter,
            List<Integer> maxIndexes, IndexParser indexParser, MovementComparator comparator,
            List<List<IndexPiecePair>> indexPieces
    ) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.comparator = comparator;
        this.indexPieces = indexPieces;
        this.lookUp = new CountReverseOrderLookUpStartsWithEmpty(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
        this.harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
    }

    @Override
    public void output(PieceNumberStep pieceNumberStep) {
        PieceNumber[] numbers = pieceNumberStep.getNumbers();
        short baseStep = pieceNumberStep.getStep();
        int solutionIndex = pieceNumberStep.getSolutionIndex();

        // numbersの順番をもとに、実際に置かれる位置operationsを逆引きする
        Stream<SimpleOriginalPiece> operations = indexPieces.get(solutionIndex).stream().map(IndexPiecePair::getSimpleOriginalPiece);
        Field initField = FieldFactory.createField(fieldHeight);
        List<Piece> pieceOrder = Arrays.stream(numbers).map(PieceNumber::getPiece).collect(Collectors.toList());
        HarddropReachable reachable = harddropReachableThreadLocal.get();
        Optional<List<MinoOperationWithKey>> optionalBuildOperations = new BuildUpStream(reachable, fieldHeight).existsValidByOrderForTetris(initField, operations, pieceOrder);

        // FirstMainで検索しているため、対応する手順が必ず存在する
        if (!optionalBuildOperations.isPresent()) {
            throw new IllegalStateException();
        }
        List<MinoOperationWithKey> buildOperations = optionalBuildOperations.get();

        assert Movements.isPossible(baseStep);
        assert numbers.length == 10;

        List<Integer> beforeIndexes = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        assert beforeIndexes.size() == 10;

        lookUp.parse(beforeIndexes).forEach(pair -> {
            List<Integer> afterIndexes = pair.getList();

            assert afterIndexes.size() == 11;

            // indexをPieceNumberに変換
            // holdIndexを記録する
            int holdIndex = -1;
            PieceNumber[] piecesAfterHold = new PieceNumber[11];
            for (int i = 0; i < afterIndexes.size(); i++) {
                Integer element = afterIndexes.get(i);
                if (element == null) {
                    piecesAfterHold[i] = null;
                    holdIndex = i;
                } else {
                    piecesAfterHold[i] = numbers[element];
                }
            }

            assert 0 <= holdIndex;

            int holdCount = pair.getHoldCount();
            short step = calc(baseStep, holdCount);

            if (holdCount == 0) {
                // ホールドが必要ないケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int offset = 0;  // empty
                        int index = indexParser.parse(piecesAfterHold) * 8 + offset;

                        // どのミノでもホールドは、計算するまでもなく発生しない
                        boolean[] holds = new boolean[numbers.length];
                        outputBinary.putIfSatisfy(index, step, buildOperations, holds, comparator::shouldUpdate);
                    }
                }
            } else {
                // ホールドが必要になるケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    // ホールドにミノを入れる
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int offset = holdPieceNumber.getNumber() + 1;
                        int index = indexParser.parse(piecesAfterHold) * 8 + offset;

                        if (outputBinary.satisfies(index, step, comparator::shouldUpdate)) {
                            // 更新できそうなときは、さらに詳細な値を計算する  // 計算量削減のためチェックを先に実施

                            // どのPieceNumberを置くときにホールドが発生するか計算する
                            boolean[] holds = countHolds(numbers, afterIndexes, true);

                            // ホールドの数を数えてチェックする
                            int count = 0;
                            for (boolean b : holds) {
                                if (b) {
                                    count++;
                                }
                            }

                            if (holdCount != count) {
                                throw new IllegalStateException();
                            }

                            // 非同期で他に更新されている可能性もあるため、書き込む前に再確認する
                            outputBinary.putIfSatisfy(index, step, buildOperations, holds, comparator::shouldUpdate);
                        }
                    }
                }
            }
        });
    }

    private short calc(short moveAndRotate, int holdCount) {
        return Movements.possible(moveAndRotate, holdCount);
    }

    @Override
    public SolutionSecondBinary get() {
        return outputBinary;
    }
}

class WithHold implements BinaryOutput {
    private static final int fieldHeight = 4;

    private final SolutionSecondBinary outputBinary;
    private final IndexParser indexParser;
    private final CountReverseOrderLookUpStartsWithAny lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;
    private final MovementComparator comparator;
    private final List<List<IndexPiecePair>> indexPieces;
    private final HarddropReachableThreadLocal harddropReachableThreadLocal;

    WithHold(
            SolutionSecondBinary outputBinary, PieceNumberConverter converter,
            List<Integer> maxIndexes, IndexParser indexParser, MovementComparator comparator,
            List<List<IndexPiecePair>> indexPieces
    ) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.comparator = comparator;
        this.indexPieces = indexPieces;
        this.lookUp = new CountReverseOrderLookUpStartsWithAny(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
        this.harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
    }

    @Override
    public void output(PieceNumberStep pieceNumberStep) {
        PieceNumber[] numbers = pieceNumberStep.getNumbers();
        short baseStep = pieceNumberStep.getStep();
        int solutionIndex = pieceNumberStep.getSolutionIndex();

        // numbersの順番をもとに、実際に置かれる位置operationsを逆引きする
        List<IndexPiecePair> indexPiecePairs = indexPieces.get(solutionIndex);
        Stream<SimpleOriginalPiece> operations = indexPiecePairs.stream().map(IndexPiecePair::getSimpleOriginalPiece);
        Field initField = FieldFactory.createField(fieldHeight);
        List<Piece> pieceOrder = Arrays.stream(numbers).map(PieceNumber::getPiece).collect(Collectors.toList());
        HarddropReachable reachable = harddropReachableThreadLocal.get();
        Optional<List<MinoOperationWithKey>> optionalBuildOperations = new BuildUpStream(reachable, fieldHeight).existsValidByOrderForTetris(initField, operations, pieceOrder);

        // FirstMainで検索しているため、対応する手順が必ず存在する
        if (!optionalBuildOperations.isPresent()) {
            throw new IllegalStateException();
        }
        List<MinoOperationWithKey> buildOperations = optionalBuildOperations.get();

        assert Movements.isPossible(baseStep);
        assert numbers.length == 10;

        List<Integer> beforeIndexes = IntStream.range(0, 10).boxed().collect(Collectors.toList());
        assert beforeIndexes.size() == 10;

        lookUp.parse(beforeIndexes).forEach(pair -> {
            List<Integer> afterIndexes = pair.getList();

            assert afterIndexes.size() == 11;

            // indexをPieceNumberに変換
            // holdIndexを記録する
            int holdIndex = -1;
            PieceNumber[] piecesAfterHold = new PieceNumber[11];
            for (int i = 0; i < afterIndexes.size(); i++) {
                Integer element = afterIndexes.get(i);
                if (element == null) {
                    piecesAfterHold[i] = null;
                    holdIndex = i;
                } else {
                    piecesAfterHold[i] = numbers[element];
                }
            }

            assert 0 <= holdIndex;

            int holdCount = pair.getHoldCount();
            short step = calc(baseStep, holdCount);

            // ホールドしないパターンは明示的に区別しない
            // 最後の残りミノが同じパターンに集約される

            // ホールドが必要になるケース
            for (PieceNumber holdPieceNumber : allPieceNumbers) {
                // ホールドにミノを入れる
                piecesAfterHold[holdIndex] = holdPieceNumber;

                if (rangeChecker.check(piecesAfterHold)) {
                    int offset = holdPieceNumber.getNumber() + 1;
                    int index = indexParser.parse(piecesAfterHold) * 8 + offset;

                    if (outputBinary.satisfies(index, step, comparator::shouldUpdate)) {
                        // 更新できそうなときは、さらに詳細な値を計算する  // 計算量削減のためチェックを先に実施

                        // どのPieceNumberを置くときにホールドが発生するか計算する
                        boolean[] holds = countHolds(numbers, afterIndexes, false);

                        // ホールドの数を数えてチェックする
                        int count = 0;
                        for (boolean b : holds) {
                            if (b) {
                                count++;
                            }
                        }

                        if (holdCount != count) {
                            throw new IllegalStateException();
                        }

                        // 非同期で他に更新されている可能性もあるため、書き込む前に再確認する
                        outputBinary.putIfSatisfy(index, step, buildOperations, holds, comparator::shouldUpdate);
                    }
                }
            }
        });
    }

    private short calc(short moveAndRotate, int holdCount) {
        return Movements.possible(moveAndRotate, holdCount);
    }

    @Override
    public SolutionSecondBinary get() {
        return outputBinary;
    }
}