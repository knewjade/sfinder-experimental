import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ProgressEstimator(private val max: Int) {
    private val counter = AtomicInteger()
    private val start = System.nanoTime()
    private val milestones = listOf(
        0.001, 0.005, 0.01, 0.05, 0.1, 0.25, 0.5, 0.75, 0.9, 1.0, Double.MAX_VALUE
    )
    private var next = 0

    companion object {
        fun start(max: Int): ProgressEstimator {
            val estimator = ProgressEstimator(max)
            println("<start: ${LocalDateTime.now()}> max: ${max}")
            return estimator
        }
    }

    private fun isShows(current: Int): Boolean {
        return milestones[next] * max <= current
    }

    fun increment() {
        val current = counter.incrementAndGet()
        if (isShows(current)) {
            synchronized(this) {
                if (isShows(current)) {
                    val end = System.nanoTime()
                    val duration = end - start
                    val average = duration.toDouble() / current

                    val consume = TimeUnit.NANOSECONDS.toSeconds((average * current).toLong())
                    val estimate = TimeUnit.NANOSECONDS.toSeconds((average * (max - current)).toLong())

                    val percentage = "%5.1f%%".format(milestones[next] * 100)

                    val finish = LocalDateTime.now().plusSeconds(estimate)

                    println("[$percentage] finish: $finish <consume: ${time(consume)}> count: $current")
                    next += 1
                }
            }
        }
    }

    private fun time(totalSeconds: Long): String {
        var times = totalSeconds
        val hours = times / 3600
        times %= 3600
        val minutes = times / 60
        val seconds = times % 60
        return "${hours}h${minutes}m${seconds}s"
    }
}
