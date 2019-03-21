package mainv2;

import bin.MovementComparator;
import bin.Movements;
import bin.RangeChecker;
import bin.SolutionShortBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import common.order.CountReverseOrderLookUpStartsWithAny;
import common.order.CountReverseOrderLookUpStartsWithEmpty;
import core.mino.Piece;
import main.BinaryLoader;
import main.CountPrinter;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        MovementComparator comparator = new MovementComparator();

        // 実行
        for (List<Integer> maxIndexes : maxIndexesList) {
            System.out.println("Searching: " + maxIndexes);

            // 初期化
            IndexParser indexParser = new IndexParser(maxIndexes);
            SolutionShortBinary outputBinary = new SolutionShortBinary(indexParser.getMax() * 8);

            // 出力方式とファイル名
            BinaryOutput output;
            String outputName;
            if (isFirstHoldEmpty) {
                output = new HoldEmpty(outputBinary, converter, maxIndexes, indexParser, comparator);

                String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
                outputName = "output/" + postfix + "/" + prefix + "_mov.bin";
            } else {
                output = new WithHold(outputBinary, converter, maxIndexes, indexParser, comparator);

                String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
                outputName = "output/" + postfix + "/h" + prefix + "_mov.bin";
            }

            run(solutions9Piece, output, outputName);
        }
    }

    private static ArrayList<PieceNumberStep> load9Mino(String postfix, PieceNumberConverter converter) throws IOException {
        // 9ミノのテトリスパフェ結果を読み込み
        SolutionShortBinary binary9Piece = BinaryLoader.loadShortBinary("resources/9pieces_" + postfix + "_mov.bin");
        System.out.println(binary9Piece.max());  // 40353607 * 2 bytes

        // Iを加えてホールドありの手順に展開
        // 指定された範囲のミノのみ抽出
        PieceNumber pieceNumberI = converter.get(Piece.I);

        // 9ミノでテトリスパフェできる手順を抽出
        ArrayList<PieceNumberStep> solutions9Piece = new ArrayList<>();
        SecondRunner runner = createRunner(binary9Piece, converter);
        runner.run((pieceNumbers, step) -> {
            PieceNumber[] copy = Arrays.copyOf(pieceNumbers, pieceNumbers.length + 1);
            copy[copy.length - 1] = pieceNumberI;
            solutions9Piece.add(new PieceNumberStep(copy, step));
        });
        System.out.println(solutions9Piece.size());  // SRS=4095329,SRS7BAG=2206800

        return solutions9Piece;
    }

    private static void run(
            ArrayList<PieceNumberStep> solutions9Piece, BinaryOutput binaryOutput, String outputName
    ) throws IOException {
        CountPrinter printer = new CountPrinter(50000, solutions9Piece.size());
        solutions9Piece.stream().parallel()
                .peek(ignore -> printer.increaseAndShow())
                .forEach(binaryOutput::output);

        // 書き込み
        {
            short[] shorts = binaryOutput.get();
            try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputName)))) {
                for (short value : shorts) {
                    dataOutStream.writeShort(value);
                }
            }
        }
    }

    private static SecondRunner createRunner(SolutionShortBinary binary, PieceNumberConverter converter) {
        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        return new SecondRunner(converter, indexParser, binary);
    }
}

interface BinaryOutput {
    void output(PieceNumberStep pieceNumberStep);

    short[] get();
}

class HoldEmpty implements BinaryOutput {
    private final SolutionShortBinary outputBinary;
    private final IndexParser indexParser;
    private final CountReverseOrderLookUpStartsWithEmpty lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;
    private final MovementComparator comparator;

    HoldEmpty(SolutionShortBinary outputBinary, PieceNumberConverter converter, List<Integer> maxIndexes, IndexParser indexParser, MovementComparator comparator) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.comparator = comparator;
        this.lookUp = new CountReverseOrderLookUpStartsWithEmpty(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
    }

    @Override
    public void output(PieceNumberStep pieceNumberStep) {
        List<PieceNumber> beforeHoldList = Arrays.asList(pieceNumberStep.getNumbers());
        short baseStep = pieceNumberStep.getStep();

        assert Movements.isPossible(baseStep);
        assert beforeHoldList.size() == 10;

        lookUp.parse(beforeHoldList).forEach(pair -> {
            List<PieceNumber> afterHoldWithNull = pair.getList();

            assert afterHoldWithNull.size() == 11;

            int holdIndex = afterHoldWithNull.indexOf(null);

            assert 0 <= holdIndex;

            PieceNumber[] piecesAfterHold = new PieceNumber[11];
            afterHoldWithNull.toArray(piecesAfterHold);

            int holdCount = pair.getHoldCount();
            short step = calc(baseStep, holdCount);

            if (holdCount == 0) {
                // ホールドが必要ないケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int offset = 0;  // empty
                        int index = indexParser.parse(piecesAfterHold) * 8 + offset;
                        outputBinary.putIfSatisfy(index, step, comparator::shouldUpdate);
                    }
                }
            } else {
                // ホールドが必要になるケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int offset = holdPieceNumber.getNumber() + 1;
                        int index = indexParser.parse(piecesAfterHold) * 8 + offset;
                        outputBinary.putIfSatisfy(index, step, comparator::shouldUpdate);
                    }
                }
            }
        });
    }

    private short calc(short moveAndRotate, int holdCount) {
        return Movements.possible(moveAndRotate, holdCount);
    }

    @Override
    public short[] get() {
        return outputBinary.get();
    }
}

class WithHold implements BinaryOutput {
    private final SolutionShortBinary outputBinary;
    private final IndexParser indexParser;
    private final CountReverseOrderLookUpStartsWithAny lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;
    private final MovementComparator comparator;

    WithHold(SolutionShortBinary outputBinary, PieceNumberConverter converter, List<Integer> maxIndexes, IndexParser indexParser, MovementComparator comparator) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.comparator = comparator;
        this.lookUp = new CountReverseOrderLookUpStartsWithAny(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
    }

    @Override
    public void output(PieceNumberStep pieceNumberStep) {
        List<PieceNumber> beforeHoldList = Arrays.asList(pieceNumberStep.getNumbers());
        short baseStep = pieceNumberStep.getStep();

        assert beforeHoldList.size() == 10;

        lookUp.parse(beforeHoldList).forEach(pair -> {
            List<PieceNumber> afterHoldWithNull = pair.getList();

            assert afterHoldWithNull.size() == 11;

            int holdIndex = afterHoldWithNull.indexOf(null);

            assert 0 <= holdIndex;

            PieceNumber[] piecesAfterHold = new PieceNumber[11];
            afterHoldWithNull.toArray(piecesAfterHold);

            int holdCount = pair.getHoldCount();
            short step = calc(baseStep, holdCount);

            // ホールドしないパターンは明示的に区別しない
            // 最後の残りミノが同じパターンに集約される

            for (PieceNumber holdPieceNumber : allPieceNumbers) {
                piecesAfterHold[holdIndex] = holdPieceNumber;

                if (rangeChecker.check(piecesAfterHold)) {
                    int offset = holdPieceNumber.getNumber() + 1;
                    int index = indexParser.parse(piecesAfterHold) * 8 + offset;
                    outputBinary.putIfSatisfy(index, step, comparator::shouldUpdate);
                }
            }
        });
    }

    private short calc(short moveAndRotate, int holdCount) {
        return Movements.possible(moveAndRotate, holdCount);
    }

    @Override
    public short[] get() {
        return outputBinary.get();
    }
}