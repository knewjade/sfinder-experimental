package line;

import common.buildup.BuildUp;
import common.datastore.MinimalOperationWithKey;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//
public class Line3Main {
    static List<MinoOperationWithKey> toOperationWithKeys(MinoFactory minoFactory, List<? extends Operation> operations, int minY) {
        return operations.stream()
                .map(operation -> {
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    int operationX = operation.getX();
                    int operationY = operation.getY() - minY;
                    return new MinimalOperationWithKey(mino, operationX, operationY, 0L);
                })
                .collect(Collectors.toList());
    }

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
                    List<MinoOperationWithKey> keys = Line3Main.toOperationWithKeys(minoFactory, operations, minY);
                    String parse = parser.parse(keys, emptyField, maxHeight);
                    int count = counter.incrementAndGet();
                    System.out.println(count + ": http://fumen.zui.jp/?v115@" + parse);
                });
    }
}

class SpinChecker {
    private final MinoFactory minoFactory;
    private final int maxHeight;
    private final RotateReachable rotateReachable;
    private final LockedReachable lockedReachable;

    SpinChecker(MinoFactory minoFactory, RotateReachable rotateReachable, LockedReachable lockedReachable, int maxHeight) {
        this.minoFactory = minoFactory;
        this.rotateReachable = rotateReachable;
        this.lockedReachable = lockedReachable;
        this.maxHeight = maxHeight;
    }

    int getMinY(List<? extends Operation> operationsList) {
        // 最も低いブロックが一番下になるように移動
        int minY = Integer.MAX_VALUE;
        for (Operation operation : operationsList) {
            Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
            int min = operation.getY() + mino.getMinY();
            if (min < minY) {
                minY = min;
            }
        }
        return minY;
    }

    boolean checks(List<? extends Operation> operationsList) {
        int minY = getMinY(operationsList);
        return checks(operationsList, minY);
    }

    private boolean checks(List<? extends Operation> operationsList, int minY) {
        List<? extends Operation> withoutT = operationsList.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());

        Optional<? extends Operation> optional = operationsList.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();
        assert optional.isPresent();
        Operation tOperation = optional.get();

        // T以外の地形
        Field field = FieldFactory.createField(maxHeight);
        for (Operation operation : withoutT) {
            Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
            field.put(mino, operation.getX(), operation.getY() - minY);
        }

        Mino mino = minoFactory.create(tOperation.getPiece(), tOperation.getRotate());
        int x = tOperation.getX();
        int y = tOperation.getY() - minY;

        Field emptyField = FieldFactory.createField(maxHeight);
        if (!Commons.isTSpin(field, x, y)) {
            return false;
        }

        if (!rotateReachable.checks(field, mino, x, y, maxHeight)) {
            return false;
        }

        List<MinoOperationWithKey> keysWithoutT = Line3Main.toOperationWithKeys(minoFactory, withoutT, minY);
        return BuildUp.existsValidBuildPattern(emptyField, keysWithoutT, maxHeight, lockedReachable);
    }
}