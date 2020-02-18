package main.first;

import common.buildup.BuildUpStream;
import common.datastore.Operation;
import common.datastore.Pair;
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
import utils.CountPrinter;
import utils.Movement;
import utils.index.IndexParser;
import utils.index.IndexPiecePair;
import utils.index.IndexPiecePairs;
import utils.pieces.PieceNumber;
import utils.pieces.PieceNumberConverter;
import utils.step.MaxMoveRotateStep;
import utils.step.Step;
import utils.step.Steps;
import utils.step.StepsComparator;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FirstBinaryMain {
    public static void main(String[] args) throws IOException {
//        run("SRS");
        run("SRS7BAG");
//        run(args[0]);
    }

    private static void run(String postfix) throws IOException {
        // 初期化
        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = MinoRotation.create();

        Field initField = FieldFactory.createField(fieldHeight);
        HarddropReachableThreadLocal harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);
        PieceNumberConverter converter = PieceNumberConverter.createDefaultConverter();

        StepsComparator stepsComparator = new StepsComparator();
        Movement movement = new Movement(minoFactory, minoRotation, minoShifter);
        Step frame = Step.create(movement);

        // Indexを読み込み
        Path indexPath = Paths.get("resources/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);

        // 対象となる解の数を表示用に事前に取得
        String solutionFilePath = "resources/tetris_indexed_solutions_" + postfix + ".csv";
        CountPrinter countPrinter = new CountPrinter(1000, (int) Files.lines(Paths.get(solutionFilePath)).count());

        // 出力用の配列 & インデックスを計算
        IndexParser indexParser = new IndexParser(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1, 1));
        int max = indexParser.getMax();
        assert max == 7 * 7 * 7 * 7 * 7 * 7 * 7 * 7 * 7;
        SolutionFirstBinary binary = new SolutionFirstBinary(max);

        // 解をすべてロード
        List<Pair<Integer, String>> linesWithSolutionIndex = getLinesWithSolutionIndex(solutionFilePath);

        // 解から9ミノ（最後のIを除いた手順）で組めるミノ順をすべて列挙
        // すべての地形ごとに結果を保存する
        linesWithSolutionIndex.parallelStream()
                .map(lineWithSolutionIndex -> {
                    // ファイルから読みこむ
                    String line = lineWithSolutionIndex.getValue();
                    List<IndexPiecePair> pairs = Arrays.stream(line.split(","))
                            .map(Integer::parseInt)
                            .map(indexes::get)
                            .collect(Collectors.toList());
                    return new Pair<>(lineWithSolutionIndex.getKey(), pairs);
                })
                .peek(ignored -> countPrinter.increaseAndShow())
                .flatMap(pairsWithSolutionIndex -> {
                    int solutionIndex = pairsWithSolutionIndex.getKey();
                    List<IndexPiecePair> pairs = pairsWithSolutionIndex.getValue();

                    // stepを計算
                    // ミノの置く順番には影響を受けない
                    // 指定した範囲より値が大きいときは 0 となる
                    List<SimpleOriginalPiece> allOperations = pairs.stream()
                            .map(IndexPiecePair::getSimpleOriginalPiece)
                            .collect(Collectors.toList());

                    assert allOperations.size() == 10;

                    // operationsの順番にmovementは影響を受けない
                    // holdは0と仮定する (ある固定されたミノ順下で最も小さい値を見つけるため、ホールドの回数は定数として扱っても問題ない)
                    short step = frame.calcMinSteps(allOperations);

                    assert Steps.isPossible(step);

                    // すべてのI縦を取得
                    List<IndexPiecePair> allIList = pairs.stream()
                            .filter(it -> {
                                SimpleOriginalPiece originalPiece = it.getSimpleOriginalPiece();
                                return originalPiece.getPiece() == Piece.I && originalPiece.getRotate() == Rotate.Left;
                            })
                            .collect(Collectors.toList());

                    assert 0 < allIList.size();

                    // テトリスパフェに変換する
                    return allIList.stream().map(iPiece -> {
                        List<SimpleOriginalPiece> operations = pairs.stream()
                                .filter(it -> !iPiece.equals(it))
                                .map(IndexPiecePair::getSimpleOriginalPiece)
                                .collect(Collectors.toList());

                        return new Target(solutionIndex, operations, iPiece.getSimpleOriginalPiece(), step);
                    });
                })
                .forEach(target -> {
                    int solutionIndex = target.getSolutionIndex();
                    short step = target.getMoveAndRotate();
                    HarddropReachable reachable = harddropReachableThreadLocal.get();

                    // すべてHarddropで組める
                    new BuildUpStream(reachable, fieldHeight).existsValidBuildPattern(initField, target.getOperations())
                            .forEach(operations -> {
                                // 解が存在するときは、結果を更新する
                                PieceNumber[] pieces = operations.stream()
                                        .map(Operation::getPiece)
                                        .map(converter::get)
                                        .toArray(PieceNumber[]::new);
                                int index = indexParser.parse(pieces);
                                binary.putIfSatisfy(index, step, solutionIndex, stepsComparator::shouldUpdate);
                            });
                });

        // 書き込み
        {
            short[] steps = binary.getSteps();

            String name = "output/9pieces_" + postfix + "_mov.bin";
            try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)))) {
                for (short value : steps) {
                    dataOutStream.writeShort(value);
                }
            }
        }
    }

    public static List<Pair<Integer, String>> getLinesWithSolutionIndex(String solutionFilePath) throws IOException {
        List<Pair<Integer, String>> linesWithSolutionIndex = new ArrayList<>();
        List<String> lines = Files.lines(Paths.get(solutionFilePath)).collect(Collectors.toList());
        for (int index = 0; index < lines.size(); index++) {
            linesWithSolutionIndex.add(new Pair<>(index, lines.get(index)));
        }
        return linesWithSolutionIndex;
    }
}
