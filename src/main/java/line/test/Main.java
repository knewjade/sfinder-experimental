package line.test;

import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.Operations;
import common.parser.OperationInterpreter;
import common.tetfu.common.ColorConverter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import entry.path.output.OneFumenParser;
import line.commons.LineCommons;
import line.step3.EmptySlideOperations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        int maxHeight = 24;
        MinoFactory minoFactory = new MinoFactory();
        ColorConverter colorConverter = new ColorConverter();
        OneFumenParser parser = new OneFumenParser(minoFactory, colorConverter);
        int max = Files.lines(Paths.get("output/new"))
                .map(OperationInterpreter::parseToOperations)
                .mapToInt(o -> {
                    Field field = LineCommons.toField(minoFactory, o.getOperations(), maxHeight);
                    int max2 = 0;
                    for (int y = maxHeight - 1; 0 <= y; y -= 1) {
                        if (0 < field.getBlockCountOnY(y)) {
                            max2 = y;
                            break;
                        }
                    }

                    max2 = max2 - field.getLowerY();

                    if (7 <= max2) {
                        System.out.println(max2);
                        List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, o.getOperations());
                        String s = parser.parse(keys, FieldFactory.createField(maxHeight), maxHeight);
                        System.out.println("http://fumen.zui.jp/?v115@" + s);
                    }

                    return max2;
                })
                .max()
                .getAsInt();
        System.out.println(max);
        Files.lines(Paths.get("output/new"))
                .map(OperationInterpreter::parseToOperations)
                .forEach(operations -> {
//                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, operations.getOperations());
//                    String s = parser.parse(keys, FieldFactory.createField(maxHeight), maxHeight);
//                    System.out.println("http://fumen.zui.jp/?v115@" + s);
                });
    }
}
