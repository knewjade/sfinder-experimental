package main.to_index;

import common.datastore.MinimalOperationWithKey;
import common.datastore.MinoOperationWithKey;
import common.datastore.Pair;
import common.parser.OperationWithKeyInterpreter;
import common.parser.StringEnumTransform;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.Piece;
import core.srs.Rotate;
import entry.path.output.MyFile;
import helper.KeyParser;
import lib.AsyncBufferedFileWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class FromIndexFor4LineMain {
    public static void main(String[] args) throws IOException {
        MinoFactory minoFactory = new MinoFactory();
        Map<MinoOperationWithKey, Integer> index = Files.lines(Paths.get("output/index.csv"))
                .map(line -> {
                    String[] split = line.split(",");
                    int indexNumber = Integer.valueOf(split[0]);
                    Piece piece = StringEnumTransform.toPiece(split[1]);
                    Rotate rotate = StringEnumTransform.toRotate(split[2]);
                    int x = Integer.valueOf(split[3]);
                    int lowerY = Integer.valueOf(split[4]);
//                    long usingKey = KeyParser.parseToLong(split[5]);
                    long deleteKey = KeyParser.parseToLong(split[6]);
                    Mino mino = minoFactory.create(piece, rotate);
                    return new Pair<>(indexNumber, new MinimalOperationWithKey(mino, x, lowerY - mino.getMinY(), deleteKey));
                })
                .collect(Collectors.toMap(Pair::getValue, Pair::getKey));

        MyFile outputFile = new MyFile("output/indexed_solutions_10x4_SRS.csv");
        try (AsyncBufferedFileWriter writer = outputFile.newAsyncWriter()) {
            Files.lines(Paths.get("input/result_10x4.csv")).parallel()
                    .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                    .map(minoOperationWithKeys -> {
                        return minoOperationWithKeys.stream()
                                .map(operationWithKey -> {
                                    Integer integer = index.get(operationWithKey);
                                    assert integer != null : operationWithKey;

//                                    // indexで逆引き
//                                    System.out.print(integer + " " + operationWithKey + " -> ");
//                                    for (Map.Entry<MinoOperationWithKey, Integer> entry : index.entrySet()) {
//                                        if (Objects.equals(entry.getValue(), integer))
//                                            System.out.println(entry.getKey());
//                                    }

                                    return integer;
                                })
                                .sorted()
                                .map(Object::toString)
                                .collect(Collectors.joining(","));
                    })
                    .forEach(writer::writeAndNewLine);
        }
    }
}
