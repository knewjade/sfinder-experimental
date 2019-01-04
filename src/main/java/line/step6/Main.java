package line.step6;

import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

// step5の結果をひとつにまとめる
public class Main {
    public static void main(String[] args) throws IOException {
        List<String> fileNames = Arrays.asList(
                "v2_4_last",
                "v2_5_last",
                "v2_6_last",
                "v2_7_last"
        );

        MyFile outputFile = new MyFile("output/last");
        try (AsyncBufferedFileWriter writer = outputFile.newAsyncWriter()) {
            for (String fileName : fileNames) {
                Files.lines(Paths.get("output/" + fileName))
                        .forEach(writer::writeAndNewLine);
            }
        }
    }
}
