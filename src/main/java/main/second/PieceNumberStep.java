package main.second;

import utils.pieces.PieceNumber;
import utils.step.Steps;

class PieceNumberStep {
    private final PieceNumber[] numbers;
    private final int stepCount;

    PieceNumberStep(PieceNumber[] numbers, short step) {
        this.numbers = numbers;
        this.stepCount = Steps.getStepCount(step);
    }

    public PieceNumber[] getNumbers() {
        return numbers;
    }

    public int getStepCount() {
        return stepCount;
    }
}
