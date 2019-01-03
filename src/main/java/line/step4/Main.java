package line.step4;

import common.datastore.Operations;
import common.parser.OperationInterpreter;
import line.commons.Writers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

// step3の結果をミノ数ごとにまとめる
public class Main {
    public static void main(String[] args) throws IOException {
        List<String> fileNames = Arrays.asList(
                "1line_4_next",
                "1line_5_next",
                "1line_6_next",
                "1line_7_next",
                "12line_6_next",
                "12line_7_next"
        );

        Writers writers = new Writers("output/v2");

        try {
            for (String fileName : fileNames) {
                Files.lines(Paths.get("output/" + fileName))
                        .forEach(line -> {
                            Operations operations = OperationInterpreter.parseToOperations(line);
                            int size = operations.getOperations().size();
                            writers.writeAndNewLine(size, line);
                        });
            }
        } finally {
            writers.close();
        }
    }
}
