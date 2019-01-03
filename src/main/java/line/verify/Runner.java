package line.verify;

import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.Operation;
import common.datastore.PieceCounter;
import core.action.reachable.LockedReachable;
import core.action.reachable.RotateReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import line.commons.FactoryPool;
import line.commons.LineCommons;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

public class Runner {
    private final int maxHeight;
    private final MinoFactory minoFactory;
    private final FactoryPool factoryPool;

    public Runner(FactoryPool factoryPool) {
        this.maxHeight = factoryPool.getMaxHeight();
        this.minoFactory = factoryPool.getMinoFactory();
        this.factoryPool = factoryPool;
    }

    boolean verify(List<Operation> operations) {
        // ミノの数が 1 <= n <= 7 である
        if (operations.size() < 1 || 7 < operations.size()) {
            return false;
        }

        // 使用しているミノの種類に重複がない
        PieceCounter usingPieceCounter = new PieceCounter(operations.stream().map(Operation::getPiece));
        OptionalInt maxOptional = usingPieceCounter.getEnumMap().values().stream().mapToInt(v -> v).max();
        assert maxOptional.isPresent();
        int maxPieceCount = maxOptional.getAsInt();
        if (1 < maxPieceCount) {
            return false;
        }

        // T以外のミノで組む手順が存在するか
        List<Operation> operationsWithoutT = operations.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());
        Field emptyField = FieldFactory.createField(maxHeight);
        List<MinoOperationWithKey> keysWithoutT = LineCommons.toOperationWithKeys(minoFactory, operationsWithoutT);
        LockedReachable lockedReachable = factoryPool.createLockedReachable();
        if (!BuildUp.existsValidBuildPattern(emptyField, keysWithoutT, maxHeight, lockedReachable)) {
            return false;
        }

        // Tの抽出
        Optional<Operation> tOptional = operations.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();

        if (!tOptional.isPresent()) {
            return false;
        }

        Operation tOperation = tOptional.get();

        Mino tMino = minoFactory.create(tOperation.getPiece(), tOperation.getRotate());
        int tx = tOperation.getX();
        int ty = tOperation.getY();

        // T以外のミノでライン消去が発生しない
        Field fieldWithoutT = LineCommons.toField(minoFactory, operationsWithoutT, maxHeight);
        if (0 < fieldWithoutT.freeze().clearLine()) {
            return false;
        }

        // Tミノを入れるスペースが存在する
        if (!fieldWithoutT.canPut(tMino, tx, ty)) {
            return false;
        }

        // Tミノが接着する
        if (!fieldWithoutT.isOnGround(tMino, tx, ty)) {
            return false;
        }

        // Tミノを最後に置いたとき、ライン消去が発生する
        Field freeze = fieldWithoutT.freeze();
        freeze.put(tMino, tx, ty);
        if (freeze.clearLine() == 0) {
            return false;
        }

        // Tミノの周りにブロックが存在する
        if (!LineCommons.isTSpin(fieldWithoutT, tOperation.getX(), tOperation.getY())) {
            return false;
        }

        // Tミノが回転入れできる
        RotateReachable rotateReachable = factoryPool.createRotateReachable();
        return rotateReachable.checks(fieldWithoutT, tMino, tx, ty, maxHeight);
    }
}
