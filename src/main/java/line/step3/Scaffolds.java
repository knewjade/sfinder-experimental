package line.step3;

import common.datastore.Operation;
import common.datastore.Pair;
import core.field.Field;
import core.neighbor.OriginalPiece;
import line.commons.FactoryPool;
import line.commons.KeyOriginalPiece;
import line.commons.LineCommons;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class Scaffolds {
    static Scaffolds create(FactoryPool factoryPool) {
        List<KeyOriginalPiece> allOriginalPieces = createKeyOriginalPieces(factoryPool);

        Map<Integer, List<KeyOriginalPiece>> map = allOriginalPieces.stream()
                .map(keyOriginalPiece -> {
                    OriginalPiece originalPiece = keyOriginalPiece.getOriginalPiece();
                    Field minoField = originalPiece.getMinoField();

                    // 足場になるミノだけを抽出
                    List<KeyOriginalPiece> scaffolds = allOriginalPieces.stream()
                            .filter(scaffold -> minoField.canPut(scaffold.getOriginalPiece()))
                            .filter(scaffold -> scaffold.getOriginalPiece().getMinoField().isOnGround(
                                    originalPiece.getMino(), originalPiece.getX(), originalPiece.getY()
                            ))
                            .collect(Collectors.toList());

                    int key = LineCommons.toKey(originalPiece.getPiece(), originalPiece.getRotate(), originalPiece.getX(), originalPiece.getY());
                    return new Pair<>(key, new ArrayList<>(scaffolds));
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        return new Scaffolds(map);
    }

    private static List<KeyOriginalPiece> createKeyOriginalPieces(FactoryPool factoryPool) {
        AtomicInteger keyIndexCounter = new AtomicInteger();
        return factoryPool.createUniqueOriginalPieces().stream().sequential()
                .map(originalPiece -> {
                    int index = keyIndexCounter.getAndIncrement();
                    return new KeyOriginalPiece(originalPiece, index);
                })
                .collect(Collectors.toList());
    }

    private final Map<Integer, List<KeyOriginalPiece>> scaffolds;

    private Scaffolds(Map<Integer, List<KeyOriginalPiece>> scaffolds) {
        this.scaffolds = scaffolds;
    }

    public List<KeyOriginalPiece> get(Operation operation) {
        int key = LineCommons.toKey(operation.getPiece(), operation.getRotate(), operation.getX(), operation.getY());
        return this.scaffolds.get(key);
    }
}
