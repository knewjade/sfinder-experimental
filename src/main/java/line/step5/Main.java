package line.step5;

import common.parser.OperationInterpreter;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;
import line.commons.CountPrinter;
import line.commons.FactoryPool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

// Tを回転入れするために必要なミノを探索する
// Tミノの周辺 (3x3の四角) にはミノを置かない
// ミノを追加した結果、消去されるライン数が変わらない解だけを抽出
public class Main {
    public static void main(String[] args) throws IOException {
        {
            String fileName = "v2";
            int index = 4;
            run(fileName, index);
        }

        {
            String fileName = "v2";
            int index = 5;
            run(fileName, index);
        }

        {
            String fileName = "v2";
            int index = 6;
            run(fileName, index);
        }

        {
            String fileName = "v2";
            int index = 7;
            run(fileName, index);
        }
    }

    private static void run(String fileName, int index) throws IOException {
        int maxHeight = 24;
        FactoryPool factoryPool = new FactoryPool(maxHeight);
        Runner runner = new Runner(factoryPool);

        String file = "output/" + fileName + "_" + index;
        CountPrinter countPrinter = new CountPrinter(10000, Files.lines(Paths.get(file)).count());

        MyFile outputFile = new MyFile("output/" + fileName + "_" + index + "_last");
        try (AsyncBufferedFileWriter writer = outputFile.newAsyncWriter()) {
            Files.lines(Paths.get(file))
                    .peek(line -> countPrinter.increaseAndShow())
                    .map(OperationInterpreter::parseToOperations)
                    .flatMap(runner::run)
                    .forEach(operations -> {
                        String line = OperationInterpreter.parseToString(operations);
                        writer.writeAndNewLine(line);
                    });
        }
    }
}
