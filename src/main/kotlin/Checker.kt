import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import core.mino.Piece
import main.aws.Bucket

fun main(args: Array<String>) {
    val s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()
    val bucket = Bucket(s3Client, "fumen")
    (1..8).forEach { cycle ->
        Piece.values().forEach { piece ->
            val key = "$cycle/$piece/vhAAgWBAUAAAA"
            if (!bucket.existsObject(key))
                println(key)
        }
    }
}