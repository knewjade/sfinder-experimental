package square4x10;

import core.mino.MinoFactory;
import core.mino.Piece;
import core.srs.Rotate;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Extract tetris solutions
public class ExtractTetrisMain {
    public static void main(String[] args) throws Exception {
        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();

        Path indexPath = Paths.get("output/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);
        System.out.println(indexes.size());

        List<List<IndexPiecePair>> solutions = Files.lines(Paths.get("output/indexed_solutions_10x4_SRS.csv"))
                .map(line -> (
                        Arrays.stream(line.split(","))
                                .map(Integer::parseInt)
                                .map(indexes::get)
                                .collect(Collectors.toList())
                ))
                .map(pairs -> {
                    boolean containsILeft = pairs.stream()
                            .map(IndexPiecePair::getSimpleOriginalPiece)
                            .anyMatch(it -> it.getPiece() == Piece.I && it.getRotate() == Rotate.Left);

                    if (!containsILeft) {
                        return null;
                    }

                    boolean noNeedDeletedKey = pairs.stream()
                            .map(IndexPiecePair::getSimpleOriginalPiece)
                            .allMatch(it -> it.getNeedDeletedKey() == 0L);

                    if (!noNeedDeletedKey) {
                        return null;
                    }

                    return pairs;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        System.out.println(solutions.size());

        MyFile file = new MyFile("output/tetris_solutions_SRS.csv");
        try (AsyncBufferedFileWriter writer = file.newAsyncWriter()) {
            for (List<IndexPiecePair> solution : solutions) {
                String line = solution.stream()
                        .map(IndexPiecePair::getIndex)
                        .map(index -> Integer.toString(index))
                        .collect(Collectors.joining(","));
                writer.writeAndNewLine(line);
            }
            writer.flush();
        }
    }
}
