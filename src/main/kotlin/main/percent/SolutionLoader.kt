package main.percent

import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import main.domain.AllMinoIndexes
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import java.util.stream.Collectors.toList

interface SolutionLoader {
    val requires: List<MinimalOperationWithKey>

    fun load(fixMinos: List<MinimalOperationWithKey>): Map<PieceCounter, List<Solution>>
}

class CachedSolutionLoader(private val allMinoIndexes: AllMinoIndexes, val index: Index, private val requiresInt: Set<Int>) : SolutionLoader {
    override val requires = requiresInt.map { index.get(it)!! }

    private val preSolutions by lazy {
        filterSolutions(requiresInt)
    }

    private fun filterSolutions(requires: Set<Int>): List<MinoIndexes> {
        if (requires.isEmpty())
            return allMinoIndexes.indexes

        return allMinoIndexes.indexes.stream()
                .filter { indexes ->
                    requires.all { indexes.contains(it) }
                }
                .collect(Collectors.toList())
    }

    override fun load(fixMinos: List<MinimalOperationWithKey>): Map<PieceCounter, List<Solution>> {
        val numbers = fixMinos.map { index.get(it)!! }.toSet()
        val allNumbers = requiresInt + numbers

        return preSolutions.stream()
                .filter { indexes ->
                    numbers.all { indexes.contains(it) }
                }
                .map {
                    val map: List<MinimalOperationWithKey> = it.numbersStream
                            .filter { !allNumbers.contains(it) }
                            .map { index.get(it)!! }
                            .collect(toList())
                    Solution(map)
                }
                .collect(Collectors.groupingBy({ it: Solution -> PieceCounter(it.keys.stream().map { it.piece }) }))
    }
}

class DirectSolutionLoader(val path: Path, val index: Index, requires: Set<Int>) : SolutionLoader {
    override val requires = requires.map { index.get(it)!! }

    private val preSolutions by lazy {
        filterSolutions(requires)
    }

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

    override fun load(fixMinos: List<MinimalOperationWithKey>): Map<PieceCounter, List<Solution>> {
        val numbers = fixMinos.map { index.get(it) }.toSet()

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