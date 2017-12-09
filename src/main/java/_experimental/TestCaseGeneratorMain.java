package _experimental;

import common.datastore.BlockField;
import common.datastore.Coordinate;
import common.datastore.Pair;
import common.tetfu.Tetfu;
import core.field.Field;
import core.mino.Piece;
import helper.EasyTetfu;
import lib.MyFiles;
import lib.Randoms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestCaseGeneratorMain {
    public static void main(String[] args) throws IOException {
        Randoms randoms = new Randoms();
        EasyTetfu easyTetfu = new EasyTetfu();

        int numOfTestCase = 500;
        List<String> lines = IntStream.range(0, numOfTestCase)
                .mapToObj(count -> {
                    int height = randoms.nextInt(3, 9);
                    int maxDepth = selectMaxDepth(randoms, height);

                    boolean isReserved = true;
//                    isReserved = randoms.nextBoolean();

                    boolean containsWildcard = true;
                    containsWildcard = randoms.nextBoolean();

                    boolean isUsingHold = false;
                    isUsingHold = randoms.nextBoolean();

                    boolean isPattern = true;
//                    isPattern = randoms.nextBoolean();

                    Field field = randoms.field(height, maxDepth);
                    String encodeUrl = getEncodeUrl(easyTetfu, height, field, randoms, isReserved);
                    String encode = Tetfu.removeDomainData(encodeUrl);
//            System.out.println(encode);

                    int numOfPiece = containsWildcard ? randoms.nextInt(Math.max(1, maxDepth - 7), maxDepth) : maxDepth;

                    int size = isUsingHold ? numOfPiece + 1 : numOfPiece;

                    String pieces = randoms.blocks(size).stream()
                            .map(Piece::getName)
                            .collect(Collectors.joining(","));
//            System.out.println(pieces);

                    System.out.println(maxDepth);
                    String commands = String.format("path -t %s -c %d", encode, height);

                    int numOfWildcard = maxDepth - numOfPiece;
                    if (0 < numOfWildcard) {
                        String wildcard = "*p" + numOfWildcard;
                        commands += String.format(" -p %s,%s", pieces, wildcard);
                    } else {
                        commands += String.format(" -p %s", pieces);
                    }

                    if (!isUsingHold)
                        commands += " --hold no";

                    if (isPattern)
                        commands += " -f csv -k pattern";

                    if (isReserved)
                        commands += " -r true";

                    return commands;
                })
                .peek(System.out::println)
                .collect(Collectors.toList());

        MyFiles.write("output/testcases", lines);
    }

    private static String getEncodeUrl(EasyTetfu easyTetfu, int height, Field field, Randoms randoms, boolean isReserved) {
        if (isReserved) {
            ArrayList<Coordinate> coordinates = new ArrayList<>();
            for (int y = 0; y < height; y++)
                for (int x = 0; x < 10; x++)
                    if (field.isEmpty(x, y))
                        coordinates.add(new Coordinate(x, y));
            List<Coordinate> sample = randoms.sample(coordinates, 1);
            assert sample.size() == 1;
            Coordinate coordinate = sample.get(0);
            BlockField blockField = new BlockField(height);
            blockField.setBlock(randoms.block(), coordinate.x, coordinate.y);
            return easyTetfu.encodeUrl(field, blockField);
        } else {
            return easyTetfu.encodeUrl(field, Collections.emptyList(), height);
        }
    }

    private static int selectMaxDepth(Randoms randoms, int height) {
        assert 3 <= height;
        switch (height) {
            case 3:
                return randoms.nextInt(5, 7);
            case 4:
                return randoms.nextInt(6, 8);
            default:
                return randoms.nextInt(6, 9);
        }
    }
}
