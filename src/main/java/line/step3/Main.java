package line.step3;

import common.datastore.Operations;
import common.parser.OperationInterpreter;
import core.mino.MinoFactory;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import line.commons.CountPrinter;
import line.commons.FactoryPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// すべてのミノが空中に浮かないようにする
public class Main {
    public static void main(String[] args) throws IOException {
        {
            String fileName = "1line";
            int index = 7;

            System.out.println(fileName + " " + index);

//            run(fileName + "_" + index);
        }

        {
            String fileName = "1line";
            int index = 6;

            System.out.println(fileName + " " + index);

//            run(fileName + "_" + index);
        }

        {
            String fileName = "1line";
            int index = 5;

            System.out.println(fileName + " " + index);

            run(fileName + "_" + index);
        }

        {
            String fileName = "1line";
            int index = 4;

            System.out.println(fileName + " " + index);

//            run(fileName + "_" + index);
        }

        {
            String fileName = "12line";
            int index = 7;

            System.out.println(fileName + " " + index);

//            run(fileName + "_" + index);
        }

        {
            String fileName = "12line";
            int index = 6;

            System.out.println(fileName + " " + index);

//            run(fileName + "_" + index);
        }
    }

    private static void run(String fileName) throws IOException {
        int maxHeight = 24;
        FactoryPool factoryPool = new FactoryPool(maxHeight);
        MinoFactory minoFactory = factoryPool.getMinoFactory();

        Runner runner = new Runner(factoryPool);

        CountPrinter countPrinter = new CountPrinter(10000, Files.lines(Paths.get("output/" + fileName)).count());

        try (AsyncBufferedFileWriter writer = new MyFile("output/" + fileName + "_next").newAsyncWriter()) {
            Files.lines(Paths.get("output/" + fileName))
                    .peek(line -> countPrinter.increaseAndShow())
                    .map(OperationInterpreter::parseToOperations)
                    .map(operations -> new EmptySlideOperations(minoFactory, operations.getOperations()))
                    .flatMap(runner::run)
                    .forEach(slideOperations -> {
                        Operations operations = new Operations(slideOperations.getGroundSlideOperationList());
                        String line = OperationInterpreter.parseToString(operations);
                        writer.writeAndNewLine(line);
                    });
        }
    }
}
