package line.step3;

import common.datastore.Operation;
import common.datastore.PieceCounter;
import core.field.Field;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.neighbor.OriginalPiece;
import line.commons.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Runner {
    private static final PieceCounter ALL_PIECE_COUNTER = new PieceCounter(Piece.valueList());

    private final MinoFactory minoFactory;
    private final int maxHeight;
    private final Scaffolds scaffolds;
    private final EnumMap<Piece, PieceCounter> allPieceCounters;
    private final AroundBlocks aroundBlock;

    Runner(FactoryPool factoryPool) {
        this.minoFactory = factoryPool.getMinoFactory();
        this.maxHeight = factoryPool.getMaxHeight();
        this.scaffolds = Scaffolds.create(factoryPool);
        this.allPieceCounters = LineCommons.getAllPieceCounters();
        this.aroundBlock = new AroundBlocks(maxHeight);
    }

    public Stream<SlideOperations> run(SlideOperations operations) {
        if (existsAllOnGround(operations)) {
            return Stream.of(operations);
        }
        return localSearch(operations);

    }

    @NotNull
    private Stream<SlideOperations> localSearch(SlideOperations operations) {
        // 使用していないミノ
        PieceCounter usingPieceCounter = operations.getPieceCounter();
        PieceCounter reminderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        if (reminderPieceCounter.getCounter() == 0L) {
            return Stream.empty();
        }

        PriorityQueue<SlideCandidate> candidates = new PriorityQueue<>(
                Comparator.comparingInt(o -> o.getSlideOperations().getRawOprationList().size())
        );
        SlideCandidate candidate = SlideCandidate.create(minoFactory, operations, maxHeight);
        candidates.add(candidate);

        // 消去されるライン数を記録
        int clearLine = candidate.newFieldOnGround().clearLine();

        Solutions solutions = new Solutions();

        Stream.Builder<SlideOperations> builder = Stream.builder();

        while (!candidates.isEmpty()) {
            SlideCandidate poll = candidates.poll();
            List<SlideCandidate> results = this.localSearch(poll, solutions, clearLine, builder);

            candidates.addAll(results);
        }

        return builder.build();
    }

    // すべてのミノが地面 or 他のミノの上にあるか
    private boolean existsAllOnGround(SlideOperations operations) {
        List<Operation> slideOperationList = operations.getGroundSlideOperationList();
        return LineCommons.existsAllOnGround(minoFactory, slideOperationList, maxHeight);
    }

    // 空中に浮いてミノの下にミノを置いて探索
    private List<SlideCandidate> localSearch(SlideCandidate candidate, Solutions solutions, int clearLine, Stream.Builder<SlideOperations> builder) {
        SlideOperations operations = candidate.getSlideOperations();
        List<Operation> initAirOperations = operations.getAirSlideOperationList();

        // 浮いているミノを取得する
        List<Operation> airOperations = extractAirOperations(initAirOperations);

        OperationsWithT operationsWithT = new OperationsWithT(initAirOperations, minoFactory, maxHeight);
        Operation operationT = operationsWithT.getT();
        Field initField = operationsWithT.newField();

        // ローカル内で探索済みのミノにマークする
        // 複数のミノから同じ足場用ミノが選択される可能性があるため
        Set<KeyOriginalPiece> visitedPieceSet = new HashSet<>();

        // 残りのミノを取得
        PieceCounter usingPieceCounter = operations.getPieceCounter();
        PieceCounter reminderPieceCounter = ALL_PIECE_COUNTER.removeAndReturnNew(usingPieceCounter);

        // Tのまわりにブロックを置かない
        Field notAllowedBlock = aroundBlock.get(operationT.getX(), operationT.getY());

        List<SlideCandidate> candidates = new ArrayList<>();

        for (Operation air : airOperations) {
            List<KeyOriginalPiece> scaffoldPieces = scaffolds.get(air);

            for (KeyOriginalPiece scaffoldPiece : scaffoldPieces) {
                // 既に探索されたか
                if (visitedPieceSet.contains(scaffoldPiece)) {
                    continue;
                }
                visitedPieceSet.add(scaffoldPiece);

                // そのミノがまだ使われていない
                PieceCounter currentPieceCounter = allPieceCounters.get(scaffoldPiece.getPiece());
                if (!reminderPieceCounter.containsAll(currentPieceCounter)) {
                    continue;
                }

                OriginalPiece originalPiece = scaffoldPiece.getOriginalPiece();

                // フィールドに置くスペースがある
                if (!initField.canPut(originalPiece)) {
                    continue;
                }

                // 置いてはいけないスペースではない
                if (!notAllowedBlock.canPut(originalPiece)) {
                    continue;
                }

                // 部分的な組み合わせでまだ解が見つかっていない
                // キーはTとの相対的な位置
                int currentKey = toRelatedIndex(scaffoldPiece, operationT);
                if (solutions.partialContains(candidate.getKeys(), currentKey)) {
                    continue;
                }

                RecursiveSlideOperations nextOperations = new RecursiveSlideOperations(minoFactory, initAirOperations, scaffoldPiece);
                SlideCandidate nextCandidate = SlideCandidate.create(minoFactory, nextOperations, candidate.nextKey(currentKey), maxHeight);

                // 消去されるライン数が変わらない
                int nextClearLine = nextCandidate.newFieldOnGround().clearLine();
                if (nextClearLine != clearLine) {
                    continue;
                }

                // まだ未使用のミノが残っているので、次の探索へ進む
                // 解の場合でも、さらに足場が高いケースがあるかもしれないため、次の探索へ進む
                PieceCounter nextPieceCounter = reminderPieceCounter.removeAndReturnNew(currentPieceCounter);
                if (nextPieceCounter.getCounter() != 0L) {
                    candidates.add(nextCandidate);
                }

                // 解であるか
                if (existsAllOnGround(nextOperations)) {
                    solutions.add(nextCandidate.getKeys());
                    builder.accept(nextOperations);
                }
            }
        }

        return candidates;
    }

    // 特定のミノからの相対的な位置を返す
    private int toRelatedIndex(Operation target, Operation pivot) {
        int dy = target.getY() - pivot.getY();
        return target.getPiece().getNumber() * 4 * 48 * 10
                + target.getRotate().getNumber() * 48 * 10
                + (dy + 24) * 10
                + target.getX();
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
}
