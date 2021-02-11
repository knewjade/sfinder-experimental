import common.tetfu.Tetfu
import common.tetfu.TetfuElement
import common.tetfu.TetfuPage
import common.tetfu.common.ColorConverter
import core.mino.MinoFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*
import kotlin.math.pow

data class Line(val index: Int, val pieces: String, val fumens: Set<String>)

data class Line2(val index: Int, val fumens: BitSet)

data class Solution(val fumen: String, val pieceBit: BitSet)

data class BitSize(val fumen: Int, val sequences: Int) {
}

fun toItem(index: Int, elements: List<String>): Line {
    // [ツモ, 対応地形数, 使用ミノ, 未使用ミノ, テト譜]
    val pieces = elements[0]
    val fumens = elements[4].split(";").filter { it.isNotEmpty() }
    assert(fumens.size == Integer.parseInt(elements[1]))
    return Line(index, pieces, fumens.toSet())
}

fun BitSet.copy(): BitSet {
    val copied = BitSet(this.size())
    copied.or(this)
    return copied
}

fun BitSet.invert(size: Int) {
    this.flip(0, size)
}

fun candidatesToUncoveredPieceBit(solutions: List<Solution>, candidates: BitSet, sequencesSize: Int): BitSet {
    val pieceBit = BitSet(sequencesSize)
    candidates.stream().forEach {
        pieceBit.or(solutions[it].pieceBit)
    }
    pieceBit.invert(sequencesSize)
    return pieceBit
}

fun main(args: Array<String>) {
    filterMain(args)
}

fun filterMain(args: Array<String>) {
    val lines = File("path.csv").readLines().asSequence()
            .drop(1)
            .map { it.split(",") }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, elements -> toItem(index, elements) }
            .toList()

    if (args.isEmpty()) {
        val populationScale = 5.0
        val childrenScale = 1.0
        val mutationScale = 6.0  // < dimension
        val maxGeneration = 300000
        run(lines, Settings(populationScale, childrenScale, mutationScale, maxGeneration))
    } else if (args[0] == "verify") {
        val output = File("output.txt").readLines().asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
        verify(lines, output)
    } else if (args.size == 4) {
        val populationScale = args[0].toDouble()
        val childrenScale = args[1].toDouble()
        val mutationScale = args[2].toDouble()
        val maxGeneration = args[3].toInt()
        run(lines, Settings(populationScale, childrenScale, mutationScale, maxGeneration))
    }
}

fun verify(lines: List<Line>, answer: List<String>) {
    val selected = answer.toSet()
    val covered = lines.asSequence()
            .filter { it.fumens.any { fumen -> selected.contains(fumen) } }
            .count()

    println("${answer.size} solutions")

    val succeedSequences = lines.filter { it.fumens.isNotEmpty() }.count()
    if (covered == succeedSequences) {
        println("OK, all sequences are covered [covered sequences = $covered]")
    } else {
        println("### all sequences are NOT covered [covered sequences = $covered] ###")
    }

    val tetfu = Tetfu(MinoFactory(), ColorConverter())
    val elements = selected.map { tetfu.decode(it.substring(5)) }
            .fold(mutableListOf<TetfuPage>()) { acc, pages ->
                val element = pages.first()
                acc.add(element)
                acc
            }
            .map {
                TetfuElement(it.field, it.colorType, it.rotate, it.x, it.y, it.comment)
            }

    val data = tetfu.encode(elements)
    println("http://fumen.zui.jp/?v115@$data")
}

fun run(lines_: List<Line>, settings: Settings) {
    val lines = lines_.asSequence()
            .filter { it.fumens.isNotEmpty() }
            .mapIndexed { index, line -> Line(index, line.pieces, line.fumens) }
            .toList()

    val solutionsPre = lines.asSequence()
            .map { it.fumens }
            .flatMap { it.asSequence() }
            .toSet()
            .map { fumen ->
                val bitSet = BitSet(lines.size)
                lines.forEach {
                    if (it.fumens.contains(fumen)) {
                        bitSet.set(it.index)
                    }
                }
                Solution(fumen, bitSet)
            }
            .shuffled()
            .toList()

    val solutions = solutionsPre.asSequence()
            .filter { solution ->
                solutionsPre
                        .filter { solution.fumen != it.fumen }
                        .all {
                            val copied = solution.pieceBit.copy()
                            val same = it.pieceBit == copied

                            copied.and(it.pieceBit)
                            val isNotSubset = solution.pieceBit != copied
                            same || isNotSubset
                        }
            }
            .toList()
    println("selected solutions = ${solutions.size}")

    val fumenToSolutionIndex = solutions.asSequence()
            .mapIndexed { index, bits -> bits.fumen to index }
            .toMap()
    assert(fumenToSolutionIndex.size == solutions.size)

    val sequenceToLine2 = lines.asSequence()
            .map { line ->
                val fumens = BitSet(solutions.size)
                line.fumens.forEach { fumen ->
                    fumenToSolutionIndex[fumen]?.let { fumens.set(it) }
                }
                Line2(line.index, fumens)
            }
            .toList()

    val ga = GA(sequenceToLine2, solutions, BitSize(solutions.size, lines.size))
    ga.run(settings)
}

data class Fitness(val gene: BitSet, val fitness: Double)

data class Settings(
        val populationScale: Double, val childrenScale: Double,
        val mutationScale: Double, val maxGeneration: Int
)

class GA(private val sequenceToLine2: List<Line2>, private val solutions: List<Solution>, private val size: BitSize) {
    private val random = Random()

    fun fix(copied1: BitSet) {
        val uncovered = candidatesToUncoveredPieceBit(solutions, copied1, size.sequences)
        if (uncovered.isEmpty) {
            return
        }

        val requiredSolutions = BitSet(size.fumen)
        uncovered.stream().forEach {
            requiredSolutions.or(sequenceToLine2[it].fumens)
        }
        requiredSolutions.xor(copied1)

        val requiredIndexList = Sequence { requiredSolutions.stream().iterator() }.toList().shuffled()

        for (selected in requiredIndexList) {
            copied1.set(selected)
            val covered = solutions[selected].pieceBit.copy()
            covered.invert(size.sequences)
            uncovered.and(covered)

            if (uncovered.isEmpty) {
                return
            }
        }

        assert(false)
    }

    fun run(settings: Settings) {
        val dimension = size.fumen
        val population = (1..(dimension * settings.populationScale).toInt()).map {
            val gene = BitSet(dimension)
            (0 until dimension).forEach { index ->
                if (random.nextBoolean()) {
                    gene.set(index)
                }
            }
            fix(gene)
            to(gene)
        }.toMutableList()

        val mutate = 1.0 / dimension * settings.mutationScale

        (1..settings.maxGeneration).forEach { generation ->
            population.shuffle()

            val children = (1..(dimension * settings.childrenScale).toInt()).map {
                val mask1 = BitSet(dimension)
                (0 until dimension).forEach { index ->
                    if (random.nextBoolean()) {
                        mask1.set(index)
                    }
                }
                val mask2 = mask1.copy()
                mask2.invert(dimension)

                val parent1 = random.nextInt(dimension + 1)
                val copied1 = population[parent1].gene.copy()
                copied1.and(mask1)

                val parent2 = random.nextInt(dimension + 1)
                val copied2 = population[parent2].gene.copy()
                copied2.and(mask2)

                copied1.or(copied2)

                (0 until dimension).forEach { index ->
                    if (random.nextDouble() < mutate) {
                        copied1.flip(index)
                    }
                }

                fix(copied1)


                copied1
            }.toMutableList()

            children.add(population[0].gene)
            children.add(population[1].gene)

            val results = children
                    .map { to(it) }
                    .sortedByDescending { it.fitness }
                    .toList()

            val a = results
                    .mapIndexed { index, fitness -> index to fitness.fitness.pow(2) }
            val sum = a.map { it.second }.sum()
            val v = random.nextDouble() * sum

            var roulette: Int? = null
            run loop@{
                var t = 0.0
                a.forEach {
                    t += it.second
                    if (v < t) {
                        roulette = it.first
                        return@loop
                    }
                }
            }
            assert(roulette != null)

            population[0] = results[0]
            population[1] = results[roulette!!]

            if (generation % 100 == 0) {
                println("---")

                val sorted = population
                        .sortedByDescending { it.fitness }
                        .toList()

                println(generation)

                val average = sorted.map { it.fitness }.average()
                println("average = $average")

                val best = sorted.first()
                best.let {
                    if (it.fitness < 1.0) {
                        println("best = ${it.fitness * 100} % covered")
                    } else {
                        println("best = ${it.gene.cardinality()} solutions")
                    }
                }

                val worst = sorted.last()
                worst.let {
                    if (it.fitness < 1.0) {
                        println("worst = ${it.fitness * 100} % covered")
                    } else {
                        println("worst = ${it.gene.cardinality()} solutions")
                    }
                }

                val fumens = Sequence { best.gene.stream().iterator() }
                        .map { solutions[it] }
                        .map { it.fumen }
                        .sorted()
                        .toList()

                val printWriter = PrintWriter(BufferedWriter(FileWriter("output.txt")))
                printWriter.use { writer ->
                    fumens.forEach { writer.println(it) }
                }
            }
        }
    }

    private fun to(gene: BitSet): Fitness {
        return Fitness(gene, evaluate(gene))
    }

    private fun evaluate(bitSet: BitSet): Double {
        val uncovered = candidatesToUncoveredPieceBit(solutions, bitSet, size.sequences).cardinality()
        return if (0 < uncovered) {
            1.0 - (uncovered.toDouble() / size.sequences)
        } else {
            size.fumen - bitSet.cardinality() + 1.0
        }
    }
}