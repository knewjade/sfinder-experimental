package main;

import bin.SolutionBinary;
import bin.index.IndexParser;
import bin.pieces.PieceNumber;
import bin.pieces.PieceNumberConverter;
import common.buildup.BuildUpStream;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import concurrent.HarddropReachableThreadLocal;
import core.action.reachable.HarddropReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.Rotate;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FirstBinaryMain {
    public static void main(String[] args) throws IOException {
//        run("SRS");
//        run("SRS7BAG");
        run(args[0]);
    }

    private static void run(String postfix) throws IOException {
        // 初期化
        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();
        Field initField = FieldFactory.createField(fieldHeight);
        HarddropReachableThreadLocal harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();

        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        int max = indexParser.getMax();
        assert max == 7 * 7 * 7 * 7 * 7 * 7 * 7 * 7 * 7;
        SolutionBinary binary = new SolutionBinary(max);

        // Indexを読み込み
        Path indexPath = Paths.get("resources/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

        // 対象となる解の数を表示用に事前に取得
        String solutionFilePath = "resources/tetris_indexed_solutions_" + postfix + ".csv";
        CountPrinter countPrinter = new CountPrinter(1000, (int) Files.lines(Paths.get(solutionFilePath)).count());

        // 解から9ミノ（最後のIを除いた手順）で組めるミノ順をすべて列挙
        // すべての地形ごとに結果を保存する
        Files.lines(Paths.get(solutionFilePath)).parallel()
                .map(line -> (
                        // ファイルから読みこむ
                        Arrays.stream(line.split(","))
                                .map(Integer::parseInt)
                                .map(indexes::get)
                                .collect(Collectors.toList())
                ))
                .peek(ignored -> countPrinter.increaseAndShow())
                .flatMap(pairs -> {
                    // テトリスパフェに変換する
                    List<IndexPiecePair> allIList = pairs.stream()
                            .filter(it -> {
                                SimpleOriginalPiece originalPiece = it.getSimpleOriginalPiece();
                                return originalPiece.getPiece() == Piece.I && originalPiece.getRotate() == Rotate.Left;
                            })
                            .collect(Collectors.toList());

                    assert 0 < allIList.size();

                    return allIList.stream().map(iPiece -> {
                        List<SimpleOriginalPiece> operations = pairs.stream()
                                .filter(it -> !iPiece.equals(it))
                                .map(IndexPiecePair::getSimpleOriginalPiece)
                                .collect(Collectors.toList());

                        return new Target(operations, iPiece.getSimpleOriginalPiece());
                    });
                })
                .flatMap(target -> {
                    HarddropReachable reachable = harddropReachableThreadLocal.get();
                    List<? extends MinoOperationWithKey> operations = target.getOperations();
                    return new BuildUpStream(reachable, fieldHeight).existsValidBuildPattern(initField, operations);
                })
                .forEach(operations -> {
                    PieceNumber[] pieces = operations.stream()
                            .map(Operation::getPiece)
                            .map(converter::get)
                            .toArray(PieceNumber[]::new);
                    int index = indexParser.parse(pieces);
                    binary.put(index, (byte) 1);
                });

        // 書き込み
        byte[] bytes = binary.get();

        String name = "output/9pieces_" + postfix + ".bin";
        try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)))) {
            dataOutStream.write(bytes, 0, bytes.length);
        }
    }
}
