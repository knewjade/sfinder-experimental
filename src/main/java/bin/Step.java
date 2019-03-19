package bin;

import core.srs.Rotate;

public class Step {
    private final int movement;
    private final int rotateCount;
    private final Rotate goalRotate;
    private final int goalX;
    private final int goalY;

    Step(int movement, int rotateCount, Rotate goalRotate, int goalX, int goalY) {
        this.movement = movement;
        this.rotateCount = rotateCount;
        this.goalRotate = goalRotate;
        this.goalX = goalX;
        this.goalY = goalY;
    }

    public int movement() {
        return movement;
    }

    public int rotateCount() {
        return rotateCount;
    }

    int sum() {
        return movement + rotateCount;
    }

    Rotate getGoalRotate() {
        return goalRotate;
    }

    int getGoalX() {
        return goalX;
    }

    int getGoalY() {
        return goalY;
    }

    @Override
    public String toString() {
        return "Step{" +
                "movement=" + movement +
                ", rotateCount=" + rotateCount +
                ", goalRotate=" + goalRotate +
                ", goalX=" + goalX +
                '}';
    }
}
