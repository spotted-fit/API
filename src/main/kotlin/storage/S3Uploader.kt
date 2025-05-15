package fit.spotted.api.storage

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3Uploader(
    private val s3Client: S3Client,
    private val bucketName: String
) {
    fun uploadImage(photoBytes: ByteArray, imagePath: String): String {
        val objectKey = "userpics/$imagePath"

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .contentType("image/png")
            .acl("public-read")
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(photoBytes))

        return objectKey
    }

    fun deleteImage(imagePath: String) {

        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(imagePath)
            .build()

        s3Client.deleteObject(request)
    }
}
