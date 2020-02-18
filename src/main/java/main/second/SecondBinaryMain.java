package main.second;

import core.mino.Piece;
import utils.CountPrinter;
import utils.bin.BinaryLoader;
import utils.bin.SolutionByteBinary;
import utils.bin.SolutionShortBinary;
import utils.frame.FramesComparator;
import utils.index.IndexParser;
import utils.pieces.PieceNumber;
import utils.pieces.PieceNumberConverter;

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
        run("SRS");
//            run("SRS7BAG");
//        run(args[0]);
    }

    public static void run(String postfix) throws IOException {
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

        FramesComparator comparator = new FramesComparator();

        // 実行
        for (List<Integer> maxIndexes : maxIndexesList) {
            System.out.println("Searching: " + maxIndexes);

            // 初期化
            IndexParser indexParser = new IndexParser(maxIndexes);
            SolutionByteBinary outputBinary = new SolutionByteBinary(indexParser.getMax() * 8);

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
            ArrayList<PieceNumberStep> solutions9Piece, BinaryOutput binaryOutput, String moveOutputName
    ) throws IOException {
        CountPrinter printer = new CountPrinter(50000, solutions9Piece.size());
        solutions9Piece.stream().parallel()
                .peek(ignore -> printer.increaseAndShow())
                .forEach(binaryOutput::output);

        SolutionByteBinary binary = binaryOutput.get();

        // 書き込み
        {
            byte[] buffer = binary.get();
            try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(moveOutputName)))) {
                for (byte value : buffer) {
                    dataOutStream.writeByte(value);
                }
            }
        }
    }

    private static SecondRunner createRunner(SolutionShortBinary binary, PieceNumberConverter converter) {
        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        return new SecondRunner(converter, indexParser, binary);
    }
}