package mainv2;

import bin.pieces.PieceNumber;

class PieceNumberStep {
    private final PieceNumber[] numbers;
    private final short step;

    PieceNumberStep(PieceNumber[] numbers, short step) {
        this.numbers = numbers;
        this.step = step;
    }

    public PieceNumber[] getNumbers() {
        return numbers;
    }

    public short getStep() {
        return step;
    }
}
