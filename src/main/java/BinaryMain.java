import bin.IndexParser;
import bin.SolutionBinary;
import common.SyntaxException;
import common.pattern.LoadedPatternGenerator;
import core.mino.Piece;
import lib.Stopwatch;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

public class BinaryMain {
    public static void main(String[] args) throws IOException, SyntaxException {
        SolutionBinary solutionBinary = new SolutionBinary(44452800);

        String name = "output/h55.bin";
        DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(name)));

        byte[] bytes = solutionBinary.get();
        dataOutStream.write(bytes, 0, bytes.length);
        dataOutStream.close();


//        ExecutorService executorService = Executors.newFixedThreadPool(4);

        Stopwatch stopwatch = Stopwatch.createStartedStopwatch();
//        executorService.shutdown();
        LoadedPatternGenerator generator = new LoadedPatternGenerator("*,*p5,*p5");

        EnumMap<Piece, Byte> pieceToNumber = new EnumMap<>(Piece.class);
        for (Piece piece : Piece.values()) {
            pieceToNumber.put(piece, (byte) piece.getNumber());
        }

        IndexParser indexParser = new IndexParser(pieceToNumber, Arrays.asList(1, 5, 5));
        long count = generator.blocksStream()
                .mapToLong(pieces -> indexParser.parse(pieces.getPieceArray()))
                .filter(l -> 44452800 < l)
                .count();

        System.out.println(count);
        stopwatch.stop();

        System.out.println(stopwatch.toMessage(TimeUnit.MILLISECONDS));

        if (count == 0) {
            System.out.println("OK");
        } else {
            System.out.println("NG");
        }
    }
}

