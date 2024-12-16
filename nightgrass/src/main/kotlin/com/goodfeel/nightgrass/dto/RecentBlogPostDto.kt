package com.goodfeel.nightgrass.dto

import java.time.LocalDateTime

data class RecentBlogPostDto(
    val postId: Int,
    val title: String,
    val thumbnail: String,
    val publishedAt: LocalDateTime
)

