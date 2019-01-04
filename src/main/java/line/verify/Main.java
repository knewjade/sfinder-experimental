package line.verify;

import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.parser.OperationInterpreter;
import common.tetfu.common.ColorConverter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.MinoFactory;
import entry.path.output.OneFumenParser;
import line.commons.CountPrinter;
import line.commons.FactoryPool;
import line.commons.LineCommons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
//        verify("output/last");
//        verify("output/v2_7");
        check("output/last");
    }

    private static void verify(String fileName) throws IOException {
        int maxHeight = 24;
        MinoFactory minoFactory = new MinoFactory();
        ColorConverter colorConverter = new ColorConverter();
        OneFumenParser parser = new OneFumenParser(minoFactory, colorConverter);

        CountPrinter countPrinter = new CountPrinter(10000, Files.lines(Paths.get(fileName)).count());

        FactoryPool factoryPool = new FactoryPool(maxHeight);
        Runner runner = new Runner(factoryPool);

        Set<String> sets = Collections.synchronizedSet(new HashSet<>());
        Files.lines(Paths.get(fileName))
                .peek(line -> countPrinter.increaseAndShow())
                .map(OperationInterpreter::parseToOperations)
                .filter(operations -> {
                    List<? extends Operation> operationList = operations.getOperations();
//                    if (!runner.verify(new ArrayList<>(operationList))) {
//                        List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, operationList);
//                        String data = parser.parse(keys, FieldFactory.createField(maxHeight), maxHeight, "");
//                        System.out.println("NG");
//                        System.out.println(LineCommons.toURL(data));
//                        return false;
//                    }
                    return true;
                })
                .forEach(operations -> {
                    List<? extends Operation> operationList = operations.getOperations();
                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, operationList);
                    String data = parser.parse(keys, FieldFactory.createField(maxHeight), maxHeight, "");
                    if (!sets.add(data)) {
                        System.out.println("DUPLICATE");
                        System.out.println(LineCommons.toURL(data));
                    }
                });
    }

    private static void check(String fileName) throws IOException {
        int maxHeight = 24;
        MinoFactory minoFactory = new MinoFactory();
        ColorConverter colorConverter = new ColorConverter();

        OneFumenParser parser = new OneFumenParser(minoFactory, colorConverter);

        int maxY = Files.lines(Paths.get(fileName))
                .map(OperationInterpreter::parseToOperations)
                .mapToInt(operations -> {
                    List<? extends Operation> operationList = operations.getOperations();
                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, operationList);
                    Field field = LineCommons.toField(minoFactory, keys, maxHeight);
                    return LineCommons.getMaxY(field);
                })
                .max()
                .orElse(-1);

        Files.lines(Paths.get(fileName))
                .map(OperationInterpreter::parseToOperations)
                .forEach(operations -> {
                    List<? extends Operation> operationList = operations.getOperations();
                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, operationList);
                    Field field = LineCommons.toField(minoFactory, keys, maxHeight);
                    int maxY1 = LineCommons.getMaxY(field);
                    if (maxY1 == maxY) {
                        String data = parser.parse(keys, FieldFactory.createField(maxHeight), maxHeight, "");
                        System.out.println(LineCommons.toURL(data));
                    }
                });

        System.out.println(maxY);
    }
}
