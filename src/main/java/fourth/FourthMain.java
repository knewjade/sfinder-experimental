package fourth;

import common.datastore.BlockField;
import common.datastore.MinoOperationWithKey;
import common.parser.OperationWithKeyInterpreter;
import common.tetfu.common.ColorConverter;
import commons.Commons;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import entry.path.output.FumenParser;
import entry.path.output.OneFumenParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 4.
 * 反転形のフィルタリングする
 * 重複を取り除く
 * テト譜に変換する
 */
public class FourthMain {
    public static void main(String[] args) throws IOException {
        String base = "output/line34";

        int maxHeight = 20;
        Field initField = FieldFactory.createField(maxHeight);

        MinoFactory minoFactory = new MinoFactory();
        List<List<MinoOperationWithKey>> candidates = Files.lines(Paths.get(base + "_solutions"))
                .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                .collect(Collectors.toList());

        ColorConverter colorConverter = new ColorConverter();
        FumenParser fumenParser = new OneFumenParser(minoFactory, colorConverter);

        TreeSet<BlockField> fields = new TreeSet<>();
        ArrayList<List<MinoOperationWithKey>> solutions = new ArrayList<>();
        for (List<MinoOperationWithKey> candidate : candidates) {
            BlockField blockField = Commons.toBlockField(candidate, maxHeight);
            if (fields.contains(blockField)) {
                continue;
            }

            BlockField mirror = Commons.toMirror(blockField);
            if (fields.contains(mirror)) {
                continue;
            }

            fields.add(blockField);
            solutions.add(candidate);

//            String data = fumenParser.parse(candidate, initField, maxHeight);
//            System.out.println("http://fumen.zui.jp/?v115@" + data);
        }

        System.out.println(solutions.size());

        int maxAll = -1;
        for (List<MinoOperationWithKey> solution : solutions) {
            int max = -1;
            for (MinoOperationWithKey operationWithKey : solution) {
                int y = operationWithKey.getMino().getMaxY() + operationWithKey.getY();
                if (max < y) max = y;
            }
            if (maxAll < max) maxAll = max;
        }
        System.out.println(maxAll + 1);

        for (List<MinoOperationWithKey> solution : solutions) {
            int max = -1;
            for (MinoOperationWithKey operationWithKey : solution) {
                int y = operationWithKey.getMino().getMaxY() + operationWithKey.getY();
                if (max < y) max = y;
            }

            if (max == maxAll) {
                String data = fumenParser.parse(solution, initField, maxHeight);
                System.out.println("http://fumen.zui.jp/?v115@" + data);
            }
        }

//        List<TetfuElement> elements = solutions.stream()
//                .map(candidate -> {
//                    BlockField blockField = Commons.toBlockField(candidate, maxHeight);
//                    ColoredField coloredField = Commons.toColoredField(blockField, colorConverter);
//                    return new TetfuElement(coloredField, "");
//                })
//                .collect(Collectors.toList());
//        Tetfu tetfu = new Tetfu(minoFactory, colorConverter);
//        String data = tetfu.encode(elements);
//        System.out.println("https://knewjade.github.io/fumen-for-mobile/#?d=v115@" + data);
    }
}
