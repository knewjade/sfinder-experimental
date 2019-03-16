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

public class SecondBinaryMain {
    public static void main(String[] args) throws IOException {
//        run("SRS");
//        run("SRS7BAG");
        run(args[0]);
    }

    private static void run(String postfix) throws IOException {
        // 9ミノのテトリスパフェ結果を読み込み
        SolutionBinary binary9Piece = load9PieceBinary(postfix);

        // 初期化
        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();
        List<Integer> maxIndexList = Arrays.asList(7, 4);
        RangeChecker rangeChecker = new RangeChecker(maxIndexList);
        IndexParser indexParser = new IndexParser(maxIndexList);

        // 9ミノでテトリスパフェできる手順を抽出
        ArrayList<PieceNumber[]> solutions9Piece = new ArrayList<>();
        SecondRunner runner = createRunner(binary9Piece, converter);
        runner.run((pieceNumbers) -> {
            PieceNumber[] copy = Arrays.copyOf(pieceNumbers, pieceNumbers.length);
            solutions9Piece.add(copy);
        });
        System.out.println(solutions9Piece.size());  // 2206800

        // Iを加えてホールドありの手順に展開
        // 指定された範囲のミノのみ抽出
        SolutionBinary outputBinary = new SolutionBinary(indexParser.getMax());
        ReverseOrderLookUp lookUp = new ReverseOrderLookUp(10, 11);

        List<PieceNumber> allPieceNumbers = PieceNumberConverter.PPT_PIECES.stream()
                .map(converter::get)
                .collect(Collectors.toList());
        PieceNumber pieceNumberI = converter.get(Piece.I);

        CountPrinter printer = new CountPrinter(1000, solutions9Piece.size());
        solutions9Piece.stream().parallel()
                .peek(ignore -> printer.increaseAndShow())
                .map(solution -> (
                        Stream.concat(
                                Stream.of(solution),
                                Stream.of(pieceNumberI)
                        ).collect(Collectors.toList())
                ))
                .forEach(beforeHoldList -> {
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
                });

        // 書き込み
        {
            byte[] bytes = outputBinary.get();

            String prefix = maxIndexList.stream().map(Object::toString).collect(Collectors.joining());
            String name = "output/" + prefix + "_" + postfix + ".bin";
            try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)))) {
                dataOutStream.write(bytes, 0, bytes.length);
            }
        }
    }

    private static SolutionBinary load9PieceBinary(String postfix) throws IOException {
        String name = "resources/9pieces_" + postfix + ".bin";
        int totalByte = getTotalByte(name);
        System.out.println(totalByte);  // 40353607

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

