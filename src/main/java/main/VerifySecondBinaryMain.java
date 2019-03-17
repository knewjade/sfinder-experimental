package main;

import bin.SolutionBinary;

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
            SolutionBinary binarySRS = load(maxIndexes, "SRS", isFirstHoldEmpty);
            SolutionBinary binarySR7BAG = load(maxIndexes, "SRS7BAG", isFirstHoldEmpty);

            byte[] first = binarySRS.get();
            byte[] second = binarySR7BAG.get();

            // SRSとSRS7BAGの結果が一致していること
            if (!Arrays.equals(first, second)) {
                System.out.println("NG");
            }

            // 解がある個数を表示
            int solutionCounter = 0;
            for (byte value : first) {
                if (value != 0) {
                    solutionCounter += 1;
                }
            }

            System.out.printf("%d (%.3f%% = %d/%d)%n", solutionCounter, 100.0 * solutionCounter / first.length, solutionCounter, first.length);
        }
    }

    private static SolutionBinary load(List<Integer> maxIndexes, String postfix, boolean isFirstHoldEmpty) throws IOException {
        // 出力方式とファイル名
        String inputName;
        if (isFirstHoldEmpty) {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/second/" + prefix + "_" + postfix + ".bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            inputName = "resources/second/h" + prefix + "_" + postfix + ".bin";
        }

        return BinaryLoader.load(inputName);
    }
}
