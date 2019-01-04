package line.step3;

import core.field.Field;
import core.mino.MinoFactory;
import line.commons.LineCommons;

import java.util.HashSet;
import java.util.Set;

class SlideCandidate {
    static SlideCandidate create(MinoFactory minoFactory, SlideOperations operations, int maxHeight) {
        Field fieldOnGround = LineCommons.toField(minoFactory, operations.getGroundSlideOperationList(), maxHeight);
        return new SlideCandidate(operations, fieldOnGround, new HashSet<>());
    }

    static SlideCandidate create(MinoFactory minoFactory, SlideOperations operations, Set<Integer> keys, int maxHeight) {
        Field fieldOnGround = LineCommons.toField(minoFactory, operations.getGroundSlideOperationList(), maxHeight);
        return new SlideCandidate(operations, fieldOnGround, keys);
    }

    private final SlideOperations operations;
    private final Field fieldOnGround;
    private final Set<Integer> keys;

    private SlideCandidate(SlideOperations operations, Field fieldOnGround, Set<Integer> keys) {
        this.operations = operations;
        this.fieldOnGround = fieldOnGround;
        this.keys = keys;
    }

    SlideOperations getSlideOperations() {
        return operations;
    }

    Field newFieldOnGround() {
        return fieldOnGround.freeze();
    }

    private Set<Integer> newKeys() {
        return new HashSet<>(keys);
    }

    Set<Integer> getKeys() {
        return keys;
    }

    Set<Integer> nextKey(int key) {
        Set<Integer> keys = newKeys();
        keys.add(key);
        return keys;
    }
}
