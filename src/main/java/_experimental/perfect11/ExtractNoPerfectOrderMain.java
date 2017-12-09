package _experimental.perfect11;

import common.buildup.BuildUp;
import common.datastore.MinoOperationWithKey;
import common.datastore.blocks.LongPieces;
import common.datastore.blocks.Pieces;
import common.order.OrderLookup;
import common.parser.BlockInterpreter;
import common.parser.OperationWithKeyInterpreter;
import concurrent.LockedReachableThreadLocal;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Piece;
import core.mino.MinoFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

// 10ミノでパフェできない組み合わせのうち、できない順序のものだけを抽出
public class ExtractNoPerfectOrderMain {
    public static void main(String[] args) throws IOException {
        Path includeNGPath = Paths.get("output/combIncludeNG.csv");

        int height = 4;
        Field field = FieldFactory.createField(height);
        LockedReachableThreadLocal reachableThreadLocal = new LockedReachableThreadLocal(height);

        List<LongPieces> notPerfectOrders = Files.lines(includeNGPath, StandardCharsets.UTF_8)
                .peek(System.out::println)
                .flatMap(name -> {
                    Path perfectPath = Paths.get(String.format("output/perfect10each/%s.csv", name));
                    List<List<MinoOperationWithKey>> perfects = loadPerfects(perfectPath);

                    Path orderPath = Paths.get(String.format("output/order10each/%s.csv", name));

                    try {
                        return Files.lines(orderPath, StandardCharsets.UTF_8)
                                .map(BlockInterpreter::parse10)
                                .map(LongPieces::new)
                                .filter(longPieces -> {
                                    List<Piece> pieces = longPieces.getPieces();
                                    return perfects.parallelStream()
                                            .noneMatch(operationWithKeys -> BuildUp.existsValidByOrder(field, operationWithKeys.stream(), pieces, 4, reachableThreadLocal.get()));
                                })
                                .filter(longPieces -> {
                                    List<Piece> pieces = longPieces.getPieces();
                                    return OrderLookup.forwardBlocks(pieces, 10).stream()
                                            .noneMatch(forward -> perfects.parallelStream()
                                                    .anyMatch(operationWithKeys -> BuildUp.existsValidByOrder(field, operationWithKeys.stream(), forward.toList(), 4, reachableThreadLocal.get())));
                                });
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                    throw new IllegalStateException("No reachable");
                })
                .collect(Collectors.toList());

        // output
        File outputFile = new File("output/order10noperfect.csv");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (Pieces pieces : notPerfectOrders) {
                String blocks = pieces.blockStream().map(Piece::getName).collect(Collectors.joining());
                try {
                    writer.write(blocks);
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            writer.flush();
        }
    }

    private static List<List<MinoOperationWithKey>> loadPerfects(Path perfectPath) {
        MinoFactory minoFactory = new MinoFactory();
        try {
            return Files.lines(perfectPath, Charset.forName("UTF-8"))
                    .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        throw new IllegalStateException("No reachable");
    }
}
