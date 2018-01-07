import main.percent.ResultsSerializer
import main.caller.ContentCaller
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


fun main(args: Array<String>) {
    val serializer = ResultsSerializer()

    Files.walk(Paths.get("input/fumen"))
            .map { it.toFile() }
            .filter { it.isFile && it.name != ".DS_Store" }
            .forEach {
                val content = it.readText()
                val caller = ContentCaller(content)
                val results = caller.call()
                val newContent = serializer.str(results)

                println("${content != newContent}  $it")
                val file = File("output/${it.path}")
                file.parentFile.mkdirs()
                file.writeText(newContent)
            }
}