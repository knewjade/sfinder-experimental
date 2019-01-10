package line.step2;

import common.datastore.Operation;
import common.datastore.Operations;
import common.datastore.PieceCounter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import line.commons.*;

import java.util.*;
import java.util.stream.Stream;

class Runner {
    private static final PieceCounter ALL_PIECE_COUNTER = new PieceCounter(Piece.valueList());

    private final int maxHeight;
    private final MinoFactory minoFactory;
    private final HashMap<Long, HashMap<Long, EnumMap<Piece, List<KeyOriginalPiece>>>> pieceMap;
    private final HashMap<Integer, List<MaskField>> maskFields;

    Runner(FactoryPool pool) {
        this(pool.getMinoFactory(), pool.getMaxHeight(), pool.getBlockMaskMapBoard2(), pool.getTSpinMaskFields(pool.getMaxHeight()));
    }

    private Runner(
            MinoFactory minoFactory, int maxHeight,
            HashMap<Long, HashMap<Long, EnumMap<Piece, List<KeyOriginalPiece>>>> pieceMap, HashMap<Integer, List<MaskField>> maskFields
    ) {
        this.maxHeight = maxHeight;
        this.minoFactory = minoFactory;
        this.pieceMap = pieceMap;
        this.maskFields = maskFields;
    }

    Stream<Operations> search(List<Operation> operationList) {
        Candidate candidate = EmptyCandidate.create(minoFactory, maxHeight, operationList);

        // 必ずTが含まれていること
        OperationsWithT operationsWithT = new OperationsWithT(candidate.getOperationList(), minoFactory, maxHeight);

        // Tがない地形でライン消去が発生するとき
        Field fieldWithoutT = operationsWithT.newFieldWithoutT();
        if (0 < fieldWithoutT.freeze().clearLine()) {
            return Stream.empty();
        }

        // Tミノを取得
        // Tミノが入れば、Tスピンになる
        if (operationsWithT.isTSpin()) {
            return Stream.of(operationsWithT.newOperations());
        }

        // 使っていないミノを置いてみてTスピンができないか探索
        Solutions solutions = new Solutions();

        Operation operationT = operationsWithT.getT();
        int index = operationT.getX() + operationT.getY() * 10;

        // 探索開始時のライン消去数を記録
        int clearedLine = candidate.newField().clearLine();

        // Tの回転軸より下にブロックがない
        // y=0のT-Spin Miniは床が使用されるため、Tが最も低い位置にあるとき、T下を埋めないケースも探索する
        int minY = operationsWithT.newOperations().getOperations().stream()
                .mapToInt(operation -> {
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    return operation.getY() + mino.getMinY();
                })
                .min()
                .orElseGet(() -> 1);

        Stream.Builder<MaskField> builder = Stream.builder();
        int ty = operationT.getY();
        if (ty == minY) {
            int tx = operationT.getX();

            List<List<Integer>> diffs = Arrays.asList(
                    Collections.singletonList(-1),
                    Collections.singletonList(1),
                    Arrays.asList(-1, 1)
            );

            for (List<Integer> diff : diffs) {
                Field needBlock = FieldFactory.createField(maxHeight);

                for (Integer dx : diff) {
                    int x = tx + dx;
                    needBlock.setBlock(x, ty + 1);
                }

                Field notAllowed = FieldFactory.createField(maxHeight);

                for (int y = 0; y < ty; y++) {
                    for (int x = 0; x < 10; x++) {
                        notAllowed.setBlock(x, y);
                    }
                }

                builder.accept(new MaskField(needBlock, notAllowed));
            }
        }

        // まだブロックがない部分同じ形のマスクを取り除く
        return Stream.concat(this.maskFields.get(index).stream(), builder.build())
                .filter(maskField -> {
                    // 既に置くことができない場所にブロックがある
                    Field field = candidate.newField();
                    return maskField.getNotAllowed().canMerge(field);
                })
                .flatMap(maskField -> {
                    // Tスピンとして判定されるために必要なブロック
                    Field needBlock = maskField.getNeed().freeze();
                    needBlock.reduce(operationsWithT.newFieldWithoutT());

                    assert !needBlock.isPerfect();

                    // 置くことができないブロック
                    Field notAllowedBlock = maskField.getNotAllowed();

                    // 探索
                    return this.next(solutions, candidate, needBlock, notAllowedBlock, clearedLine);
                });
    }

    private Stream<Operations> next(
            Solutions solutions, Candidate candidate, Field needBlock, Field notAllowedBlock, int clearedLine
    ) {
        // 消去されるライン数が探索開始時から変わっていない
        if (clearedLine < candidate.newField().clearLine()) {
            return Stream.empty();
        }

        // すべてが埋まっている
        if (needBlock.isPerfect()) {
            solutions.add(candidate);
            return Stream.of(candidate.toOperations());
        }

        // すべてのミノを使い切った
        PieceCounter usingPieceCounter = candidate.newPieceCounter();
        PieceCounter remainderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);
        if (remainderPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        // 既に解として登録済み
        if (solutions.contains(candidate)) {
            return Stream.empty();
        }

        EnumMap<Piece, List<KeyOriginalPiece>> map = findPiecesFromMap(needBlock);

        return remainderPieceCounter.getBlockStream()
                .flatMap(piece -> {
                    // 実際にミノをおく
                    return map.get(piece).stream()
                            .filter(candidate::canPut)
                            .filter(keyOriginalPiece -> notAllowedBlock.canPut(keyOriginalPiece.getOriginalPiece()))
                            .flatMap(keyOriginalPiece -> {
                                // 埋める必要があるブロック
                                Field freezeNeedBlock = needBlock.freeze();
                                freezeNeedBlock.remove(keyOriginalPiece.getOriginalPiece());

                                // これまでの操作をリストにする
                                Candidate nextCandidate = new RecursiveCandidate(candidate, keyOriginalPiece);
                                return this.next(solutions, nextCandidate, freezeNeedBlock, notAllowedBlock, clearedLine);
                            });
                });
    }

    // フィールドから1ブロックを取り出して、そのブロックを埋めるミノ一覧を取得
    private EnumMap<Piece, List<KeyOriginalPiece>> findPiecesFromMap(Field needBlock) {
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
