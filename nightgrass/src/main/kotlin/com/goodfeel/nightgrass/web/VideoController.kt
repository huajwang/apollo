package com.goodfeel.nightgrass.web

import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpHeaders
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@Controller
@RequestMapping("/videos")
class VideoController {

    private val logger = LoggerFactory.getLogger(VideoController::class.java)

    private val uploadPath = Paths.get("uploads/videos")

    init {
        // Ensure the directory exists
        if (!uploadPath.toFile().exists()) {
            uploadPath.toFile().mkdirs()
        }
    }

    @GetMapping
    fun getVideos(model: Model): Mono<String> {
        return Mono.fromCallable {
            val videoFiles = uploadPath.toFile().listFiles { file: File ->
                file.isFile && file.extension == "mp4"
            } ?: emptyArray()

            videoFiles.map { file ->
                logger.debug("video file name = ${file.name}")
                mapOf("name" to file.name, "url" to "/videos/${file.name}")
            }
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext { videos ->
                logger.debug("Size of videos = ${videos.size}")
                model.addAttribute("videos", videos)
            }
            .doOnSuccess { logger.debug("Reactive chain completed.") }
            .thenReturn("video-gallery") // Return the template name reactively
    }

    @GetMapping("/{filename}")
    fun getVideo(@PathVariable filename: String, @RequestHeader headers: HttpHeaders): Mono<ResponseEntity<Resource>> {
        val videoPath: Path = uploadPath.resolve(filename)

        return Mono.fromCallable {
            val resource = UrlResource(videoPath.toUri())
            if (!resource.exists() || !resource.isReadable) {
                throw RuntimeException("File not found or not readable")
            }
            resource as Resource // Explicitly cast UrlResource to Resource
        }.map { resource ->
            val contentLength = resource.contentLength()
            val rangeHeaders = headers.range

            val builder = ResponseEntity.status(206).header(HttpHeaders.CONTENT_TYPE, "video/mp4") // Default to partial content

            if (rangeHeaders.isNotEmpty()) {
                val range = rangeHeaders[0]
                val start = range.getRangeStart(contentLength)
                val end = range.getRangeEnd(contentLength)

                builder.header(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$contentLength")
                    .body(resource)
            } else {
                ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
                    .body(resource)
            }
        }.onErrorResume {
            Mono.just(ResponseEntity.notFound().build())
        }
    }

    @PostMapping("/upload")
    fun uploadVideo(@RequestPart("file") filePart: FilePart): Mono<ResponseEntity<String>> {
        val videoPath = uploadPath.resolve(filePart.filename())
        return filePart.transferTo(videoPath)
            .then(Mono.just(ResponseEntity.ok("Video uploaded successfully: ${filePart.filename()}")))
    }

}
