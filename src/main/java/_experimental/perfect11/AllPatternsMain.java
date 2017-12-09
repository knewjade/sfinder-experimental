package _experimental.perfect11;

import common.SyntaxException;
import common.datastore.blocks.Pieces;
import common.pattern.LoadedPatternGenerator;
import common.pattern.PatternGenerator;
import core.mino.Piece;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AllPatternsMain {
    public static void main(String[] args) throws IOException, SyntaxException {
        switch (args[0]) {
            case "use11":
                runOnHold11(Integer.valueOf(args[1]));
                break;
            case "use10":
                runOnHold10(Integer.valueOf(args[1]));
                break;
            case "avoid":
                runWithoutHold(Integer.valueOf(args[1]));
                break;
        }
    }

    private static void runOnHold11(int index) throws IOException, SyntaxException {
        // 7種1巡で可能性のあるツモ順
        // 4line on hold
        List<String> patternsOnHold = Arrays.asList(
                "*p7, *p4",
                "*, *p3, *p7",
                "*, *p7, *p3",
                "*, *p4, *p6",
                "*, *, *p7, *p2",
                "*, *p5, *p5",
                "*, *p2, *p7, *",
                "*, *p6, *p4"
        );

        String pattern = patternsOnHold.get(index);
        createOnHold11(pattern, index);
    }

    private static void createOnHold11(String pattern, int index) throws IOException, SyntaxException {
        String path = String.format("output/order%donhold11.csv", index + 1);
        output(pattern, path);
    }

    private static void runOnHold10(int index) throws IOException, SyntaxException {
        // 7種1巡で可能性のあるツモ順
        // 4line on hold
        List<String> patternsOnHold = Arrays.asList(
                "*p7, *p3",
                "*, *p3, *p6",
                "*, *p7, *p2",
                "*, *p4, *p5",
                "*, *, *p7, *",
                "*, *p5, *p4",
                "*, *p2, *p7",
                "*, *p6, *p3"
        );

        String pattern = patternsOnHold.get(index);
        createOnHold10(pattern, index);
    }

    private static void createOnHold10(String pattern, int index) throws IOException, SyntaxException {
        String path = String.format("output/order%donhold10.csv", index + 1);
        output(pattern, path);
    }

    private static void runWithoutHold(int index) throws IOException, SyntaxException {
        // 4line without hold
        List<String> patternsWithoutHold = Arrays.asList(
                "*p7, *p3",
                "*p4, *p6",
                "*, *p7, *p2",
                "*p5, *p5",
                "*p2, *p7, *",
                "*p6, *p4",
                "*p3, *p7"
        );

        String pattern = patternsWithoutHold.get(index);
        createWithoutHold(pattern, index);
    }

    private static void createWithoutHold(String pattern, int index) throws IOException, SyntaxException {
        String path = String.format("output/order%davoid.csv", index + 1);
        output(pattern, path);
    }

    private static void output(String pattern, String path) throws IOException, SyntaxException {
        File outputFile = new File(path);
        PatternGenerator generator = new LoadedPatternGenerator(pattern);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            generator.blocksStream()
                    .map(Pieces::getPieces)
                    .map(blocks -> blocks.stream().map(Piece::getName).collect(Collectors.joining()))
                    .sorted()
                    .forEach(line -> {
                        try {
                            writer.write(line);
                            writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            writer.flush();
        }
    }
}
