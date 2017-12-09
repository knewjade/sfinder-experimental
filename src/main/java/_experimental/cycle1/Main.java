package _experimental.cycle1;

import common.SyntaxException;
import common.buildup.BuildUp;
import common.comparator.PiecesNameComparator;
import common.datastore.PieceCounter;
import common.datastore.OperationWithKey;
import common.datastore.blocks.Pieces;
import common.datastore.blocks.LongPieces;
import common.order.ForwardOrderLookUp;
import common.pattern.LoadedPatternGenerator;
import common.pattern.PatternGenerator;
import core.action.reachable.LockedReachable;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.srs.MinoRotation;
import helper.EasyPath;
import helper.EasyTetfu;
import searcher.pack.memento.MinoFieldMemento;
import searcher.pack.separable_mino.SeparableMino;
import searcher.pack.task.Result;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main {
    private static final List<Piece> ALL_PIECES = Piece.valueList();

    static {
        ALL_PIECES.sort(Comparator.comparing(Piece::getName));
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, SyntaxException {
        // フィールドの指定
        Field initField = FieldFactory.createField("" +
                "XXXXX_____" +
                "XXXXXX____" +
                "XXXXXXX___" +
                "XXXXXX____"
        );

        PieceCounter allPieceCounter = new PieceCounter(Arrays.asList(Piece.I, Piece.I, Piece.T, Piece.S, Piece.Z, Piece.O, Piece.J, Piece.L));

        int width = 3;
        int height = 4;
        EasyPath easyPath = new EasyPath();
        List<Result> results = easyPath.calculate(initField, width, height).stream()
                .filter(result -> {
                    MinoFieldMemento memento = result.getMemento();
                    PieceCounter pieceCounter = memento.getSumBlockCounter();
                    return allPieceCounter.containsAll(pieceCounter);
                })
                .collect(Collectors.toList());

        EasyTetfu easyTetfu = new EasyTetfu();
        for (Result result : results) {
            List<OperationWithKey> operationWithKeys = result.getMemento().getOperationsStream(width)
                    .collect(Collectors.toList());
            System.out.println(easyTetfu.encodeUrl(initField, operationWithKeys, height));
        }

        System.out.println(results.size());

        System.out.println("sequence," + ALL_PIECES.stream().map(Piece::getName).collect(Collectors.joining(",")));

        int cnt = 0;
        int suc = 0;
        LockedReachable reachable = new LockedReachable(new MinoFactory(), new MinoShifter(), new MinoRotation(), height);
        ForwardOrderLookUp lookUp = new ForwardOrderLookUp(4, 5);

        PatternGenerator blocksGenerator = new LoadedPatternGenerator("I, *p4");
        List<Pieces> allBlocks = blocksGenerator.blocksStream().collect(Collectors.toList());
        allBlocks.sort(new PiecesNameComparator());

        for (Pieces pieces : allBlocks) {
            PieceCounter counter = new PieceCounter(pieces.blockStream());
            Set<Piece> holdPieces = results.stream()
                    .map(Result::getMemento)
                    .filter(memento -> {
                        PieceCounter pieceCounter = memento.getSumBlockCounter();
                        return counter.containsAll(pieceCounter);
                    })
                    .filter(memento -> {
                        PieceCounter perfectUsingPieceCounter = memento.getSumBlockCounter();
                        return lookUp.parse(pieces.getPieces())
                                .map(stream -> stream.limit(4L))
                                .map(LongPieces::new)
                                .filter(longBlocks -> {
                                    PieceCounter pieceCounter = new PieceCounter(longBlocks.blockStream());
                                    return perfectUsingPieceCounter.containsAll(pieceCounter);
                                })
                                .anyMatch(longBlocks -> BuildUp.existsValidByOrder(initField, memento.getSeparableMinoStream(width).map(SeparableMino::toMinoOperationWithKey), longBlocks.getPieces(), height, reachable));
                    })
                    .map(memento -> {
                        PieceCounter pieceCounter = memento.getSumBlockCounter();
                        return counter.removeAndReturnNew(pieceCounter);
                    })
                    .map(blockCounter -> blockCounter.getBlocks().get(0))
                    .collect(Collectors.toSet());

            System.out.println(parseToString(pieces.getPieces(), holdPieces));

            cnt++;
            if (!holdPieces.isEmpty())
                suc++;
        }

        System.out.println((double) suc / cnt);
        System.out.println(suc);

//
//        // 準備
//        MinoFactory minoFactory = new MinoFactory();
//        MinoShifter minoShifter = new MinoShifter();
//        MinoRotation minoRotation = new MinoRotation();
//        LockedReachable reachable = new LockedReachable(minoFactory, minoShifter, minoRotation, height);
//        BuildUpStream buildUpStream = new BuildUpStream(reachable, height);
//        int maxDepth = 6;
//        ForwardOrderLookUp lookUp = new ForwardOrderLookUp(maxDepth, 7);
//        ColorConverter colorConverter = new ColorConverter();
//
//
//        LoadedPatternGenerator blocksGenerator = new LoadedPatternGenerator("*p7");
//        List<Pieces> allBlocks = MyIterables.toList(piecesGenerator);

    }

    private static String parseToString(List<Piece> pieces, Set<Piece> hold) {
        String blockName = String.format("[%s]%s", pieces.get(0), pieces.subList(1, pieces.size()).stream().map(Piece::getName).collect(Collectors.joining()));

        if (hold.isEmpty()) {
            // 失敗時
            return String.format("-%s,-,-,-,-,-,-,-", blockName);
        } else {
            // 成功時
            String values = ALL_PIECES.stream().map(block -> {
                if (hold.contains(block))
                    return "LAST_OPERATION";
                else if (!pieces.subList(1, pieces.size()).contains(block))
                    return "*";
                return "";
            }).collect(Collectors.joining(","));
            return String.format("%s,%s", blockName, values);
        }
    }
}
