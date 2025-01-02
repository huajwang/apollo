package com.goodfeel.nightgrass.service

import com.goodfeel.nightgrass.data.BlogPost
import com.goodfeel.nightgrass.dto.CategoryPostCountDto
import com.goodfeel.nightgrass.dto.RecentBlogPostDto
import com.goodfeel.nightgrass.dto.StickyBlogPostDto
import com.goodfeel.nightgrass.repo.BlogPostRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class BlogPostService(
    private val blogPostRepository: BlogPostRepository
) {
    fun getStickyBlogPosts(): Flux<StickyBlogPostDto> =
        blogPostRepository.findAllStickyPosts()

    fun getPostCountByCategory(): Flux<CategoryPostCountDto> =
        blogPostRepository.findBlogPostsCountByCategory()

    fun getRecentPosts(): Flux<RecentBlogPostDto> =
        blogPostRepository.findRecentPosts()

    fun findByPostId(postId: Int) = blogPostRepository.findByPostId(postId)

    fun findByShowOnHomepage(): Mono<BlogPost> =
        blogPostRepository.findByShowOnHomepage(true).next()

    fun saveBlogPost(blogPost: BlogPost): Mono<BlogPost> =
        blogPostRepository.save(blogPost)
}
