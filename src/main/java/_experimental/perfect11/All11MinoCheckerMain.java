package _experimental.perfect11;

import common.datastore.PieceCounter;
import common.datastore.blocks.LongPieces;
import common.order.ForwardOrderLookUp;
import common.parser.BlockInterpreter;
import core.mino.Piece;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class All11MinoCheckerMain {
    public static void main(String[] args) throws IOException {
        System.out.println("start");

        // パフェできない組みあわせ
        Path allNGOrders = Paths.get("output/combAllNG.csv");
        HashSet<PieceCounter> pieceCounters = Files.lines(allNGOrders)
                .map(BlockInterpreter::parse10)
                .map(PieceCounter::new)
                .collect(Collectors.toCollection(HashSet::new));

        // パフェできない順序
        Path includeNGOrder = Paths.get("output/orderNG.csv");
        Set<LongPieces> ngPieces = Files.lines(includeNGOrder)
                .map(BlockInterpreter::parse10)
                .map(LongPieces::new)
                .collect(Collectors.toSet());

        //
        Path all11onhold = Paths.get("output/all11onhold.csv");
        AtomicInteger counter = new AtomicInteger();
        List<String> failed = Files.lines(all11onhold)
                .parallel()
                .peek(s -> {
                    int i = counter.incrementAndGet();
                    if (i % 10000000 == 0)
                        System.out.println(i);
                })
                .filter(line -> {
                    // パフェできないものは true で次に送る

                    // パフェできない組み合わせである
                    PieceCounter pieceCounter = new PieceCounter(BlockInterpreter.parse10(line));
                    if (pieceCounters.contains(pieceCounter))
                        return true;

                    // パフェできない順序である
                    LongPieces pieces = new LongPieces(BlockInterpreter.parse10(line));
                    return ngPieces.contains(pieces);
                })
                .filter(line -> {
                    List<Piece> blocks = BlockInterpreter.parse11(line).collect(Collectors.toList());
                    ForwardOrderLookUp orderLookUp = new ForwardOrderLookUp(10, true);

                    // すべてのパターンでパフェできないものは true で次に送る
                    return orderLookUp.parse(blocks)
                            .map(LongPieces::new)
                            .allMatch(pieces -> {
                                // パフェできないものは true で次に送る

                                // パフェできない順序である
                                if (ngPieces.contains(pieces))
                                    return true;

                                // パフェできない組み合わせである
                                PieceCounter pieceCounter = new PieceCounter(pieces.blockStream());
                                return pieceCounters.contains(pieceCounter);
                            });

                })
                .peek(System.out::println)
                .collect(Collectors.toList());

        System.out.println("check count: " + counter);

        System.out.println("failed: " + failed.size());
        failed.forEach(System.out::println);

        System.out.println("end");
    }
}
