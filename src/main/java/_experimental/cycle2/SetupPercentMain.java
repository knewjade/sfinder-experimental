package _experimental.cycle2;

import common.SyntaxException;
import common.datastore.PieceCounter;
import common.datastore.blocks.LongPieces;
import common.pattern.LoadedPatternGenerator;
import helper.EasyPath;
import helper.EasyPool;
import common.order.ForwardOrderLookUp;
import common.tree.AnalyzeTree;
import core.field.Field;
import core.field.FieldFactory;
import core.field.FieldView;
import core.mino.Piece;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SetupPercentMain {
    public static void main(String[] args) throws ExecutionException, InterruptedException, SyntaxException {
        int width = 3;
        int height = 4;

        EasyPool easyPool = new EasyPool();
        EasyPath easyPath = new EasyPath(easyPool);
        Field emptyField = FieldFactory.createField(height);

        HashSet<LongPieces> allBlocksSet = new HashSet<>();
        PieceCounter allPieceCounter = new PieceCounter(Stream.of(Piece.I, Piece.O, Piece.S, Piece.Z, Piece.L, Piece.J));

        String marksRight = "" +
                "XXXXXX____" +
                "XXXXXX____" +
                "XXXXXX____" +
                "XXXXXX____";
        Field rightField = FieldFactory.createField(marksRight);

        for (int x = 0; x <= 6; x++) {
            String slidedField = FieldView.toString(rightField, 4, "");
            System.out.println(FieldView.toString(FieldFactory.createField(slidedField)));

            Set<LongPieces> results = easyPath.buildUp(slidedField, emptyField, width, height).stream()
                    .filter(longBlocks -> new PieceCounter(longBlocks.blockStream()).equals(allPieceCounter))
                    .collect(Collectors.toSet());
            System.out.println(results.size());

            allBlocksSet.addAll(results);

            rightField.slideLeft(1);
            for (int y = 0; y < height; y++) {
                rightField.setBlock(9, y);
            }
        }

        AnalyzeTree tree = new AnalyzeTree();
        ForwardOrderLookUp lookUp = new ForwardOrderLookUp(7, 7);
        new LoadedPatternGenerator("*!").blocksStream()
                .forEach(blocks -> {
                    boolean canBuildUp = lookUp.parse(blocks.getPieces())
                            .map(LongPieces::new)
                            .filter(longBlocks -> longBlocks.getLastBlock() == Piece.T)
                            .anyMatch(longBlocks -> allBlocksSet.contains(new LongPieces(longBlocks.blockStream().limit(6))));
                    tree.set(canBuildUp, blocks);
                    System.out.println(blocks.blockStream().map(Piece::getName).collect(Collectors.joining()) + "," + (canBuildUp ? "LAST_OPERATION" : "X"));
                });
//        System.out.println(tree.show());
//        System.out.println(tree.tree(2));
    }
}
