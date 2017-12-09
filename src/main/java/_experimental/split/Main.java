package _experimental.split;

import common.datastore.MinoOperationWithKey;
import common.parser.OperationWithKeyInterpreter;
import core.mino.MinoFactory;
import helper.EasyTetfu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;

public class Main {
    public static void main(String[] args) throws IOException {
        EasyTetfu easyTetfu = new EasyTetfu();
        MinoFactory minoFactory = new MinoFactory();
        long count = Files.lines(Paths.get("output/SRS7Bag/result_10x4.csv"))
                .map(line -> OperationWithKeyInterpreter.parseToList(line, minoFactory))
                .filter(operationWithKeys -> {
                    // 左端のミノから順に探索する
                    operationWithKeys.sort(Comparator.comparingInt(o -> o.getX() + o.getMino().getMinX()));

                    MinoOperationWithKey first = operationWithKeys.get(0);
                    int maxX = first.getX() + first.getMino().getMaxX();
                    for (MinoOperationWithKey operationWithKey : operationWithKeys.subList(1, operationWithKeys.size())) {
                        int min = operationWithKey.getX() + operationWithKey.getMino().getMinX();
                        if (maxX < min)
                            return false;  // これまでの塊と独立して始まる  // 分割可能

                        int max = operationWithKey.getX() + operationWithKey.getMino().getMaxX();
                        if (maxX < max)
                            maxX = max;  // 右端を更新
                    }

                    return true;
                })
//                .peek(operationWithKeys -> {
//                    String encode = easyTetfu.encodeUrl(FieldFactory.createField(4), operationWithKeys, 4);
//                    System.out.println(encode);
//                })
                .count();
        System.out.println(count);
//                .collect(Collectors.toList());
    }
}
