package line.step3;

import common.datastore.Operation;
import common.datastore.PieceCounter;
import core.field.Field;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.srs.Rotate;
import line.commons.FactoryPool;
import line.commons.LineCommons;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Runner {
    private static final PieceCounter ALL_PIECE_COUNTER = new PieceCounter(Piece.valueList());

    private final EnumMap<Piece, PieceCounter> allPieceCounters;
    private final MinoFactory minoFactory;
    private final int maxHeight;
    private final Scaffolds scaffolds;

    Runner(FactoryPool factoryPool) {
        this.allPieceCounters = LineCommons.getAllPieceCounters();
        this.minoFactory = factoryPool.getMinoFactory();
        this.maxHeight = factoryPool.getMaxHeight();
        this.scaffolds = Scaffolds.create(factoryPool);
    }

    public Stream<SlideOperations> run(SlideOperations operations) {
        if (existsAllOnGround(operations)) {
            return Stream.of(operations);
        }

        // 使用していないミノ
        PieceCounter usingPieceCounter = operations.getPieceCounter();
        PieceCounter reminderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        if (reminderPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        return this.localSearch(operations, reminderPieceCounter, new HashSet<>(), new HashSet<>());
    }

    // すべてのミノが地面 or 他のミノの上にあるか
    private boolean existsAllOnGround(SlideOperations operations) {
        List<Operation> slideOperationList = operations.getGroundSlideOperationList();

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

    // 空中に浮いてミノの下にミノを置いて探索
    private Stream<SlideOperations> localSearch(
            SlideOperations operations, PieceCounter reminderPieceCounter,
            Set<Integer> currentKeySet, Set<Set<Integer>> solutionKeySet
    ) {
        List<Operation> initAirOperations = operations.getAirSlideOperationList();

        List<Operation> airOperations = extractAirOperations(initAirOperations);

        Operation tOperation = initAirOperations.stream()
                .filter(operation -> operation.getPiece() == Piece.T)
                .findFirst()
                .orElse(null);

        assert tOperation != null;

        Stream.Builder<SlideOperations> candidates = Stream.builder();
        Stream.Builder<SlideOperations> results = Stream.builder();
        HashSet<Integer> visitedIndexSet = new HashSet<>();

        Field initField = LineCommons.toField(minoFactory, initAirOperations, maxHeight);

        for (Operation air : airOperations) {
            List<KeyOriginalPiece> scaffoldPieces = scaffolds.get(air);

            PIECE_LOOP:
            for (KeyOriginalPiece scaffoldPiece : scaffoldPieces) {
                // 既に探索されたか
                int index = scaffoldPiece.getIndex();
                if (visitedIndexSet.contains(index)) {
                    continue;
                }
                visitedIndexSet.add(index);

                // そのミノがまだ使われていない
                PieceCounter currentPieceCounter = allPieceCounters.get(scaffoldPiece.getPiece());
                if (!reminderPieceCounter.containsAll(currentPieceCounter)) {
                    continue;
                }

                // フィールドに置くスペースがある
                if (!initField.canPut(scaffoldPiece.getOriginalPiece())) {
                    continue;
                }

                // 部分的な組み合わせでまだ解が見つかっていない
                // キーはTとの相対的な位置
                Set<Integer> keySet = new HashSet<>(currentKeySet);

                int currentKey = toRelatedKey(scaffoldPiece.getPiece(), scaffoldPiece.getRotate(), scaffoldPiece.getX(), scaffoldPiece.getY() - tOperation.getY());
                keySet.add(currentKey);

                for (int prevKey : currentKeySet) {
                    keySet.remove(prevKey);

                    if (solutionKeySet.contains(keySet)) {
                        continue PIECE_LOOP;
                    }

                    keySet.add(prevKey);
                }

                // 解であるか
                RecursiveSlideOperations nextOperations = new RecursiveSlideOperations(minoFactory, initAirOperations, scaffoldPiece);
                if (!existsAllOnGround(nextOperations)) {
                    // まだ未使用のミノが残っているので、次の探索へ進む
                    PieceCounter nextPieceCounter = reminderPieceCounter.removeAndReturnNew(currentPieceCounter);
                    if (nextPieceCounter.getCounter() != 0L) {
                        candidates.accept(nextOperations);
                    }

                    continue;
                }

                solutionKeySet.add(new HashSet<>(keySet));
                results.accept(nextOperations);
            }
        }

        Stream<SlideOperations> nextResults = candidates.build()
                .flatMap(slideOperations -> {
                    KeyOriginalPiece scaffoldPiece = slideOperations.getKeyOriginalPiece();

                    PieceCounter usingPieceCounter = slideOperations.getPieceCounter();
                    PieceCounter nextPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

                    Set<Integer> keySet = new HashSet<>(currentKeySet);

                    int currentKey = toRelatedKey(scaffoldPiece.getPiece(), scaffoldPiece.getRotate(), scaffoldPiece.getX(), scaffoldPiece.getY() - tOperation.getY());
                    keySet.add(currentKey);

                    return localSearch(slideOperations, nextPieceCounter, keySet, solutionKeySet);
                });

        return Stream.concat(results.build(), nextResults);
    }

    // すべてのミノが地面 or 他のミノの上にあるか
    private List<Operation> extractAirOperations(List<? extends Operation> slideOperationList) {
        List<Operation> slideOperationsWithoutT = slideOperationList.stream()
                .filter(operation -> operation.getPiece() != Piece.T)
                .collect(Collectors.toList());

        Field field = LineCommons.toField(minoFactory, slideOperationsWithoutT, maxHeight);

        return slideOperationList.stream()
                .filter(operation -> {
                    Field freeze = field.freeze();
                    Mino mino = minoFactory.create(operation.getPiece(), operation.getRotate());
                    int x = operation.getX();
                    int y = operation.getY();
                    freeze.remove(mino, x, y);
                    return !freeze.isOnGround(mino, x, y);
                })
                .collect(Collectors.toList());
    }

    private int toRelatedKey(Piece piece, Rotate rotate, int x, int y) {
        return piece.getNumber() * 4 * 48 * 10
                + rotate.getNumber() * 48 * 10
                + (y + 24) * 10
                + x;
    }
}
