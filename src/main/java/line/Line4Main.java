package line;

import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.Operations;
import common.parser.OperationInterpreter;
import common.tetfu.common.ColorConverter;
import commons.Commons;
import core.action.reachable.LockedReachable;
import core.action.reachable.RotateReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import entry.path.output.OneFumenParser;
import line.commons.LineCommons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/*
public class Line4Main {
    public static void main(String[] args) throws IOException {
        {
            String fileName = "1line";
            int index = 7;

            System.out.println(fileName + " " + index);
            System.out.println(Files.lines(Paths.get("output/" + fileName + "_" + index)).count());

            run(fileName + "_" + index);
        }

        {
            String fileName = "12line";
            int index = 7;

            System.out.println(fileName + " " + index);
            System.out.println(Files.lines(Paths.get("output/" + fileName + "_" + index)).count());

//            run(fileName + "_" + index);
        }

        {
            String fileName = "12line";
            int index = 6;

            System.out.println(fileName + " " + index);
            System.out.println(Files.lines(Paths.get("output/" + fileName + "_" + index)).count());

//            run(fileName + "_" + index);
        }
    }

    private static void run(String fileName) throws IOException {
        int maxHeight = 12;

        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();

        ColorConverter colorConverter = new ColorConverter();
        OneFumenParser parser = new OneFumenParser(minoFactory, colorConverter);

        RotateReachable rotateReachable = new RotateReachable(minoFactory, minoShifter, minoRotation, maxHeight);
        LockedReachable lockedReachable = new LockedReachable(minoFactory, minoShifter, minoRotation, maxHeight);

        Field emptyField = FieldFactory.createField(maxHeight);

        SpinChecker spinChecker = new SpinChecker(minoFactory, rotateReachable, lockedReachable, maxHeight);

        AtomicInteger counter = new AtomicInteger();
        Files.lines(Paths.get("output/" + fileName))
                .map(OperationInterpreter::parseToOperations)
                .map(Operations::getOperations)
                .filter(spinChecker::checks)
                .forEach(operations -> {
                    int minY = spinChecker.getMinY(operations);
                    List<MinoOperationWithKey> keys = LineCommons.toOperationWithKeys(minoFactory, operations, minY);
                    String parse = parser.parse(keys, emptyField, maxHeight);
                    int count = counter.incrementAndGet();
                    System.out.println(count + ": http://fumen.zui.jp/?v115@" + parse);
                });
    }
}
*/
