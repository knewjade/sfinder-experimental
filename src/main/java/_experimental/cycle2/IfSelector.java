package _experimental.cycle2;

import common.SyntaxException;
import common.buildup.BuildUpStream;
import common.datastore.*;
import common.datastore.action.Action;
import common.datastore.blocks.LongPieces;
import common.datastore.blocks.Pieces;
import common.iterable.CombinationIterable;
import common.iterable.PermutationIterable;
import common.order.ForwardOrderLookUp;
import common.pattern.LoadedPatternGenerator;
import common.pattern.PatternGenerator;
import common.tetfu.TetfuElement;
import common.tetfu.common.ColorConverter;
import common.tetfu.field.ColoredField;
import common.tetfu.field.ColoredFieldFactory;
import common.tree.AnalyzeTree;
import concurrent.LockedCandidateThreadLocal;
import concurrent.LockedReachableThreadLocal;
import concurrent.checker.CheckerUsingHoldThreadLocal;
import concurrent.checker.invoker.CheckerCommonObj;
import concurrent.checker.invoker.using_hold.ConcurrentCheckerUsingHoldInvoker;
import core.action.reachable.LockedReachable;
import core.column_field.ColumnField;
import core.field.Field;
import core.field.FieldFactory;
import core.field.FieldView;
import core.field.SmallField;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import helper.EasyPath;
import helper.EasyPool;
import helper.EasyTetfu;
import searcher.pack.SeparableMinos;
import searcher.pack.SizedBit;
import searcher.pack.calculator.BasicSolutions;
import searcher.pack.memento.SRSValidSolutionFilter;
import searcher.pack.memento.SolutionFilter;
import searcher.pack.mino_fields.RecursiveMinoFields;
import searcher.pack.separable_mino.SeparableMino;
import searcher.pack.solutions.BasicSolutionsCalculator;
import searcher.pack.solutions.MappedBasicSolutions;
import searcher.pack.task.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class IfSelector {
    private static class Data {
        private final Field field;
        private final double percent;
        private final int count;

        Data(long field, double percent) {
            this(field, percent, -1);
        }

        Data(long field, double percent, int count) {
            this.field = new SmallField(field);
            this.percent = percent;
            this.count = count;
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException, SyntaxException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        int width = 3;
        int height = 4;
        int maxDepth = 6;

        EasyPool easyPool = new EasyPool();
        EasyPath easyPath = new EasyPath(easyPool);
        EasyTetfu easyTetfu = new EasyTetfu(easyPool);
        PatternGenerator blocksGenerator = new LoadedPatternGenerator("*p7");

        int fromDepth = blocksGenerator.getDepth();
        ForwardOrderLookUp lookUp = new ForwardOrderLookUp(maxDepth, fromDepth);
        List<Pieces> allPieces2 = blocksGenerator.blocksStream().collect(Collectors.toList());

        MinoFactory minoFactory = easyPool.getMinoFactory();
        MinoShifter minoShifter = easyPool.getMinoShifter();
        MinoRotation minoRotation = easyPool.getMinoRotation();

        LockedCandidateThreadLocal candidateThreadLocal = new LockedCandidateThreadLocal(height);
        CheckerUsingHoldThreadLocal<Action> checkerThreadLocal = new CheckerUsingHoldThreadLocal<>();
        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(minoFactory, minoShifter, minoRotation, height);
        CheckerCommonObj commonObj = new CheckerCommonObj(minoFactory, candidateThreadLocal, checkerThreadLocal, reachableThreadLocal);
        ConcurrentCheckerUsingHoldInvoker invoker = new ConcurrentCheckerUsingHoldInvoker(executorService, commonObj, fromDepth);

        PieceCounter allIncluded = new PieceCounter(Piece.valueList());

        // すべての100%地形を読み込み
        List<Data> list = Files.lines(Paths.get("output/cycle2/IJSO.csv"))
                .map(line -> line.split(","))
                .map(split -> new Data(Long.valueOf(split[0]), Double.valueOf(split[1])))
                .filter(data -> 0.94 <= data.percent)
                .collect(Collectors.toList());

        System.out.println(list.size());

        for (Data data : list) {
            Field initField = data.field;
            System.out.println(FieldView.toString(initField));

            // すべての検索するBlocks
            // パフェできる手順が対象
            List<Pair<Pieces, Boolean>> search = invoker.search(initField, allPieces2, height, maxDepth);
            List<Pieces> targetBlocks = search.stream()
                    .filter(Pair::getValue)
                    .map(Pair::getKey)
                    .collect(Collectors.toList());
            System.out.println("target block size: " + targetBlocks.size());

            // 条件で手順を分割
            // 2分岐させる
            EnumMap<Piece, EnumMap<Piece, Map<Boolean, List<Pieces>>>> firstChoice = new EnumMap<>(Piece.class);
            PermutationIterable<Piece> blockPermutations = new PermutationIterable<>(Piece.valueList(), 2);
            for (List<Piece> pieces : blockPermutations) {
                assert pieces.size() == 2;
                Piece firstPiece = pieces.get(0);
                Piece secondPiece = pieces.get(1);

                Map<Boolean, List<Pieces>> conditional = targetBlocks.stream()
                        .collect(Collectors.groupingBy(o -> {
                            List<Piece> pieceList = o.getPieces();
                            return pieceList.indexOf(firstPiece) < pieceList.indexOf(secondPiece);
                        }));

                EnumMap<Piece, Map<Boolean, List<Pieces>>> secondChoice = firstChoice.computeIfAbsent(firstPiece, block -> new EnumMap<>(Piece.class));
                secondChoice.put(secondPiece, conditional);
            }

            // すべてのパフェ手順を取得
            List<Result> results = easyPath.calculate(initField, width, height)
                    .stream()
                    .filter(result -> allIncluded.containsAll(new PieceCounter(result.getMemento().getOperationsStream(width).map(OperationWithKey::getPiece))))
                    .collect(Collectors.toList());
            System.out.println("result size: " + results.size());

            for (Result result : results) {
                // パフェ手順から組み立てられるミノ順を抽出
                List<MinoOperationWithKey> operationWithKeys = result.getMemento()
                        .getSeparableMinoStream(width)
                        .map(SeparableMino::toMinoOperationWithKey)
                        .collect(Collectors.toList());
                assert operationWithKeys.size() == maxDepth;

                String tetfu = easyTetfu.encodeUrl(initField, operationWithKeys, height);
                System.out.println(tetfu);

                LockedReachable reachable = easyPool.getLockedReachable(height);
                BuildUpStream buildUpStream = new BuildUpStream(reachable, height);
                AnalyzeTree possibleTree = getPossibleBuildingTree(initField, operationWithKeys, buildUpStream);

                for (Map.Entry<Piece, EnumMap<Piece, Map<Boolean, List<Pieces>>>> firstEntry : firstChoice.entrySet()) {
                    Piece firstPiece = firstEntry.getKey();
                    for (Map.Entry<Piece, Map<Boolean, List<Pieces>>> secondEntry : firstEntry.getValue().entrySet()) {
                        Piece secondPiece = secondEntry.getKey();
                        for (Map.Entry<Boolean, List<Pieces>> splitMap : secondEntry.getValue().entrySet()) {
                            Boolean isSatisfy = splitMap.getKey();
                            List<Pieces> piecesList = splitMap.getValue();
//                            boolean isFound = checksAll(lookUp, piecesList, possibleTree);
                            boolean isFound = checksAllPercent(lookUp, piecesList, possibleTree, 0.9);
//                            System.out.printf("%s < %s == %s -> %s%n", firstPiece, secondPiece, isSatisfy, isSucceed);

                            if (isFound)
                                System.out.println("##########################");
                        }
                    }
                }
            }
        }

        executorService.shutdown();
        System.exit(0);

        // 準備
        LockedReachable reachable = new LockedReachable(minoFactory, minoShifter, minoRotation, height);
        BuildUpStream buildUpStream = new BuildUpStream(reachable, height);


        ArrayList<Result> results = new ArrayList<>();
        Field initField = FieldFactory.createField(4);

        int size = results.size();

        assert 0 < size;

        System.out.println(size);
        System.out.println(size * (size - 1) / 2);

        CombinationIterable<Result> combination = new CombinationIterable<>(results, 2);
        for (List<Result> resultList : combination) {
            assert resultList.size() == 2;

            // パフェ手順から組み立てられるミノ順を抽出
            Result result1 = resultList.get(0);
            List<MinoOperationWithKey> operationWithKeys1 = result1.getMemento()
                    .getSeparableMinoStream(width)
                    .map(SeparableMino::toMinoOperationWithKey)
                    .collect(Collectors.toList());
            assert operationWithKeys1.size() == maxDepth;
            AnalyzeTree possibleTree1 = getPossibleBuildingTree(initField, operationWithKeys1, buildUpStream);

//            boolean noDeleteLine1 = result1.getMemento().getRawOperationsStream().allMatch(operationWithKey -> operationWithKey.getNeedDeletedKey() == 0L);

            // パフェ手順から組み立てられるミノ順を抽出
            Result result2 = resultList.get(1);
            List<MinoOperationWithKey> operationWithKeys2 = result2.getMemento()
                    .getSeparableMinoStream(width)
                    .map(SeparableMino::toMinoOperationWithKey)
                    .collect(Collectors.toList());
            assert operationWithKeys2.size() == maxDepth;
            AnalyzeTree possibleTree2 = getPossibleBuildingTree(initField, operationWithKeys2, buildUpStream);

//            boolean noDeleteLine2 = result2.getMemento().getRawOperationsStream().allMatch(operationWithKey -> operationWithKey.getNeedDeletedKey() == 0L);
//
//            // 両方ともライン消去が絡むときは探索カット
//            if (!noDeleteLine1 && !noDeleteLine2) {
//                System.out.print(".");
//                continue;
//            }

//            // すべてのパターンを網羅できるかチェック
//            boolean canAll = checksAll90(lookUp, allBlocks, possibleTree1, possibleTree2);
//            if (!canAll) {
//                System.out.print("*");
//                continue;
//            }

            CombinationIterable<Piece> blockCombinations = new CombinationIterable<>(Piece.valueList(), 2);
            for (List<Piece> pieceCombination : blockCombinations) {
                assert pieceCombination.size() == 2;
                System.out.println(pieceCombination);
                Piece piece1 = pieceCombination.get(0);
                Piece piece2 = pieceCombination.get(1);

                // 条件で手順を分割
//                Map<Boolean, List<Pieces>> conditional = allBlocks.stream()
//                        .collect(Collectors.groupingBy(o -> {
//                            List<Piece> blockList = o.getBlockList();
//                            return blockList.indexOf(piece1) < blockList.indexOf(piece2);
//                        }));
                // 条件ごとパフェ成功確率を計算する
//                Pair<AnalyzeTree, AnalyzeTree> pair1 = calcPerfectPercent(lookUp, conditional, possibleTree1);
//                Pair<AnalyzeTree, AnalyzeTree> pair2 = calcPerfectPercent(lookUp, conditional, possibleTree2);

                System.out.println();
                System.out.println(pieceCombination);
//                System.out.printf("pattern1 | %.2f | %.2f %n", pair1.getKey().getSuccessPercent(), pair1.getValue().getSuccessPercent());
//                System.out.printf("pattern2 | %.2f | %.2f %n", pair2.getKey().getSuccessPercent(), pair2.getValue().getSuccessPercent());
//
                String tetfu1 = easyTetfu.encodeUrl(initField, operationWithKeys1, height);
                System.out.println(tetfu1);

                String tetfu2 = easyTetfu.encodeUrl(initField, operationWithKeys1, height);
                System.out.println(tetfu2);
            }
        }
    }

    private static boolean checksAll90(ForwardOrderLookUp lookUp, List<Pieces> allBlocks, AnalyzeTree possibleTree1, AnalyzeTree possibleTree2) {
//        int need = (int) (allBlocks.size() * 0.8 );
        int count = 0;
        int no = 0;
        for (Pieces target : allBlocks) {
            boolean anyMatch = lookUp.parse(target.getPieces())
                    .map(LongPieces::new)
                    .anyMatch(blocks -> possibleTree1.isVisited(blocks) || possibleTree2.isVisited(blocks));
            if (anyMatch) {
                count++;
            } else {
                no++;
//                if (need < no)
//                    return false;
            }
        }
        double percent = (double) count / allBlocks.size();
        return 0.8 < percent;
    }

    private static boolean checksAll(ForwardOrderLookUp lookUp, List<Pieces> allBlocks, AnalyzeTree possibleTree) {
        return allBlocks.parallelStream()
                .map(Pieces::getPieces)
                .map(lookUp::parse)
                .map(stream -> stream.map(LongPieces::new))
                .allMatch(stream -> stream.anyMatch(possibleTree::isVisited));
    }

    private static boolean checksAllPercent(ForwardOrderLookUp lookUp, List<Pieces> allBlocks, AnalyzeTree possibleTree, double percent) {
        long succeedCount = allBlocks.parallelStream()
                .map(Pieces::getPieces)
                .map(lookUp::parse)
                .map(stream -> stream.map(LongPieces::new))
                .filter(stream -> stream.anyMatch(possibleTree::isVisited))
                .count();
        return percent <= succeedCount / allBlocks.size();
    }

    private static boolean checksAll(ForwardOrderLookUp lookUp, List<Pieces> allBlocks, AnalyzeTree possibleTree1, AnalyzeTree possibleTree2) {
        for (Pieces target : allBlocks) {
            boolean anyMatch = lookUp.parse(target.getPieces())
                    .map(LongPieces::new)
                    .anyMatch(blocks -> possibleTree1.isVisited(blocks) || possibleTree2.isVisited(blocks));
            if (!anyMatch)
                return false;
        }
        return true;
    }

    private static BasicSolutions createMappedBasicSolutions(SizedBit sizedBit) {
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        SeparableMinos separableMinos = SeparableMinos.createSeparableMinos(minoFactory, minoShifter, sizedBit);
        BasicSolutionsCalculator calculator = new BasicSolutionsCalculator(separableMinos, sizedBit);
        Map<ColumnField, RecursiveMinoFields> calculate = calculator.calculate();
        return new MappedBasicSolutions(calculate);
    }

    private static SolutionFilter createSRSSolutionFilter(SizedBit sizedBit, Field initField) {
        LockedReachableThreadLocal lockedReachableThreadLocal = new LockedReachableThreadLocal(sizedBit.getHeight());
        return new SRSValidSolutionFilter(initField, lockedReachableThreadLocal, sizedBit);
    }

    private static Pair<AnalyzeTree, AnalyzeTree> calcPerfectPercent(ForwardOrderLookUp lookUp, Map<Boolean, List<Pieces>> conditional, AnalyzeTree possibleTree1) {
        AnalyzeTree resultTreeTrue = new AnalyzeTree();
        for (Pieces pieces : conditional.get(true)) {
            boolean anyMatch = lookUp.parse(pieces.getPieces())
                    .map(LongPieces::new)
                    .anyMatch(possibleTree1::isVisited);
            resultTreeTrue.set(anyMatch, pieces);
        }

        AnalyzeTree resultTreeFalse = new AnalyzeTree();
        for (Pieces pieces : conditional.get(false)) {
            boolean anyMatch = lookUp.parse(pieces.getPieces())
                    .map(LongPieces::new)
                    .anyMatch(possibleTree1::isVisited);
            resultTreeFalse.set(anyMatch, pieces);
        }

        return new Pair<>(resultTreeTrue, resultTreeFalse);
    }

    private static AnalyzeTree getPossibleBuildingTree(Field field, List<MinoOperationWithKey> operationWithKeys, BuildUpStream buildUpStream) {
        Set<LongPieces> canBuild = buildUpStream.existsValidBuildPattern(field, operationWithKeys)
                .map(List::stream)
                .map(stream -> stream.map(OperationWithKey::getPiece))
                .map(LongPieces::new)
                .collect(Collectors.toSet());

        AnalyzeTree tree1 = new AnalyzeTree();
        for (LongPieces blocks : canBuild) {
            tree1.success(blocks);
        }
        return tree1;
    }

    private static List<TetfuElement> parseTetfuElements(Field initField, ColorConverter colorConverter, Operations operations) {
        ColoredField grayField = ColoredFieldFactory.createGrayField(initField);
        List<? extends Operation> operationsList = operations.getOperations();
        ArrayList<TetfuElement> elements = new ArrayList<>();
        for (int index = 0; index < operationsList.size(); index++) {
            Operation o = operationsList.get(index);
            if (index == 0)
                elements.add(new TetfuElement(grayField, colorConverter.parseToColorType(o.getPiece()), o.getRotate(), o.getX(), o.getY()));
            else
                elements.add(new TetfuElement(colorConverter.parseToColorType(o.getPiece()), o.getRotate(), o.getX(), o.getY()));
        }
        return elements;
    }
}
