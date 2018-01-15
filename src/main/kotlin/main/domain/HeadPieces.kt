package main.domain

import common.datastore.MinimalOperationWithKey
import common.datastore.PieceCounter
import core.mino.Piece

class HeadPieces(val headMinos: List<MinimalOperationWithKey>, val current: Piece) {
    val representation: String = headMinos.map { it.piece }.sorted().joinToString("") + current.name
    val allPieceCounter: PieceCounter = PieceCounter(headMinos.map { it.piece } + current)
}