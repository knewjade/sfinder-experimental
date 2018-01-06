package main.aws

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.ListObjectsRequest
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class Bucket(private val client: AmazonS3, private val bucketName: String) {
    fun putObject(path: String, content: String) {
        client.putObject(bucketName, path, content)
    }

    fun existsObject(path: String): Boolean {
        return client.doesObjectExist(bucketName, path)
    }

    fun getObject(path: String): String? {
        return try {
            client.getObject(bucketName, path)
        } catch (e: AmazonS3Exception) {
            null
        }?.objectContent?.let { convert(it) }
    }

    fun deleteObject(path: String) {
        client.deleteObject(bucketName, path)
    }

    fun listAllKeys(maxKeys: Int = Int.MAX_VALUE): List<String> {
        val request = ListObjectsRequest().also {
            it.bucketName = bucketName
            it.maxKeys = maxKeys
        }

        val keys = mutableListOf<String>()
        do {
            val objects = client.listObjects(request)
            keys.addAll(objects.objectSummaries.map { it.key!! })

            if (maxKeys <= keys.size) break

            request.marker = objects.nextMarker
        } while (objects.isTruncated)

        return keys.toList()
    }

    @Throws(IOException::class)
    private fun convert(inputStream: InputStream): String {
        val reader = InputStreamReader(inputStream)
        return StringBuilder().run {
            val buffer = CharArray(512)

            while (true) {
                reader.read(buffer).takeIf { 0 <= it }
                        ?.let { append(buffer, 0, it) }
                        ?: break
            }

            toString()
        }
    }
}