import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import common.parser.StringEnumTransform
import main.Bucket


fun main(args: Array<String>) {
//    to()
//    revert()
    renameField()
}

private fun renameField() {
    val s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()
    val from = Bucket(s3Client, "fumen")
    val to = Bucket(s3Client, "fumens")
    val keys = from.listAllKeys()
    println(keys.size)
    for (key in keys) {
        val regex = Regex("([0-8])/([TIOLJSZ]+)/([a-zA-Z0-9+/?]+)")
        assert(regex.matches(key), { key })
        regex.find(key)!!.groupValues.let {
            val obj = from.getObject(key)!!
            val (cycle, minos, field) = Triple(it[1], it[2], it[3])
            val newField = field.replace("/", "_")
            if (field != newField) println("rename: $key")
            val newKey = "$cycle/$minos/$newField"
            to.putObject(newKey, obj)
        }
        Thread.sleep(10L)
    }
}

private fun to() {
    val s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()
    val from = Bucket(s3Client, "fumen")
    val to = Bucket(s3Client, "fumen2")
    for (key in from.listAllKeys()) {
        println(key)
        val split = key.split("/")
        val (cycle, mino, field) = split

        val newMino = mino.toCharArray()
                .map { it.toString() }
                .map { StringEnumTransform.toPiece(it) }
                .sorted()
                .joinToString("")

        val newKey = "$cycle/$newMino/$field"
        val obj = from.getObject(key)!!
        to.putObject(newKey, obj)
        Thread.sleep(30L)
    }
}

private fun revert() {
    val s3Client = AmazonS3ClientBuilder.standard()
            .withRegion(Regions.AP_NORTHEAST_1)
            .build()
    val from = Bucket(s3Client, "fumen2")
    val to = Bucket(s3Client, "fumen")
    for (key in from.listAllKeys()) {
        println(key)
        val obj = from.getObject(key)!!
        to.putObject(key, obj)
        Thread.sleep(10L)
    }
}