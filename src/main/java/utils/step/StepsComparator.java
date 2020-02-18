package utils.step;

import java.util.Comparator;

public class StepsComparator implements Comparator<Short> {
    public boolean shouldUpdate(Short oldValue, Short newValue) {
        return 0 < compare(oldValue, newValue);
    }

    @Override
    public int compare(Short o1, Short o2) {
        return Steps.compare(o1, o2);
    }
}
