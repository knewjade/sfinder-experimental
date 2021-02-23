// 解数: 503634
fun main() {
//    val field = FieldFactory.createField(24)
//    field.setBlock(0, 0)
//    println(FieldView.toReducedString(field))
//
//    val height = 12
//    val sfSpin = SFSpin(MinoFactory(), MinoShifter(), MinoRotation.create(), height)
//    val results = sfSpin.run(FieldFactory.createField(height))
//    println(results.size)

    ExpandOpeningsTo7Mino().run(true)
}