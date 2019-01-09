package line.categorize;

import common.datastore.BlockField;
import common.datastore.MinoOperationWithKey;
import common.parser.OperationInterpreter;
import common.parser.OperationTransform;
import common.tetfu.Tetfu;
import common.tetfu.TetfuElement;
import common.tetfu.common.ColorConverter;
import common.tetfu.field.ColoredField;
import commons.Commons;
import core.mino.MinoFactory;
import core.srs.Rotate;
import entry.path.output.OneFumenParser;
import line.commons.FactoryPool;
import line.commons.LineCommons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws IOException {
        int maxHeight = 24;
        FactoryPool factoryPool = new FactoryPool(maxHeight);
        Summary summary = new Summary(factoryPool);

        MinoFactory minoFactory = factoryPool.getMinoFactory();

        ColorConverter colorConverter = new ColorConverter();
        OneFumenParser parser = new OneFumenParser(minoFactory, colorConverter);

        List<Category> lists = Files.lines(Paths.get("output/last"))
                .map(OperationInterpreter::parseToOperations)
                .map(operations -> Category.create(operations, minoFactory, maxHeight))
                .filter(summary::filter).collect(Collectors.toList());

//        Collections.shuffle(lists);
        Collections.sort(lists, (o1, o2) -> {
            int c2 = Integer.compare(o1.getOperations().getOperations().size(), o2.getOperations().getOperations().size());
            if (c2 != 0) return c2;

            int c4 = Integer.compare((2 + o1.getT().getRotate().getNumber()) % 4, (2 + o2.getT().getRotate().getNumber()) % 4);
            if (c4 != 0) return c4;

            int c3 = Integer.compare(o1.getT().getY(), o2.getT().getY());
            if (c3 != 0) return c3;

            int c1 = Integer.compare(o1.getT().getX(), o2.getT().getX());
            if (c1 != 0) return c1;

            return 0;
        });
        one(maxHeight, minoFactory, colorConverter, lists);

        lists.stream()
                .peek(category -> {
//                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, category.getOperations().getOperations());
//                    String parse = parser.parse(keys, FieldFactory.createField(maxHeight), maxHeight, "");
//                    System.out.println(LineCommons.toURL(parse));
                })
                .forEach(summary::add);

        summary.show();
    }

    private static void one(int maxHeight, MinoFactory minoFactory, ColorConverter colorConverter, List<Category> lists) {
        List<TetfuElement> elements = lists.stream()
                .map(category -> {
                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, category.getOperations().getOperations());
                    BlockField blockField = OperationTransform.parseToBlockField(keys, minoFactory, maxHeight);
                    ColoredField coloredField = Commons.toColoredField(blockField, colorConverter);
                    return new TetfuElement(coloredField, "");
                })
                .collect(Collectors.toList());

        Tetfu tetfu = new Tetfu(minoFactory, colorConverter);
        String encode = tetfu.encode(elements);
        System.out.println(LineCommons.toURL(encode));
    }
}

