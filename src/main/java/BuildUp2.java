import common.datastore.MinoOperationWithKey;
import core.action.reachable.LockedReachable;
import core.action.reachable.Reachable;
import core.field.Field;
import core.field.KeyOperators;
import core.mino.Mino;
import core.mino.Piece;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

public class BuildUp2 {
    // block順番で組み立てられる手順が存在するかチェックする
    // operationsで使用するミノとblocksが一致していること
    public static boolean existsValidByOrderWithHold(
            LockedReachable lockedReachable, boolean isHold,
            Field field, Stream<? extends MinoOperationWithKey> operations, List<Piece> pieces, int height, Reachable reachable, int maxDepth
    ) {
        EnumMap<Piece, LinkedList<MinoOperationWithKey>> eachBlocks = operations.sequential().collect(() -> new EnumMap<>(Piece.class), (blockLinkedListEnumMap, operationWithKey) -> {
            Piece piece = operationWithKey.getPiece();
            LinkedList<MinoOperationWithKey> operationWithKeys = blockLinkedListEnumMap.computeIfAbsent(piece, b -> new LinkedList<>());
            operationWithKeys.add(operationWithKey);
        }, EnumMap::putAll);

        return existsValidByOrderWithHold(lockedReachable, isHold, field.freeze(height), eachBlocks, pieces, height, reachable, maxDepth, 1, null, 0);
    }

    private static boolean existsValidByOrderWithHold(
            LockedReachable lockedReachable, boolean isHold,
            Field field, EnumMap<Piece, LinkedList<MinoOperationWithKey>> eachBlocks, List<Piece> pieces, int height, Reachable reachable, int maxDepth, int depth, Piece hold, int pieceIndex
    ) {
        long deleteKey = field.clearLineReturnKey();

        Piece piece = pieceIndex < pieces.size() ? pieces.get(pieceIndex) : null;

        if (piece != null && existsValidByOrderWithHold(lockedReachable, isHold, field, eachBlocks, pieces, height, reachable, maxDepth, depth, piece, deleteKey, hold, pieceIndex)) {
            return true;
        }

        if (isHold || piece == Piece.T || hold == Piece.T) {
            if (hold == null) {
                if (pieceIndex + 1 < pieces.size() && existsValidByOrderWithHold(lockedReachable, isHold, field, eachBlocks, pieces, height, reachable, maxDepth, depth, pieces.get(pieceIndex + 1), deleteKey, piece, pieceIndex + 1)) {
                    return true;
                }
            } else {
                if (existsValidByOrderWithHold(lockedReachable, isHold, field, eachBlocks, pieces, height, reachable, maxDepth, depth, hold, deleteKey, piece, pieceIndex)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean existsValidByOrderWithHold(
            LockedReachable lockedReachable, boolean isHold,
            Field field, EnumMap<Piece, LinkedList<MinoOperationWithKey>> eachBlocks, List<Piece> pieces, int height, Reachable reachable, int maxDepth, int depth, Piece usePiece, long deleteKey, Piece nextHoldPiece, int pieceIndex
    ) {
        LinkedList<MinoOperationWithKey> operationWithKeys = eachBlocks.get(usePiece);
        if (operationWithKeys == null) {
            return false;
        }

        if (usePiece == Piece.T && depth != maxDepth) {
            return false;
        }

        Reachable reachable2 = usePiece == Piece.T && depth == maxDepth ? lockedReachable : reachable;

        for (int index = 0; index < operationWithKeys.size(); index++) {
            MinoOperationWithKey key = operationWithKeys.remove(index);

            long needDeletedKey = key.getNeedDeletedKey();
            if ((deleteKey & needDeletedKey) != needDeletedKey) {
                // 必要な列が消えていない
                operationWithKeys.add(index, key);
                continue;
            }

            // すでに下のラインが消えているときは、その分スライドさせる
            int originalY = key.getY();
            int deletedLines = Long.bitCount(KeyOperators.getMaskForKeyBelowY(originalY) & deleteKey);

            Mino mino = key.getMino();
            int x = key.getX();
            int y = originalY - deletedLines;

            if (field.isOnGround(mino, x, y) && field.canPut(mino, x, y) && reachable2.checks(field, mino, x, y, height - mino.getMinY())) {
                if (depth == maxDepth)
                    return true;

                Field nextField = field.freeze(height);
                nextField.put(mino, x, y);
                nextField.insertBlackLineWithKey(deleteKey);

                boolean exists = existsValidByOrderWithHold(lockedReachable, isHold, nextField, eachBlocks, pieces, height, reachable, maxDepth, depth + 1, nextHoldPiece, pieceIndex + 1);
                if (exists)
                    return true;
            }

            operationWithKeys.add(index, key);
        }

        return false;
    }
}
