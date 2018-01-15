package main.domain

import main.percent.MinoIndexes
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

class AllMinoIndexes(path: Path) {
    val indexes: List<MinoIndexes> by lazy {
        Files.lines(path)
                .map { line ->
                    MinoIndexes(line.split(",").map { it.toInt() })
                }
                .collect(Collectors.toList())
    }
}