package utils;

import common.datastore.action.Action;
import core.field.Field;
import core.field.FieldFactory;
import core.mino.Mino;
import core.mino.MinoFactory;
import core.mino.MinoShifter;
import core.mino.Piece;
import core.srs.MinoRotation;
import core.srs.Rotate;
import core.srs.RotateDirection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// 1ミノおくときに必要なStepを計算
public class Movement {
    private final MinoFactory minoFactory;
    private final MinoRotation minoRotation;
    private final MinoShifter minoShifter;
    private final HashMap<Integer, Step> maps;

    public Movement(MinoFactory minoFactory, MinoRotation minoRotation, MinoShifter minoShifter) {
        this.minoFactory = minoFactory;
        this.minoRotation = minoRotation;
        this.minoShifter = minoShifter;
        this.maps = create();
    }

    private HashMap<Integer, Step> create() {
        HashMap<Integer, Step> map = new HashMap<>();

        for (Piece piece : Piece.values()) {
            for (Rotate rotate : minoShifter.getUniqueRotates(piece)) {
                Mino mino = minoFactory.create(piece, rotate);

                for (int x = -mino.getMinX(); x < 10 - mino.getMaxX(); x++) {
                    Step step = getStep(mino, x);
                    int index = getIndex(piece, rotate, x);
                    map.put(index, step);
                }
            }
        }

        return map;
    }

    // 同じミノの置き方になる複数の回転方向で最も小さいステップ数を取得
    private Step getStep(Mino mino, int x) {
        Piece piece = mino.getPiece();
        Rotate rotate = mino.getRotate();

        Field field = FieldFactory.createField(8);

        int y = field.getYOnHarddrop(mino, x, 4);
        Field freeze = field.freeze();
        freeze.put(mino, x, y);

        List<Action> actions = minoShifter.enumerateSameOtherActions(piece, rotate, x, y);

        List<Step> steps = new ArrayList<>();
        steps.add(get(field, piece, rotate, x, y));
        for (Action act : actions) {
            steps.add(get(field, piece, act.getRotate(), act.getX(), act.getY()));
        }
        steps.sort(Comparator.comparingInt(Step::sum));

        assert steps.size() == 1 || steps.get(0).sum() < steps.get(1).sum();

        return steps.get(0);
    }

    private Step get(Field field, Piece piece, Rotate goalRotate, int goalX, int goalY) {
        int x = 4;
        int y = 4;
        int rotateCount = 0;

        Rotate currentRotate = Rotate.Spawn;
        Mino mino = minoFactory.create(piece, currentRotate);
        switch (goalRotate) {
            case Left: {
                RotateDirection direction = RotateDirection.Left;
                Mino nextMino = minoFactory.create(piece, currentRotate.get(direction));
                int[] kicks = minoRotation.getKicks(field, mino, nextMino, x, y, direction);
                x += kicks[0];
                y += kicks[1];
                rotateCount += 1;
                break;
            }
            case Right: {
                RotateDirection direction = RotateDirection.Right;
                Mino nextMino = minoFactory.create(piece, currentRotate.get(direction));
                int[] kicks = minoRotation.getKicks(field, mino, nextMino, x, y, direction);
                x += kicks[0];
                y += kicks[1];
                rotateCount += 1;
                break;
            }
            case Reverse: {
                RotateDirection direction = RotateDirection.Right;

                Mino nextMino1 = minoFactory.create(piece, currentRotate.get(direction));
                int[] kicks1 = minoRotation.getKicks(field, mino, nextMino1, x, y, direction);
                x += kicks1[0];
                y += kicks1[1];

                Mino nextMino2 = minoFactory.create(piece, nextMino1.getRotate().get(direction));
                int[] kicks2 = minoRotation.getKicks(field, nextMino1, nextMino2, x, y, direction);
                x += kicks2[0];
                y += kicks2[1];

                rotateCount += 2;
                break;
            }
        }
        int movement = Math.abs(goalX - x);
        return new Step(movement, rotateCount, goalRotate, goalX, goalY);
    }

    public Step harddrop(Piece piece, Rotate rotate, int x) {
        int index = getIndex(piece, rotate, x);
        assert maps.containsKey(index) : index;
        return maps.get(index);
    }

    private int getIndex(Piece piece, Rotate rotate, int x) {
        return piece.getNumber() * 10 * 4 +
                rotate.getNumber() * 10 +
                x;
    }
}
