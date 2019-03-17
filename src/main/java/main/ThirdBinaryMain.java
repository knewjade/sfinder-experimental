package main;

import bin.SolutionBinary;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ThirdBinaryMain {
    public static void main(String[] args) throws IOException {
//        String postfix = "SRS";
        String postfix = "SRS7BAG";

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

            run(maxIndexesList, postfix, true);
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

            run(maxIndexesList, postfix, false);
        }
    }

    private static void run(List<List<Integer>> maxIndexesList, String postfix, boolean isFirstHoldEmpty) throws IOException {
        // 実行
        for (List<Integer> maxIndexes : maxIndexesList) {
            System.out.println(maxIndexes);
            SolutionBinary binaryInput = load(maxIndexes, postfix, isFirstHoldEmpty);

            int max = binaryInput.max();
            SolutionBinary binaryOutput = new SolutionBinary(max);

            for (int index = 0; index < max; index++) {
                byte value = binaryInput.at(index);
                if (value != 0) {
                    binaryOutput.put(index, value);
                } else {
                    binaryOutput.put(index, (byte) 0b11111110);  // -2
                }
            }

            save(maxIndexes, isFirstHoldEmpty, binaryOutput);
        }
    }

    private static SolutionBinary load(List<Integer> maxIndexes, String postfix, boolean isFirstHoldEmpty) throws IOException {
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

    private static void save(List<Integer> maxIndexes, boolean isFirstHoldEmpty, SolutionBinary binary) throws IOException {
        // 出力方式とファイル名
        String outputName;
        if (isFirstHoldEmpty) {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            outputName = "resources/third/" + prefix + ".bin";
        } else {
            String prefix = maxIndexes.stream().map(Object::toString).collect(Collectors.joining());
            outputName = "resources/third/h" + prefix + ".bin";
        }

        byte[] bytes = binary.get();
        try (DataOutputStream dataOutStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputName)))) {
            dataOutStream.write(bytes, 0, bytes.length);
        }
    }
}
