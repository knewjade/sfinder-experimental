package main.to_index;

import common.comparator.OperationWithKeyComparator;
import common.datastore.MinoOperationWithKey;
import common.datastore.OperationWithKey;
import common.parser.StringEnumTransform;
import core.field.KeyOperators;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import helper.KeyParser;
import searcher.pack.separable_mino.AllMinoFactory;
import searcher.pack.separable_mino.SeparableMino;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ToIndexFor4LineMain {
    private static final int HEIGHT = 4;

    public static void main(String[] args) throws IOException {
        // すべてのミノを作成
        long mask = KeyOperators.getMaskForKeyBelowY(4);
        MinoFactory minoFactory = new MinoFactory();
        MinoShifter minoShifter = new MinoShifter();
        AllMinoFactory factory = new AllMinoFactory(minoFactory, minoShifter, 10, 4, mask);
        List<SeparableMino> separableMinos = new ArrayList<>(factory.create());

        // ソート
        OperationWithKeyComparator<OperationWithKey> comparator = new OperationWithKeyComparator<>();
        separableMinos.sort((o1, o2) -> {
            MinoOperationWithKey operationWithKey1 = o1.toMinoOperationWithKey();
            MinoOperationWithKey operationWithKey2 = o2.toMinoOperationWithKey();
            return comparator.compare(operationWithKey1, operationWithKey2);
        });

        Charset charset = StandardCharsets.UTF_8;
        try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output/index.csv", false), charset))) {
            for (int index = 0; index < separableMinos.size(); index++) {
                SeparableMino mino = separableMinos.get(index);
                MinoOperationWithKey operationWithKey = mino.toMinoOperationWithKey();
                String format = String.format("%d,%s,%s,%d,%d,%s,%s",
                        index,
                        operationWithKey.getPiece().getName(),
                        StringEnumTransform.toString(operationWithKey.getRotate()),
                        operationWithKey.getX(),
                        mino.getLowerY(),
                        KeyParser.parseToString(operationWithKey.getUsingKey(), HEIGHT),
                        KeyParser.parseToString(operationWithKey.getNeedDeletedKey(), HEIGHT)
                );
                System.out.println(format);
                bufferedWriter.write(format);
                bufferedWriter.newLine();
            }
        }
    }
}
