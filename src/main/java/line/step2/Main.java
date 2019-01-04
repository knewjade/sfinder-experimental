package line.step2;

import common.datastore.Operation;
import common.datastore.SimpleOperation;
import common.parser.OperationInterpreter;
import line.commons.CountPrinter;
import line.commons.FactoryPool;
import line.commons.Writers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

// ミノの個数ごとにファイルを振り分ける
// ライン消去にTが使われる解だけを抽出する
// すべての行にTがかかっている解だけを抽出する
// TのまわりのブロックをTスピン判定されるようにする
// y=5が揃っている状態
// ミノが空中に浮いている可能性あり
// ミノを追加した結果、消去されるライン数が変わらない解だけを抽出
public class Main {
    public static void main(String[] args) throws IOException {
        {
            String fileName = "1line";
            System.out.println(fileName);
            run(fileName);
        }

        {
            String fileName = "12line";
            System.out.println(fileName);
            run(fileName);
        }
    }

    private static void run(String fileName) throws IOException {
        Writers writers = new Writers("output/" + fileName);

        try {
            run2(fileName, writers);
        } finally {
            writers.close();
        }
    }

    private static void run2(String fileName, Writers writers) throws IOException {
        int maxHeight = 12;
        FactoryPool factoryPool = new FactoryPool(maxHeight);

        Runner runner = new Runner(factoryPool);

        CountPrinter countPrinter = new CountPrinter(10000, Files.lines(Paths.get("output/" + fileName)).count());

        Files.lines(Paths.get("output/" + fileName)).parallel()
                .peek(line -> countPrinter.increaseAndShow())
                .filter(line -> line.contains("T"))
                .map(OperationInterpreter::parseToOperations)
                .flatMap(operations -> {
                    List<Operation> operationList = operations.getOperations().stream()
                            .map(it -> new SimpleOperation(it.getPiece(), it.getRotate(), it.getX(), it.getY() + 2))
                            .collect(Collectors.toList());
                    return runner.search(operationList);
                })
                .forEach(operations -> {
                    // 書き込み
                    int count = operations.getOperations().size();
                    String line = OperationInterpreter.parseToString(operations);
                    writers.writeAndNewLine(count, line);
                });
    }
}
