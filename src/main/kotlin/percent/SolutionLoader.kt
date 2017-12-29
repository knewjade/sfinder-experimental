package percent

import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

class SolutionLoader(val path: Path, val index: Index, requires: Set<Int>) {
    val requires = requires.map { index.get(it)!! }
    val preSolutions = filterSolutions(requires)

    private fun filterSolutions(requires: Set<Int>): List<Set<Int>>? {
        if (requires.isEmpty())
            return null

        return Files.lines(path)
                .map { line ->
                    line.split(",").map { it.toInt() }.toSet()
                }
                .filter { it.containsAll(requires) }
                .map {
                    it.filter { !requires.contains(it) }.toSet()
                }
                .collect(Collectors.toList())
    }

    fun load(fixMinos: List<MinimalOperationWithKey>): Map<PieceCounter, List<Solution>> {
        val numbers = fixMinos.map { index.get(it) }.toSet()
//        println("next numbers: ${numbers}")

        val stream = preSolutions?.stream() ?: Files.lines(path).map { line ->
            line.split(",").map { it.toInt() }.toSet()
        }

        return stream
                .filter { it.containsAll(numbers) }
                .map {
                    val map: List<MinimalOperationWithKey> = it.filter { !numbers.contains(it) }
                            .map { index.get(it)!! }
                    Solution(map)
                }
                .collect(Collectors.groupingBy({ it: Solution -> PieceCounter(it.keys.stream().map { it.piece }) }))
    }
}