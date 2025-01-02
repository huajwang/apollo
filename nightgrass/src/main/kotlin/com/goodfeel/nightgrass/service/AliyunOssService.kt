package com.goodfeel.nightgrass.service

import com.aliyun.oss.OSS
import com.aliyun.oss.OSSClientBuilder
import com.aliyun.oss.model.DeleteObjectsRequest
import com.goodfeel.nightgrass.web.util.Utility
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class AliyunOssService(
    @Value("\${aliyun.oss_access_key_id}") private val accessKeyId: String,
    @Value("\${aliyun.oss_access_key_secret}") private val accessKeySecret: String
) {
    private val logger = LoggerFactory.getLogger(AliyunOssService::class.java)

    private val ossClient: OSS = OSSClientBuilder().build(
        Utility.OSS_ENDPOINT, accessKeyId, accessKeySecret)

    private fun uploadFile(inputStream: InputStream, fileName: String): Mono<String> {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val objectName = "${Utility.APPLICATION_NAME}/$timestamp-$fileName" // Path in the bucket

        return Mono.fromCallable {
            ossClient.putObject(Utility.OSS_BUCKET_NAME, objectName, inputStream)
            objectName
        }.doOnTerminate {
            inputStream.close()
        }.doOnError {
            logger.error("Error upload file to Aliyun OSS. Error: ${it.message}")
        }
    }

    fun uploadMultipleFiles(files: Flux<FilePart>): Flux<String> {
        return files.flatMap { filePart ->
            convertToInputStream(filePart).flatMap { inputStream ->
                uploadFile(inputStream, filePart.filename())
            }
        }.onErrorContinue { ex, obj ->
            // Log the error and continue with other files
            logger.error("Failed to upload file: $obj, Error: $ex")
        }
    }

    fun uploadSingleFile(filePartMono: Mono<FilePart>): Mono<String> {
        return filePartMono.flatMap { filePart ->
            convertToInputStream(filePart).flatMap { inputStream ->
                uploadFile(inputStream, filePart.filename())
            }
        }
    }

    private fun convertToInputStream(filePart: FilePart): Mono<InputStream> {
        return DataBufferUtils.join(filePart.content())
            .map { dataBuffer ->
                val byteArray = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(byteArray)
                DataBufferUtils.release(dataBuffer) // Important to release the buffer
                ByteArrayInputStream(byteArray)
            }
    }

    fun deletePhotoOnOss(objectName: String): Mono<Void> {
        return Mono.fromCallable {
            try {
                ossClient.deleteObject(Utility.OSS_BUCKET_NAME, objectName)
                logger.debug("Successfully deleted object from OSS: $objectName")
            } catch (ex: Exception) {
                logger.error("Failed to delete object from OSS: $objectName. Error: ${ex.message}")
                throw ex
            }
        }.then()
    }


    /**
     * Delete files from OSS bucket in batch
     */
    fun deletePhotosOnOss(objectNames: List<String>): Mono<Void> {
        return Mono.fromCallable {
            val shortObjectNames = Utility.removeOssPrefix(objectNames)

            try {
                if (objectNames.isNotEmpty()) {
                    val deleteObjectsRequest = DeleteObjectsRequest(Utility.OSS_BUCKET_NAME)
                        .withKeys(shortObjectNames)
                        .withQuiet(true) // Suppresses detailed response for large batches

                    val deleteObjectsResult = ossClient.deleteObjects(deleteObjectsRequest)
                    logger.info("Deleted objects from OSS: ${deleteObjectsResult.deletedObjects}")
                } else {
                    logger.info("No objects to delete from OSS.")
                }
            } catch (ex: Exception) {
                logger.error("Failed to delete objects from OSS. Error: ${ex.message}")
                throw ex
            }
        }.then()
    }

}
