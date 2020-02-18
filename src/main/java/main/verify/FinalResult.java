package main.verify;

class FinalResult {
    private final byte frame;
    private final String fumen;

    FinalResult(Order order, byte frame, String fumen) {
        this.frame = frame;
        this.fumen = fumen;
    }

    byte getFrame() {
        return frame;
    }

    String getFumen() {
        return fumen;
    }
}