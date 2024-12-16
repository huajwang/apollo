package com.goodfeel.nightgrass.data

import com.goodfeel.nightgrass.util.BlogPostStatus
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("e_mall_blog_posts")
data class BlogPost(
    val postId: Int? = null,
    val authorId: String,
    val title: String,
    val slug: String,
    val content: String,
    val abstract: String,
    val categoryId: Int,
    val status: BlogPostStatus = BlogPostStatus.DRAFT,
    val thumbnail: String,
    val mainMediaId: Int,
    val stickyPinNo: Int = 0, // 0 not pin on top
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime? = null,
    val publishedAt: LocalDateTime? = null,
)
