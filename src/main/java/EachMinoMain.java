import common.parser.OperationWithKeyInterpreter;
import core.mino.MinoFactory;
import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class EachMinoMain {
    public static void main(String[] args) throws IOException {
        MinoFactory minoFactory = new MinoFactory();

        HashMap<Long, AsyncBufferedFileWriter> fileMap = new HashMap<>();
        for (long count = 3; count <= 7; count++) {
            fileMap.put(count, new MyFile("output/line3_m" + count).newAsyncWriter());
        }

        Files.lines(Paths.get("output/line3_all"))
                .forEach(line -> {
                    long count = OperationWithKeyInterpreter.parseToStream(line, minoFactory).count();
                    AsyncBufferedFileWriter writer = fileMap.get(count);
                    writer.writeAndNewLine(line);
                });

        for (AsyncBufferedFileWriter writer : fileMap.values()) {
            writer.flush();
            writer.close();
        }
    }
}
