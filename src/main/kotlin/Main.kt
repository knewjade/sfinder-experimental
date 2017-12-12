fun main(args: Array<String>) {

    val array = listOf(1, -4, 3, 2, 1)
    val max = array.indexOf(array.maxBy { it * it })
    println(max)

    println("Hello Kotlin!!")
    val a = 3  // final

    infix fun Int.sum(other: Int): Int = this + other
    println(3 sum 1)  // 4

    println(Char.MAX_HIGH_SURROGATE)

    for (ch in 'a'..'c')
        print(ch)
    // -> abc

    k()
    3.sum(1)
}

fun k(): Int {
//    return 2 sum 3
    return 3
}
