import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.OperationWithKey;
import common.datastore.PieceCounter;
import common.parser.OperationWithKeyInterpreter;
import common.tetfu.common.ColorConverter;
import commons.Commons;
import commons.RotateReachableThreadLocal;
import concurrent.LockedReachableThreadLocal;
import core.action.reachable.LockedReachable;
import core.action.reachable.Reachable;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpinMain {
    private static final int MAX_HEIGHT = 12;

    public static void main(String[] args) throws IOException {
        MinoFactory minoFactory = new MinoFactory();
        ColorConverter colorConverter = new ColorConverter();

        OneFumenParser fumenParser = new OneFumenParser(minoFactory, colorConverter);

        Field initField = FieldFactory.createField(MAX_HEIGHT);
        String file = "output/line3_all";
        SpinRunner runner = new SpinRunner(initField, Paths.get(file), MAX_HEIGHT);
        runner.run()
                .map(operationWithKeys -> fumenParser.parse(operationWithKeys, initField, MAX_HEIGHT))
                .forEach(fumen -> System.out.println("http://fumen.zui.jp/?v115@" + fumen));
    }
}

class SpinRunner {
    private static final PieceCounter PIECE_COUNTER_WITH_ALL = new PieceCounter(Piece.valueList());

    private final Field initField;
    private final Path path;
    private final int maxHeight;
    private final MinoFactory minoFactory;
    private final RotateReachableThreadLocal rotateReachableThreadLocal;
    private final LockedReachableThreadLocal lockedReachableThreadLocal;

    SpinRunner(Field initField, Path path, int maxHeight) {
        this.initField = initField;
        this.path = path;
        this.maxHeight = maxHeight;
        this.minoFactory = new MinoFactory();

        MinoShifter minoShifter = new MinoShifter();
        MinoRotation minoRotation = new MinoRotation();

        this.rotateReachableThreadLocal = new RotateReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);
        this.lockedReachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, maxHeight);
    }

    Stream<List<MinoOperationWithKey>> run() throws IOException {
        // Tが入るかは見ていない
        return Files.lines(path)
                .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                .filter(operations -> operations.stream().anyMatch(operation -> operation.getPiece() == Piece.T))
                .map(operationWithKeys -> Commons.slideDownToGround(operationWithKeys, initField, maxHeight))
                .map(OperationsWithT::new)
                .flatMap(this::parse)
                .map(OperationsWithT::getOperations);
    }

    private Stream<OperationsWithT> parse(OperationsWithT operationsWithT) {
        int size = operationsWithT.getOperationWithoutT().size();
        if (size < 6) {
            return filterAndAdd(operationsWithT);
        } else {
            OperationsWithT operations = filterValid(operationsWithT);
            if (operations == null) {
                return Stream.empty();
            }
            return Stream.of(operations);
        }
    }

    private Stream<OperationsWithT> filterAndAdd(OperationsWithT operationsWithT) {
        // 少ないミノでも成り立っている地形か
        {
            OperationsWithT operations = filterValid(operationsWithT);
            if (operations != null) {
                return Stream.of(operations);
            }
        }

        // 1ミノ加える
        PieceCounter pieceCounter = new PieceCounter(operationsWithT.getOperations().stream().map(Operation::getPiece));
        PieceCounter unused = PIECE_COUNTER_WITH_ALL.removeAndReturnNew(pieceCounter);
        return unused.getBlockStream()
                .flatMap(piece -> {
                    // TODO
                    return Stream.empty();
                });
    }

    private OperationsWithT filterValid(OperationsWithT operationsWithT) {
        List<MinoOperationWithKey> operationsWithoutT = operationsWithT.getOperationWithoutT();
        OperationWithKey lastOperation = operationsWithT.getLastOperation();

        Field field = Commons.toField(operationsWithoutT, initField, maxHeight);

        // T-Spinか
        int x = lastOperation.getX();
        int y = lastOperation.getY();
        if (!isTSpin(field, x, y)) {
            return null;
        }

        // 一連の操作が回転動作で終了するか
        Reachable rotateReachable = rotateReachableThreadLocal.get();
        Mino mino = minoFactory.create(lastOperation.getPiece(), lastOperation.getRotate());
        if (!rotateReachable.checks(field, mino, x, y, maxHeight)) {
            return null;
        }

        // 実際に組み立てることができるか
        List<MinoOperationWithKey> operations = operationsWithT.getOperations();
        LockedReachable reachable = lockedReachableThreadLocal.get();
        if (!BuildUp.cansBuild(initField, operations, maxHeight, reachable)) {
            return null;
        }

        return operationsWithT;
    }

    private static boolean isTSpin(Field field, int x, int y) {
        return 3L <= Stream.of(
                isBlock(field, x - 1, y - 1),
                isBlock(field, x - 1, y + 1),
                isBlock(field, x + 1, y - 1),
                isBlock(field, x + 1, y + 1)
        ).filter(Boolean::booleanValue).count();
    }

    private static boolean isBlock(Field field, int x, int y) {
        if (x < 0 || 10 <= x || y < 0) {
            return true;
        }
        return !field.isEmpty(x, y);
    }
}

class OperationsWithT {
    private final List<MinoOperationWithKey> operationWithKeys;
    private final List<MinoOperationWithKey> operationsWithoutT;
    private final OperationWithKey lastOperation;

    OperationsWithT(List<MinoOperationWithKey> operationWithKeys) {
        this.operationWithKeys = operationWithKeys;

        this.operationsWithoutT = operationWithKeys.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());

        List<MinoOperationWithKey> pieces = operationWithKeys.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .collect(Collectors.toList());
        assert pieces.size() == 1;
        this.lastOperation = pieces.get(0);
    }

    List<MinoOperationWithKey> getOperations() {
        return operationWithKeys;
    }

    List<MinoOperationWithKey> getOperationWithoutT() {
        return operationsWithoutT;
    }

    OperationWithKey getLastOperation() {
        return lastOperation;
    }
}