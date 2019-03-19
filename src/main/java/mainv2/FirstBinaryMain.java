package mainv2;

import bin.*;
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
import core.mino.MinoShifter;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.MinoRotation;
import core.srs.Rotate;
import helper.Target;
import main.CountPrinter;
import main.IndexPiecePair;
import main.IndexPiecePairs;

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
        run("SRS");
//        run("SRS7BAG");
//        run(args[0]);
    }

    private static void run(String postfix) throws IOException {
        // 初期化
        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();

        Field initField = FieldFactory.createField(fieldHeight);
        HarddropReachableThreadLocal harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();

        MovementComparator movementComparator = new MovementComparator();
        Movement movement = new Movement(minoFactory, minoRotation, minoShifter);

        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        int max = indexParser.getMax();
        assert max == 7 * 7 * 7 * 7 * 7 * 7 * 7 * 7 * 7;
        SolutionShortBinary binary = new SolutionShortBinary(max);

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
                    // 指定した範囲より値が大きいときは 0 となる
                    short step = calcMinStep(movement, operations);

                    // 解が存在するときは、結果を更新する
                    if (Movements.isPossible(step)) {
                        PieceNumber[] pieces = operations.stream()
                                .map(Operation::getPiece)
                                .map(converter::get)
                                .toArray(PieceNumber[]::new);
                        int index = indexParser.parse(pieces);
                        binary.putIfSatisfy(index, step, movementComparator::shouldUpdate);
                    }
                });

        // 書き込み
        short[] shorts = binary.get();

        String name = "output/9pieces_" + postfix + "_mov.bin";
        try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)))) {
            for (short value : shorts) {
                dataOutStream.writeShort(value);
            }
        }
    }

    // operationsの順番は固定
    // 順番通りにおけることは確認済みの想定
    // holdは0と仮定する (ある固定されたミノ順下で最も小さい値を見つけるため、ホールドの回数は定数として扱っても問題ない)
    private static short calcMinStep(Movement movement, List<MinoOperationWithKey> operations) {
        int moveCount = 0;
        int rotateCount = 0;

        for (MinoOperationWithKey operation : operations) {
            assert operation.getNeedDeletedKey() == 0L;
            Step step = movement.harddrop(operation.getPiece(), operation.getRotate(), operation.getX());
            moveCount += step.movement();
            rotateCount += step.rotateCount();
        }

        int holdCount = 0;
        if (Movements.isRangeIn(moveCount, rotateCount, holdCount)) {
            return Movements.possible(moveCount, rotateCount, holdCount);
        }

        throw new IllegalStateException();
    }
}
