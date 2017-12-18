import main.workSQS

// 1,vhAAgWBAUAAAA,,T,1.0
// 1,9gB8HeC8GeC8GeD8PeAgWBAUAAAA,OJI,L,1.0
// 1,9gA8IeA8IeB8HeD8PeAgWBAUAAAA,IL,T,1.0
fun main(args: Array<String>) {
    workSQS(
            "fumen-dev",
            "dev-test",
            "dev-test",
            "dev-test",
            1.0,
            true
    )
}