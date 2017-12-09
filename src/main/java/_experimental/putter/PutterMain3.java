package _experimental.putter;

import common.datastore.PieceCounter;
import helper.EasyPath;
import helper.EasyPool;
import helper.EasyTetfu;
import common.datastore.OperationWithKey;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import searcher.pack.task.Result;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PutterMain3 {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        // ある特定の形に組める組み合わせを列挙
        int width = 3;
        int height = 4;

        EasyPool easyPool = new EasyPool();
        EasyPath easyPath = new EasyPath(easyPool);
        EasyTetfu easyTetfu = new EasyTetfu();

        String goalFieldMarks = "" +
                "XXXXXX____" +
                "XXXXXXX___" +
                "XXXXXXXX__" +
                "XXXXXXX___";
        Field emptyField = FieldFactory.createField(height);
        List<Result> results = easyPath.setUp(goalFieldMarks, emptyField, width, height);

        PieceCounter allBlocks = new PieceCounter(Piece.valueList());
        for (Result result : results) {
            List<OperationWithKey> operationsList = result.getMemento().getOperationsStream(width).collect(Collectors.toList());
            PieceCounter pieceCounter = new PieceCounter(operationsList.stream().map(OperationWithKey::getPiece));
            if (allBlocks.containsAll(pieceCounter)) {
                String encode = easyTetfu.encodeUrl(emptyField, operationsList, height);
                System.out.println(pieceCounter.getBlocks());
                System.out.println(encode);
            }
        }
    }
}
