package mainv2;

import bin.pieces.PieceNumber;

class PieceNumberStep {
    private final PieceNumber[] numbers;
    private final short step;
    private final int solutionIndex;

    PieceNumberStep(PieceNumber[] numbers, short step, int solutionIndex) {
        this.numbers = numbers;
        this.step = step;
        this.solutionIndex = solutionIndex;
    }

    public PieceNumber[] getNumbers() {
        return numbers;
    }

    public short getStep() {
        return step;
    }

    public int getSolutionIndex() {
        return solutionIndex;
    }
}
