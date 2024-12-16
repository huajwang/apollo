package com.goodfeel.nightgrass.dto

import com.goodfeel.nightgrass.util.MediaType
import java.time.LocalDateTime

data class StickyBlogPostDto(
    val postId: Int,
    val title: String,
    val abstract: String,
    val stickyPinNo: Int,
    val publishedAt: LocalDateTime,
    val authorName: String,
    val categoryName: String,
    val mediaFilePath: String,
    val mediaType: MediaType,
    val mediaCaption: String
)

