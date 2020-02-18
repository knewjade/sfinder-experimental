package utils.frame;

import java.util.Comparator;

public class FramesComparator implements Comparator<Byte> {
    public boolean shouldUpdate(Byte oldValue, Byte newValue) {
        return 0 < compare(oldValue, newValue);
    }

    @Override
    public int compare(Byte o1, Byte o2) {
        return Frames.compare(o1, o2);
    }
}
