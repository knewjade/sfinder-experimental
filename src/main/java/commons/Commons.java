package commons;

import common.datastore.BlockField;
import common.datastore.FullOperationWithKey;
import common.datastore.MinoOperationWithKey;
import common.tetfu.common.ColorConverter;
import common.tetfu.common.ColorType;
import common.tetfu.field.ArrayColoredField;
import common.tetfu.field.ColoredField;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Commons {
    @NotNull
    public static List<MinoOperationWithKey> slideDownToGround(List<MinoOperationWithKey> operationWithKeys, Field initField, int maxHeight) {
        Field field = Commons.toField(operationWithKeys, initField, maxHeight);

        // 一番下が埋まるようにスライドする
        int lowerY = field.getLowerY();
        if (lowerY == 0) {
            return operationWithKeys;
        }

        return slideDownList(operationWithKeys, lowerY);
    }

    public static List<MinoOperationWithKey> slideDownList(List<MinoOperationWithKey> operationWithKeys, int slideDownY) {
        return operationWithKeys.stream()
                .map(operation -> {
                    int x = operation.getX();
                    int y = operation.getY();
                    long needDeletedKey = operation.getNeedDeletedKey();
                    long usingKey = operation.getUsingKey();
                    return new FullOperationWithKey(operation.getMino(), x, y - slideDownY, needDeletedKey, usingKey);
                })
                .collect(Collectors.toList());
    }

    @NotNull
    public static List<MinoOperationWithKey> slideUpTo4(List<MinoOperationWithKey> operationWithKeys, Field initField, int maxHeight) {
        Field field = Commons.toField(operationWithKeys, initField, maxHeight);

        // 一番下がy=3になるようにスライドする
        int lowerY = field.getLowerY();
        int slide = lowerY - 3;
        if (slide == 0) {
            return operationWithKeys;
        }

        return slideDownList(operationWithKeys, slide);
    }

    @NotNull
    public static CandidateObj slideUpCandidateTo4(CandidateObj candidate, Field initField, int maxHeight) {
        List<MinoOperationWithKey> allOperations = candidate.getAllOperations();
        Field field = Commons.toField(allOperations, initField, maxHeight);

        // 一番下がy=3になるようにスライドする
        int lowerY = field.getLowerY();
        int slide = lowerY - 3;
        return candidate.slideDown(slide);
    }

    public static Field toField(List<MinoOperationWithKey> operationWithKeys, Field initField, int maxHeight) {
        Field field = initField.freeze(maxHeight);
        for (MinoOperationWithKey operation : operationWithKeys) {
            Field piece = FieldFactory.createField(maxHeight);
            piece.put(operation.getMino(), operation.getX(), operation.getY());
            piece.insertWhiteLineWithKey(operation.getNeedDeletedKey());
            field.merge(piece);
        }
        return field;
    }

    public static BlockField toBlockField(List<MinoOperationWithKey> operationWithKeys, int maxHeight) {
        BlockField field = new BlockField(maxHeight);
        for (MinoOperationWithKey operation : operationWithKeys) {
            Field piece = FieldFactory.createField(maxHeight);
            piece.put(operation.getMino(), operation.getX(), operation.getY());
            piece.insertWhiteLineWithKey(operation.getNeedDeletedKey());
            field.merge(piece, operation.getPiece());
        }
        return field;
    }

    public static BlockField toMirror(BlockField original) {
        int maxHeight = original.getHeight();
        BlockField mirrored = new BlockField(maxHeight);
        for (Piece piece : Piece.values()) {
            Field field = original.get(piece);
            Field freeze = field.freeze(maxHeight);
            freeze.mirror();
            mirrored.merge(freeze, mirroredPiece(piece));
        }
        return mirrored;
    }

    private static Piece mirroredPiece(Piece piece) {
        switch (piece) {
            case I:
            case O:
            case T:
                return piece;
            case J:
                return Piece.L;
            case L:
                return Piece.J;
            case S:
                return Piece.Z;
            case Z:
                return Piece.S;
        }
        throw new IllegalArgumentException("Illegal piece type: type=" + piece);
    }

    public static ColoredField toColoredField(BlockField blockField, ColorConverter converter) {
        int maxHeight = blockField.getHeight();
        ArrayColoredField coloredField = new ArrayColoredField(24);
        for (int y = 0; y < maxHeight; y++) {
            for (int x = 0; x < 10; x++) {
                Piece piece = blockField.getBlock(x, y);
                ColorType colorType = converter.parseToColorType(piece);
                if (colorType != null && ColorType.isMinoBlock(colorType))
                    coloredField.setColorType(colorType, x, y);
            }
        }
        return coloredField;
    }

    public static boolean isTSpin(Field field, int x, int y) {
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
