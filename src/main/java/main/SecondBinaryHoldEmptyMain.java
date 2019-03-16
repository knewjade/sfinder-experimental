package main;

import bin.RangeChecker;
import bin.SolutionBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import common.order.ReverseOrderLookUp;
import core.mino.Piece;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// start when hold is empty
public class SecondBinaryHoldEmptyMain {
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
        // 9ミノのテトリスパフェ結果を読み込み
        SolutionBinary binary9Piece = load9PieceBinary("resources/9pieces_" + postfix + ".bin");
        System.out.println(binary9Piece.get().length);  // 40353607 bytes

        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();

        // 9ミノでテトリスパフェできる手順を抽出
        ArrayList<PieceNumber[]> solutions9Piece = new ArrayList<>();
        SecondRunner runner = createRunner(binary9Piece, converter);
        runner.run((pieceNumbers) -> {
            PieceNumber[] copy = Arrays.copyOf(pieceNumbers, pieceNumbers.length);
            solutions9Piece.add(copy);
        });
        System.out.println(solutions9Piece.size());  // SRS=4095329,SRS7BAG=2206800

        // 実行
        for (List<Integer> maxIndexes : maxIndexesList) {
            System.out.println("Searching: " + maxIndexes);

            // 初期化
            IndexParser indexParser = new IndexParser(maxIndexes);
            SolutionBinary outputBinary = new SolutionBinary(indexParser.getMax());

            // 出力方式とファイル名
            BinaryOutput output;
            String outputName;
            if (isFirstHoldEmpty) {
                output = new HoldEmpty(outputBinary, converter, maxIndexes, indexParser);

                String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
                outputName = "output/" + prefix + "_" + postfix + ".bin";
            } else {
                output = new WithHold(outputBinary, converter, maxIndexes, indexParser);

                String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
                outputName = "output/h" + prefix + "_" + postfix + ".bin";
            }

            run(converter, solutions9Piece, output, outputName);
        }
    }

    private static void run(
            PieceNumberConverter converter, ArrayList<PieceNumber[]> solutions9Piece,
            BinaryOutput binaryOutput, String outputName
    ) throws IOException {
        // Iを加えてホールドありの手順に展開
        // 指定された範囲のミノのみ抽出
        PieceNumber pieceNumberI = converter.get(Piece.I);

        CountPrinter printer = new CountPrinter(50000, solutions9Piece.size());
        solutions9Piece.stream().parallel()
                .peek(ignore -> printer.increaseAndShow())
                .map(solution -> (
                        Stream.concat(
                                Stream.of(solution),
                                Stream.of(pieceNumberI)
                        ).collect(Collectors.toList())
                ))
                .forEach(binaryOutput::output);

        // 書き込み
        {
            byte[] bytes = binaryOutput.get();
            try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputName)))) {
                dataOutStream.write(bytes, 0, bytes.length);
            }
        }
    }

    private static SolutionBinary load9PieceBinary(String name) throws IOException {
        int totalByte = getTotalByte(name);

        byte[] bytes = new byte[totalByte];
        try (DataInputStream dataInStream = new DataInputStream(new BufferedInputStream(new FileInputStream(name)))) {
            dataInStream.readFully(bytes, 0, bytes.length);
        }

        return new SolutionBinary(bytes);
    }

    private static SecondRunner createRunner(SolutionBinary binary, PieceNumberConverter converter) {
        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        return new SecondRunner(converter, indexParser, binary);
    }

    private static int getTotalByte(String name) throws IOException {
        return (int) Files.size(Paths.get(name));
    }
}

interface BinaryOutput {
    void output(List<PieceNumber> beforeHoldList);

    byte[] get();
}

class HoldEmpty implements BinaryOutput {
    private final SolutionBinary outputBinary;
    private final IndexParser indexParser;
    private final ReverseOrderLookUp lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;

    HoldEmpty(SolutionBinary outputBinary, PieceNumberConverter converter, List<Integer> maxIndexes, IndexParser indexParser) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.lookUp = new ReverseOrderLookUp(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
    }

    @Override
    public void output(List<PieceNumber> beforeHoldList) {
        assert beforeHoldList.size() == 10;

        {
            // ホールドしないパターン
            PieceNumber[] beforeHold = new PieceNumber[11];
            for (int index = 0, size = beforeHoldList.size(); index < size; index++) {
                beforeHold[index] = beforeHoldList.get(index);
            }

            int holdIndex = 10;
            assert beforeHold[holdIndex] == null;

            for (PieceNumber holdPieceNumber : allPieceNumbers) {
                beforeHold[holdIndex] = holdPieceNumber;

                if (rangeChecker.check(beforeHold)) {
                    int index = indexParser.parse(beforeHold);
                    outputBinary.put(index, (byte) 0xff);
                }
            }
        }

        {
            // ホールドするパターン
            lookUp.parse(beforeHoldList).forEach(pieceStream -> {
                List<PieceNumber> afterHoldWithNull = pieceStream.collect(Collectors.toList());

                assert afterHoldWithNull.size() == 11;

                int holdIndex = afterHoldWithNull.indexOf(null);

                if (holdIndex == 0) {
                    // ホールドなしで置けるパターン
                    return;
                }

                assert 1 <= holdIndex;

                PieceNumber[] piecesAfterHold = new PieceNumber[11];
                afterHoldWithNull.toArray(piecesAfterHold);

                // ホールドが必要になるケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int index = indexParser.parse(piecesAfterHold);
                        outputBinary.or(index, holdPieceNumber.getBitByte());
                    }
                }
            });
        }
    }

    @Override
    public byte[] get() {
        return outputBinary.get();
    }
}


class WithHold implements BinaryOutput {
    private final SolutionBinary outputBinary;
    private final IndexParser indexParser;
    private final ReverseOrderLookUp lookUp;
    private final RangeChecker rangeChecker;
    private final List<PieceNumber> allPieceNumbers;

    WithHold(SolutionBinary outputBinary, PieceNumberConverter converter, List<Integer> maxIndexes, IndexParser indexParser) {
        this.outputBinary = outputBinary;
        this.indexParser = indexParser;
        this.lookUp = new ReverseOrderLookUp(10, 11);
        this.rangeChecker = new RangeChecker(maxIndexes);
        this.allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
    }

    @Override
    public void output(List<PieceNumber> beforeHoldList) {
        assert beforeHoldList.size() == 10;

        // ホールドしないパターンはなし
        // すでにホールドされている かつ ホールドするパターンに含まれているため
        // 最終的に残りミノのフラグに含まれる

        {
            // ホールドするパターン
            lookUp.parse(beforeHoldList).forEach(pieceStream -> {
                List<PieceNumber> afterHoldWithNull = pieceStream.collect(Collectors.toList());

                assert afterHoldWithNull.size() == 11;

                int holdIndex = afterHoldWithNull.indexOf(null);

                assert 0 <= holdIndex;

                PieceNumber[] piecesAfterHold = new PieceNumber[11];
                afterHoldWithNull.toArray(piecesAfterHold);

                // ホールドが必要になるケース
                for (PieceNumber holdPieceNumber : allPieceNumbers) {
                    piecesAfterHold[holdIndex] = holdPieceNumber;

                    if (rangeChecker.check(piecesAfterHold)) {
                        int index = indexParser.parse(piecesAfterHold);
                        outputBinary.or(index, holdPieceNumber.getBitByte());
                    }
                }
            });
        }
    }

    @Override
    public byte[] get() {
        return outputBinary.get();
    }
}