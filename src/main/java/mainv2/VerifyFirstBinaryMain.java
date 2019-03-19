package mainv2;

import bin.MovementComparator;
import bin.Movements;
import bin.SolutionBinary;
import bin.SolutionShortBinary;
import main.BinaryLoader;

import java.io.IOException;

public class VerifyFirstBinaryMain {
    public static void main(String[] args) throws IOException {
//        String postfix = "SRS";
        String postfix = "SRS7BAG";

        // 9ミノのテトリスパフェ結果を読み込み
        SolutionBinary byteBinary = BinaryLoader.load("resources/9pieces_" + postfix + ".bin");
        SolutionShortBinary shortBinary = BinaryLoader.loadShortBinary("resources/9pieces_" + postfix + "_mov.bin");

        if (byteBinary.max() != shortBinary.max()){
            throw new IllegalStateException("Not verified max");
        }

        for (int index = 0, max = byteBinary.max(); index < max; index++) {
            byte at = byteBinary.at(index);
            short at1 = shortBinary.at(index);
            if ((at == 0L) == (Movements.isPossible(at1))) {
                throw new IllegalStateException("Not verified value: index=" + index);
            }
        }

        short min = Movements.impossible();
        int minIndex = -1;
        MovementComparator comparator = new MovementComparator();
        for (int index = 0, max = byteBinary.max(); index < max; index++) {
            short at = shortBinary.at(index);
            if (comparator.shouldUpdate(min, at)) {
                min = at;
                minIndex = index;
            }
        }

        System.out.println(min);
        System.out.println(minIndex);
    }
}
