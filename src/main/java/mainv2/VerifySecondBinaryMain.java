package mainv2;

import bin.Movements;
import bin.SolutionBinary;
import bin.SolutionShortBinary;
import main.BinaryLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VerifySecondBinaryMain {
    public static void main(String[] args) throws IOException {
        {
            // ホールドが空のとき
            List<List<Integer>> maxIndexesList = Arrays.asList(
                    Arrays.asList(7, 4),
                    Arrays.asList(6, 5),
                    Arrays.asList(5, 6),
                    Arrays.asList(4, 7),
                    Arrays.asList(3, 7, 1),
                    Arrays.asList(2, 7, 2),
                    Arrays.asList(1, 7, 3)
            );

            run(maxIndexesList, true);
        }

        {
            // すでにホールドがあるとき
            List<List<Integer>> maxIndexesList = Arrays.asList(
                    Arrays.asList(1, 7, 3),
                    Arrays.asList(1, 6, 4),
                    Arrays.asList(1, 5, 5),
                    Arrays.asList(1, 4, 6),
                    Arrays.asList(1, 3, 7),
                    Arrays.asList(1, 2, 7, 1),
                    Arrays.asList(1, 1, 7, 2)
            );

            run(maxIndexesList, false);
        }
    }

    private static void run(List<List<Integer>> maxIndexesList, boolean isFirstHoldEmpty) throws IOException {
        // 実行
        for (List<Integer> maxIndexes : maxIndexesList) {
            System.out.println(maxIndexes);
            SolutionShortBinary binarySRS = loadShortBinary(maxIndexes, "SRS", isFirstHoldEmpty);
            SolutionShortBinary binarySR7BAG = loadShortBinary(maxIndexes, "SRS7BAG", isFirstHoldEmpty);
            SolutionBinary prevBinary = loadByteBinary(maxIndexes, isFirstHoldEmpty);

            short[] first = binarySRS.get();
            short[] second = binarySR7BAG.get();
            byte[] prev = prevBinary.get();

            // SRSとSRS7BAGの結果が一致していること
            if (!Arrays.equals(first, second)) {
                throw new RuntimeException();
            }

            // 配列の要素数は、前回の結果の8倍であること
            if (first.length != prev.length * 8) {
                throw new RuntimeException();
            }

            // 前回の結果と一致していること
            for (int index = 0, max = prev.length; index < max; index++) {
                byte prevSolution = prev[index];

                for (int offset = 0; offset < 8; offset++) {
                    short currentSolution = first[index * 8 + offset];

                    if ((Byte.toUnsignedInt(prevSolution) & (1 << (7 - offset))) != 0) {
                        // パフェできる
                        if (!Movements.isPossible(currentSolution)) {
                            throw new RuntimeException();
                        }
                    } else {
                        // パフェできない
                        if (Movements.isPossible(currentSolution)) {
                            throw new RuntimeException(prevSolution + " " + currentSolution);
                        }
                    }
                }
            }

            // 解がある個数を表示
            int solutionCounter = 0;
            for (short value : first) {
                if (value != 0) {
                    solutionCounter += 1;
                }
            }

            System.out.printf("%d (%.3f%% = %d/%d)%n", solutionCounter, 100.0 * solutionCounter / first.length, solutionCounter, first.length);
        }
    }

    private static SolutionBinary loadByteBinary(List<Integer> maxIndexes, boolean isFirstHoldEmpty) throws IOException {
        // 出力方式とファイル名
        String inputName;
        if (isFirstHoldEmpty) {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/bin/" + prefix + ".bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/bin/h" + prefix + ".bin";
        }

        return BinaryLoader.load(inputName);
    }

    private static SolutionShortBinary loadShortBinary(List<Integer> maxIndexes, String postfix, boolean isFirstHoldEmpty) throws IOException {
        // 出力方式とファイル名
        String inputName;
        if (isFirstHoldEmpty) {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/mov/" + postfix + "/" + prefix + "_mov.bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/mov/" + postfix + "/h" + prefix + "_mov.bin";
        }

        return BinaryLoader.loadShortBinary(inputName);
    }
}
