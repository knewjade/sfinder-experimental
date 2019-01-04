package line.commons;

import core.field.Field;
import core.field.FieldFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AroundBlocks {
    private final Map<Integer, Field> map;

    public AroundBlocks(int maxHeight) {
        Map<Integer, Field> map = new HashMap<>();
        for (int cy = 0; cy < maxHeight; cy++) {
            for (int cx = 0; cx < 10; cx++) {
                Field field = createField(cx, cy, maxHeight);
                map.put(cx + cy * 10, field);
            }
        }
        this.map = map;
    }

    @NotNull
    private Field createField(int cx, int cy, int maxHeight) {
        List<Integer> delta = Arrays.asList(-1, 0, 1);

        Field field = FieldFactory.createField(maxHeight);
        for (int dy : delta) {
            int y = cy + dy;
            if (y < 0 || maxHeight <= y) {
                continue;
            }

            for (int dx : delta) {
                int x = cx + dx;
                if (x < 0 || 10 <= x) {
                    continue;
                }

                field.setBlock(x, y);
            }
        }
        return field;
    }

    public Field get(int x, int y) {
        return map.get(x + y * 10).freeze();
    }
}
