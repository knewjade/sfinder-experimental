package line.step2;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import commons.Commons;
import core.field.Field;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import line.commons.FactoryPool;
import line.commons.LineCommons;

import java.util.*;
import java.util.stream.Stream;

class Runner {
    private static final PieceCounter ALL_PIECE_WITHOUT_T = new PieceCounter(
            Piece.valueList().stream().filter(piece -> piece != Piece.T)
    );

    private final int maxHeight;
    private final MinoFactory minoFactory;
    private final HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> pieceMap;
    private final HashMap<Integer, List<Field>> maskFields;
    private final EnumMap<Piece, PieceCounter> allPieceCounters;


    Runner(FactoryPool pool) {
        this(pool.getMinoFactory(), pool.getMaxHeight(), pool.getBlockMaskMapBoard2(), pool.getTSpinMaskFields(pool.getMaxHeight()));
    }

    private Runner(
            MinoFactory minoFactory, int maxHeight,
            HashMap<Long, HashMap<Long, EnumMap<Piece, List<OriginalPiece>>>> pieceMap, HashMap<Integer, List<Field>> maskFields
    ) {
        this.maxHeight = maxHeight;
        this.minoFactory = minoFactory;
        this.pieceMap = pieceMap;
        this.maskFields = maskFields;
        this.allPieceCounters = LineCommons.getAllPieceCounters();
    }

    Stream<Operations> search(List<Operation> operationList) {
        Field fieldWithoutT = LineCommons.toFieldWithoutT(minoFactory, operationList, maxHeight);

        // Tがない地形でライン消去が発生するとき
        int clearLine = fieldWithoutT.freeze().clearLine();
        if (0 < clearLine) {
            return Stream.empty();
        }

        // Tミノを取得
        // 必ずTが含まれていること
        Optional<? extends Operation> optional = operationList.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst();
        assert optional.isPresent();
        Operation operationT = optional.get();

        // Tミノが入れば、Tスピンになる
        if (Commons.isTSpin(fieldWithoutT, operationT.getX(), operationT.getY())) {
            // 解
            return Stream.of(new Operations(operationList));
        }

        // 使っていないミノを置いてみてTスピンができないか探索
        return localSearch(operationList, operationT, fieldWithoutT);
    }

    private Stream<Operations> localSearch(List<Operation> operationList, Operation operationT, Field fieldWithoutT) {
        PieceCounter usingPieceCounter = new PieceCounter(
                operationList.stream().map(Operation::getPiece).filter(piece -> piece != Piece.T)
        );
        PieceCounter restPieceCounter = ALL_PIECE_WITHOUT_T.removeAndReturnNew(usingPieceCounter);

        if (restPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        int index = operationT.getX() + operationT.getY() * 10;
        return maskFields.get(index).stream()
                .flatMap(maskField -> {
                    // Tスピンとして判定されるために必要なブロック
                    Field needBlock = maskField.freeze();
                    needBlock.reduce(fieldWithoutT);

                    assert !needBlock.isPerfect();

                    // 探索
                    Field field = LineCommons.toField(minoFactory, operationList, maxHeight);
                    return this.next(operationList, restPieceCounter, field, needBlock);
                });
    }

    private Stream<Operations> next(List<Operation> operationList, PieceCounter restPieceCounter, Field field, Field needBlock) {
        // すべてが埋まっている
        if (needBlock.isPerfect()) {
            return Stream.of(new Operations(operationList));
        }

        // すべてのミノを使い切った
        if (restPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        EnumMap<Piece, List<OriginalPiece>> map = findPiecesFromMap(needBlock);

        return restPieceCounter.getBlockStream()
                .flatMap(piece -> {
                    // 次で使えるミノ
                    PieceCounter nextRestPieceCounter = restPieceCounter.removeAndReturnNew(allPieceCounters.get(piece));

                    // 実際にミノをおく
                    return map.get(piece).stream()
                            .filter(originalPiece -> field.canMerge(originalPiece.getMinoField()))
                            .flatMap(originalPiece -> {
                                // 次の地形
                                Field freeze = field.freeze();
                                freeze.put(originalPiece);

                                // 埋める必要があるブロック
                                Field freezeNeedBlock = needBlock.freeze();
                                freezeNeedBlock.remove(originalPiece);

                                // これまでの操作をリストにする
                                ArrayList<Operation> operations = new ArrayList<>(operationList);
                                operations.add(originalPiece);
                                return this.next(operations, nextRestPieceCounter, freeze, freezeNeedBlock);
                            });
                });
    }

    // フィールドから1ブロックを取り出して、そのブロックを埋めるミノ一覧を取得
    private EnumMap<Piece, List<OriginalPiece>> findPiecesFromMap(Field needBlock) {
        long lowBoard = needBlock.getBoard(0);

        if (lowBoard != 0L) {
            long bit = lowBoard & (-lowBoard);
            return this.pieceMap.get(bit).get(0L);
        }

        long highBoard = needBlock.getBoard(1);
        long bit = highBoard & (-highBoard);
        return this.pieceMap.get(0L).get(bit);
    }
}
