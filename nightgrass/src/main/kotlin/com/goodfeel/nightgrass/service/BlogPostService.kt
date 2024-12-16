package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.dto.CategoryPostCountDto
import com.goodfeel.nightgrass.dto.StickyBlogPostDto
import com.goodfeel.nightgrass.repo.BlogPostRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class BlogPostService(
    private val blogPostRepository: BlogPostRepository
) {
    fun getStickyBlogPosts(): Flux<StickyBlogPostDto> =
        blogPostRepository.findAllStickyPosts()

    fun getPostCountByCategory(): Flux<CategoryPostCountDto> =
        blogPostRepository.findBlogPostsCountByCategory()
}
