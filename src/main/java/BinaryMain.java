import bin.IndexParserOld;
import bin.SolutionBinary;
import common.SyntaxException;
import common.buildup.BuildUpStream;
import common.datastore.MinoOperation;
import common.datastore.MinoOperationWithKey;
import concurrent.HarddropReachableThreadLocal;
import core.action.reachable.HarddropReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.SimpleOriginalPiece;
import core.srs.Rotate;
import lib.Stopwatch;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BinaryMain {
    public static void main(String[] args) throws IOException, SyntaxException {
        SolutionBinary solutionBinary = new SolutionBinary(7*7*7*7*7*7*7*7*7*7);

        String name = "output/10pieces.bin";
        DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)));
        byte[] bytes = solutionBinary.get();
        dataOutStream.write(bytes, 0, bytes.length);
        dataOutStream.close();

        Stopwatch stopwatch = Stopwatch.createStartedStopwatch();

        EnumMap<Piece, Byte> pieceToNumber = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.values()) {
            pieceToNumber.put(piece, (byte) piece.getNumber());
        }
        IndexParserOld indexParser = new IndexParserOld(pieceToNumber, Arrays.asList(1, 5, 5));

        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();

        Path indexPath = Paths.get("output/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);
        System.out.println(indexes.size());

        Field initField = FieldFactory.createField(fieldHeight);
        HarddropReachableThreadLocal harddropReachableThreadLocal = new HarddropReachableThreadLocal(fieldHeight);

        CountPrinter countPrinter = new CountPrinter(1000, 155000);

        Files.lines(Paths.get("output/tetris_indexed_solutions_SRS7BAG.csv")).parallel()
                .map(line -> (
                        Arrays.stream(line.split(","))
                                .map(Integer::parseInt)
                                .map(indexes::get)
                                .collect(Collectors.toList())
                ))
                .flatMap(pairs -> {
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
                .peek(ignored -> countPrinter.increaseAndShow())
                .flatMap(target -> {
                    HarddropReachable reachable = harddropReachableThreadLocal.get();
                    List<? extends MinoOperationWithKey> operations = target.getOperations();
                    return new BuildUpStream(reachable, fieldHeight).existsValidBuildPattern(initField, operations);
                })
                .forEach(list -> {
                    Stream<Piece> stream = list.stream().map(MinoOperation::getMino).map(Mino::getPiece);
                });

        /**


         .filter(Objects::nonNull)
         .collect(Collectors.toList());
         System.out.println("results = " + results.size());

         if (results.isEmpty()) {
         return;
         }
         */

        stopwatch.stop();

        System.out.println(stopwatch.toMessage(TimeUnit.MILLISECONDS));
    }
}

class Target {
    private final List<SimpleOriginalPiece> operations;
    private final SimpleOriginalPiece lastPiece;

    Target(List<SimpleOriginalPiece> operations, SimpleOriginalPiece lastPiece) {
        this.operations = operations;
        this.lastPiece = lastPiece;
    }

    public List<SimpleOriginalPiece> getOperations() {
        return operations;
    }
}

