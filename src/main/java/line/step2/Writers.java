package line.step2;

import entry.path.output.MyFile;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.util.HashMap;

class Writers {
    private final HashMap<Integer, AsyncBufferedFileWriter> writersMap;
    private final String fileName;

    Writers(String fileName) {
        this.fileName = fileName;
        this.writersMap = new HashMap<>();
    }

    synchronized void writeAndNewLine(int index, String line) {
        if (!writersMap.containsKey(index)) {
            String outputFileName = fileName + "_" + index;
            AsyncBufferedFileWriter writer = create(outputFileName);
            writersMap.put(index, writer);
        }

        AsyncBufferedFileWriter writer = writersMap.get(index);
        writer.writeAndNewLine(line);
    }

    private AsyncBufferedFileWriter create(String outputFileName) {
        try {
            return new MyFile(outputFileName).newAsyncWriter();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    void close() throws IOException {
        for (AsyncBufferedFileWriter writer : writersMap.values()) {
            writer.flush();
            writer.close();
        }
    }
}
