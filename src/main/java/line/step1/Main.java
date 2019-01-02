package line.step1;

import common.datastore.Operations;
import common.datastore.PieceCounter;
import common.parser.OperationInterpreter;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import line.commons.CountPrinter;

import java.io.IOException;

// 1ラインを埋めるミノの組み合わせを列挙
// y=3が揃っている状態
// ミノが空中に浮いている可能性あり
public class Main {
    public static void main(String[] args) {
        {
            int maxHeight = 7;
            Field lineField = FieldFactory.createField("" +
                    "__________" +
                    "__________" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "__________" +
                    "__________"
            );
            String fileName = "1line";

            System.out.println(fileName);
            run(maxHeight, lineField, fileName);
        }

        {
            int maxHeight = 8;
            Field lineField = FieldFactory.createField("" +
                    "__________" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "__________" +
                    "__________"
            );
            String fileName = "12line";

            System.out.println(fileName);
            run(maxHeight, lineField, fileName);
        }

        {
            int maxHeight = 9;
            Field lineField = FieldFactory.createField("" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "XXXXXXXXXX" +
                    "__________" +
                    "__________" +
                    "__________"
            );
            String fileName = "13line";

//            System.out.println(fileName);
//            run(maxHeight, lineField, fileName);
        }
    }

    private static void run(int maxHeight, Field lineField, String fileName) {
        int lineY = 3;

        FactoryPool factoryPool = new FactoryPool(maxHeight, lineY);

        PieceCounter allPieceCounter = new PieceCounter(Piece.valueList());

        Field initField = FieldFactory.createField(maxHeight);

        Runner runner = new Runner(factoryPool);
        Result result = new EmptyResult(lineField, initField, allPieceCounter, lineY);

        CountPrinter countPrinter = new CountPrinter(100000);

        MyFile file = new MyFile("output/" + fileName);
        try (AsyncBufferedFileWriter writer = file.newAsyncWriter()) {
            runner.run(result, lineObj -> {
                countPrinter.increaseAndShow();

                Operations operations = new Operations(lineObj.pieceStream());
                String str = OperationInterpreter.parseToString(operations);
                writer.writeAndNewLine(str);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
