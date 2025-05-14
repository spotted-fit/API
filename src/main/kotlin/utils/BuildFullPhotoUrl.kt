package utils

import storage.S3Config

fun buildFullPhotoUrl(path: String?): String {
    if (path.isNullOrEmpty()) return ""
    return "${S3Config.endpoint}/object/public/${S3Config.bucketName}/$path".replace("/s3", "")
}