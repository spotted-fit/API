package fit.spotted.api.storage

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

object S3Config {
    val accessKey = System.getenv("S3_ACCESS_KEY")
    val secretKey = System.getenv("S3_SECRET_ACCESS_KEY")
    val endpoint = System.getenv("S3_ENDPOINT")
    val region = System.getenv("S3_REGION")
    val bucketName = System.getenv("S3_BUCKET")

    val s3Client: S3Client = createS3Client()

    private fun createS3Client(): S3Client {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)

        return S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .forcePathStyle(true)
            .build()
    }
}

