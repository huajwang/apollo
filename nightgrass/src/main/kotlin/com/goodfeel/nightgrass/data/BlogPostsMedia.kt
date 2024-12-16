package com.goodfeel.nightgrass.data

import com.goodfeel.nightgrass.util.MediaType
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("e_mall_blog_posts_media")
data class BlogPostsMedia(
    val mediaId: Int? = null,
    val postId: Int,
    val type: MediaType,
    val filePath: String,
    val caption: String? = null,
    val uploadedAt: LocalDateTime = LocalDateTime.now()
)
