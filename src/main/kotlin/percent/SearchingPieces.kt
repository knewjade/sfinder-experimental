package percent

import common.datastore.PieceCounter
import common.datastore.blocks.LongPieces
import common.datastore.blocks.Pieces
import common.pattern.LoadedPatternGenerator
import java.util.stream.Collectors

class SearchingPieces(pattern: String, headPieceCounter: PieceCounter) {
    val piecesMap: Map<LongPieces, List<Pieces>>
    val allCount: Int

    init {
        val piecesSet = createSet(pattern, headPieceCounter)
        piecesMap = createMap(piecesSet, headPieceCounter)
        allCount = piecesSet.size
    }

    private fun createSet(pattern: String, headPieceCounter: PieceCounter): Set<Pieces> {
        val generator = LoadedPatternGenerator(pattern)
        val numOfHead = headPieceCounter.blockStream.count() + 1

        return generator.blocksStream().parallel()
                .filter {
                    val pieceCounter = PieceCounter(it.blockStream().limit(numOfHead))
                    pieceCounter.containsAll(headPieceCounter)
                }
                .map {
                    val pieces = it.pieces
                    for (entry in headPieceCounter.enumMap.entries)
                        for (x in 0 until entry.value)
                            pieces.remove(entry.key)
                    LongPieces(pieces)
                }
                .collect(Collectors.toSet())
    }

    private fun createMap(piecesSet: Set<Pieces>, headPieceCounter: PieceCounter): Map<LongPieces, List<Pieces>> {
        val maxDepth = 10L - headPieceCounter.enumMap.values.sum()
        return piecesSet
                .parallelStream()
                .collect(Collectors.groupingBy({ it: Pieces -> LongPieces(it.blockStream().limit(maxDepth).map { it }) }))
    }
}