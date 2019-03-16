import common.datastore.MinoOperationWithKey;
import common.tetfu.common.ColorConverter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import entry.path.output.MyFile;
import entry.path.output.OneFumenParser;
import lib.AsyncBufferedFileWriter;
import main.IndexPiecePair;
import main.IndexPiecePairs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Parse indexed solutions to fumens
public class TetfuMain {
    public static void main(String[] args) throws IOException {
        int fieldHeight = 4;
        MinoFactory minoFactory = new MinoFactory();

        Path indexPath = Paths.get("output/index.csv");
        Map<Integer, IndexPiecePair> indexes = IndexPiecePairs.create(indexPath, minoFactory, fieldHeight);
        System.out.println(indexes.size());

        ColorConverter colorConverter = new ColorConverter();
        OneFumenParser fumenParser = new OneFumenParser(minoFactory, colorConverter);

        Field initField = FieldFactory.createField(fieldHeight);

        List<String> lines = Files.lines(Paths.get("output/tetris_indexed_solutions_SRS7BAG.csv"))
                .map(line -> (
                        Arrays.stream(line.split(","))
                                .map(Integer::parseInt)
                                .map(indexes::get)
                                .collect(Collectors.toList())
                ))
                .map(pairs -> {
                    List<MinoOperationWithKey> operations = pairs.stream()
                            .map(IndexPiecePair::getSimpleOriginalPiece)
                            .collect(Collectors.toList());
                    String fumen = fumenParser.parse(operations, initField, fieldHeight, "");
                    return String.format("v115@%s", fumen);
                })
                .collect(Collectors.toList());
        System.out.println(lines.size());

        MyFile file = new MyFile("output/tetris_fumen_solutions_SRS7BAG.csv");
        try (AsyncBufferedFileWriter writer = file.newAsyncWriter()) {
            for (String line : lines) {
                writer.writeAndNewLine(line);
            }
            writer.flush();
        }
    }
}
