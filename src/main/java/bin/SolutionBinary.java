package bin;

import common.datastore.blocks.LongPieces;
import common.datastore.blocks.Pieces;

public class SolutionBinary {
    private final byte[] bytes;
    private final int max;

    public SolutionBinary(int max) {
        this.bytes = new byte[max];
        this.max = max;
    }

    public void put(Pieces pieces, byte value) {
        LongPieces longPieces = new LongPieces(pieces);
        int index = (int) longPieces.getLong();
        assert 0 <= index && index < max;
        bytes[index] = value;
    }

    public byte[] get() {
        return bytes;
    }
}
