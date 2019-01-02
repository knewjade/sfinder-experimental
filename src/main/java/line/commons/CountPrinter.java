package line.commons;

import lib.Stopwatch;

import java.util.concurrent.TimeUnit;

public class CountPrinter {
    private final int checkPoint;
    private final int max;
    private final Stopwatch stopwatch;
    private int counter;

    public CountPrinter(int checkPoint) {
        this(checkPoint, -1);
    }

    public CountPrinter(int checkPoint, int max) {
        this.checkPoint = checkPoint;
        this.max = max;
        this.stopwatch = Stopwatch.createStartedStopwatch();
        this.counter = 0;
    }

    public synchronized void increaseAndShow() {
        stopwatch.stop();

        counter += 1;

        if (counter % checkPoint == 0) {
            show();
        }

        stopwatch.start();
    }

    private void show() {
        double nanoAverageTime = stopwatch.getNanoAverageTime();
        if (max != -1) {
            long nanoRemainder = (long) nanoAverageTime * (max - counter);
            long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoRemainder);
            long minutes = seconds / 60;
            System.out.printf("%d/%d [%5.1f%%]: remainder=%2d:%02d%n", counter, max, 100.0 * counter / max, minutes, seconds % 60);
        } else {
            long nanoElapsed = (long) nanoAverageTime * counter;
            long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoElapsed);
            long minutes = seconds / 60;
            System.out.printf("%d: elapsed=%2d:%02d%n", counter, minutes, seconds % 60);
        }
    }
}
