import entry.path.output.MyFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

fun main() {
    val fileName = "resources/tspins.txt"
    val lines = Files.newBufferedReader(Paths.get(fileName), StandardCharsets.UTF_8).use { reader ->
        reader.lines().filter { it.isNotBlank() }.collect(Collectors.toList())
    }
    lines.shuffle()
    lines.chunked(105000).forEachIndexed { index, list ->
        MyFile("tspins${index}.txt").newAsyncWriter().use { it.writeAndNewLine(list) }
    }
}
