import core.field.Field
import core.field.FieldFactory
import core.field.FieldView
import core.mino.Mino
import core.mino.MinoFactory
import core.mino.Piece
import core.srs.MinoRotation
import core.srs.Rotate
import lib.BooleanWalker
import java.util.stream.Collectors

fun main(args: Array<String>) {
    val runner = Runner()
    runner.test()
}

class Runner(private val height: Int = 12) {
    private val minoFactory = MinoFactory()
    private val minoRotation = MinoRotation()

    fun test() {
        val currentMino = minoFactory.create(Piece.T, Rotate.Spawn)

        (-currentMino.minY until height - currentMino.maxY).forEach { py ->
            (-currentMino.minX until 10 - currentMino.maxX).forEach { px ->
                val (field, maps) = checkLeft(currentMino, px, py)
                val freeze = field.freeze(height)
                freeze.inverse()
                freeze.remove(currentMino, px, py)
                println(FieldView.toString(freeze))
                println("#")

            }
        }

    }

    fun checkLeft(current: Mino, x: Int, y: Int): Pair<Field, Map<Int, List<Field>>> {
        // 回転後の移動量
        val patterns: Map<Position, Int> = minoRotation.getLeftPatternsFrom(current)
                .mapIndexed { index, pattern -> Position(pattern[0], pattern[1]) to index }
                .toMap()

        // 左回転後のミノ
        val leftMino = minoFactory.create(current.piece, current.rotate.leftRotate)

        // 回転に影響を与えるブロック
        val field = FieldFactory.createField(height)
        for (pattern in patterns.keys) {
            // 回転前のミノを取り除く // 回転前にミノを置けるようにする
            field.remove(current, x, y)

            // 回転後のブロックを埋める
            for (position in leftMino.positions) {
                val dx = x + pattern.x + position[0]
                val dy = y + pattern.y + position[1]
                if (dx in 0 until 10 && dy in 0 until height) {
                    field.setBlock(dx, dy)
                }
            }
        }

        // 影響のあるブロックを座標に変換する
        val blocks = (0 until 10)
                .flatMap { py ->
                    (0 until field.maxFieldHeight).map { px -> Position(px, py) }
                }
                .filter { !field.isEmpty(it.x, it.y) }

        // 回転先とフィールドを紐付ける
        return field to BooleanWalker.walk(blocks.size)
                .map { booleans ->
                    val blockField = FieldFactory.createField(height)
                    booleans.zip(blocks)
                            .filter { it.first }
                            .map { it.second }
                            .forEach { blockField.setBlock(it.x, it.y) }
                    blockField
                }
                .filter { it.canPut(current, x, y) }
                .collect(Collectors.groupingBy {
                    val pattern = minoRotation.getKicksWithLeftRotation(it, current, leftMino, x, y)
                    if (pattern == null) -1 else patterns[Position(pattern[0], pattern[1])]
                })
    }

    fun checkRight(current: Mino, x: Int, y: Int): Pair<Field, Map<Int, List<Field>>> {
        // 回転後の移動量
        val patterns: Map<Position, Int> = minoRotation.getRightPatternsFrom(current)
                .mapIndexed { index, pattern -> Position(pattern[0], pattern[1]) to index }
                .toMap()

        // 右回転後のミノ
        val rightMino = minoFactory.create(current.piece, current.rotate.rightRotate)

        // 回転に影響を与えるブロック
        val field = FieldFactory.createField(height)
        for (pattern in patterns.keys) {
            // 回転前のミノを取り除く // 回転前にミノを置けるようにする
            field.remove(current, x, y)

            // 回転後のブロックを埋める
            for (position in rightMino.positions) {
                val dx = x + pattern.x + position[0]
                val dy = y + pattern.y + position[1]
                if (dx in 0 until 10 && dy in 0 until height) {
                    field.setBlock(dx, dy)
                }
            }
        }

        // 影響のあるブロックを座標に変換する
        val blocks = (0 until 10)
                .flatMap { py ->
                    (0 until field.maxFieldHeight).map { px -> Position(px, py) }
                }
                .filter { !field.isEmpty(it.x, it.y) }

        // 回転先とフィールドを紐付ける
        return field to BooleanWalker.walk(blocks.size)
                .map { booleans ->
                    val blockField = FieldFactory.createField(height)
                    booleans.zip(blocks)
                            .filter { it.first }
                            .map { it.second }
                            .forEach { blockField.setBlock(it.x, it.y) }
                    blockField
                }
                .filter { it.canPut(current, x, y) }
                .collect(Collectors.groupingBy {
                    val pattern = minoRotation.getKicksWithRightRotation(it, current, rightMino, x, y)
                    if (pattern == null) -1 else patterns[Position(pattern[0], pattern[1])]
                })
    }
}

data class Position(val x: Int, val y: Int) {
    fun add(dx: Int, dy: Int): Position {
        return Position(x + dx, y + dy)
    }
}