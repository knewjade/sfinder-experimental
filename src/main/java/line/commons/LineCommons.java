package line.commons;

import common.datastore.*;
import commons.Commons;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.srs.Rotate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LineCommons {
    // Fieldに変換
    public static Field toField(MinoFactory minoFactory, List<? extends Operation> operationList, int maxHeight) {
        Field field = FieldFactory.createField(maxHeight);
        for (Operation operation : operationList) {
            Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
            field.put(mino, operation.getX(), operation.getY());
        }
        return field;
    }

    // Operationをy軸でスライドさせる
    public static List<Operation> slideOperations(List<? extends Operation> operations, int slideY) {
        if (slideY == 0) {
            return new ArrayList<>(operations);
        }

        return operations.stream()
                .map(operation -> toOperation(operation, slideY))
                .collect(Collectors.toList());
    }

    @NotNull
    private static Operation toOperation(Operation operation, int slideY) {
        int operationX = operation.getX();
        int operationY = operation.getY() + slideY;
        return new SimpleOperation(operation.getPiece(), operation.getRotate(), operationX, operationY);
    }

    // Operation -> MinoOperationWithKey
    public static <T extends Operation> List<MinoOperationWithKey> toOperationWithKeys(MinoFactory minoFactory, List<T> operations) {
        return operations.stream()
                .map(operation -> toMinimalOperationWithKey(minoFactory, operation))
                .collect(Collectors.toList());
    }

    @NotNull
    private static MinimalOperationWithKey toMinimalOperationWithKey(MinoFactory minoFactory, Operation operation) {
        Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
        int operationX = operation.getX();
        int operationY = operation.getY();
        return new MinimalOperationWithKey(mino, operationX, operationY, 0L);
    }

    // Tミノ以外をFieldに変換
    public static Field toFieldWithoutT(MinoFactory minoFactory, List<? extends Operation> operationList, int maxHeight) {
        Field field = FieldFactory.createField(maxHeight);
        for (Operation operation : operationList) {
            if (operation.getPiece() == Piece.T) {
                continue;
            }

            Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
            field.put(mino, operation.getX(), operation.getY());
        }
        return field;
    }

    // 各ミノ単体のPieceCounterに変換
    public static EnumMap<Piece, PieceCounter> getAllPieceCounters() {
        EnumMap<Piece, PieceCounter> pieceCounters = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.valueList()) {
            pieceCounters.put(piece, new PieceCounter(Stream.of(piece)));
        }
        return pieceCounters;
    }

    // 最も低いブロックのy座標を取得
    public static int getMinY(MinoFactory minoFactory, List<? extends Operation> operationsList) {
        return operationsList.stream()
                .mapToInt(operation -> {
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    return operation.getY() + mino.getMinY();
                })
                .min()
                .orElse(-1);
    }

    public static int toKey(Piece piece, Rotate rotate, int x, int y) {
        return piece.getNumber() * 4 * 24 * 10
                + rotate.getNumber() * 24 * 10
                + y * 10
                + x;
    }

    public static String toURL(String data) {
        return "http://fumen.zui.jp/?v115@" + data;
    }

    public static PieceCounter toPieceCounter(List<? extends Operation> operations) {
        return new PieceCounter(operations.stream().map(Operation::getPiece));
    }

    public static boolean existsAllOnGround(MinoFactory minoFactory, List<Operation> slideOperationList, int maxHeight) {
        List<Operation> slideOperationsWithoutT = slideOperationList.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());

        Field field = LineCommons.toField(minoFactory, slideOperationsWithoutT, maxHeight);

        return slideOperationList.stream()
                .allMatch(operation -> {
                    Field freeze = field.freeze();
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    int x = operation.getX();
                    int y = operation.getY();
                    freeze.remove(mino, x, y);
                    return freeze.isOnGround(mino, x, y);
                });
    }

    public static boolean isTSpin(Field field, int x, int y) {
        return Commons.isTSpin(field, x, y);
    }
}