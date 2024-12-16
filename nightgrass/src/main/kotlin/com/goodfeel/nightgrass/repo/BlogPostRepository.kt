package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.BlogPost
import com.goodfeel.nightgrass.dto.CategoryPostCountDto
import com.goodfeel.nightgrass.dto.StickyBlogPostDto
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface BlogPostRepository : ReactiveCrudRepository<BlogPost, Int> {
    @Query("SELECT p.post_id, p.title, p.abstract, p.sticky_pin_no, p.published_at, u.nick_name AS author_name, " +
            "c.name AS category_name, m.file_path AS media_file_path, m.type AS media_type, m.caption AS media_caption\n" +
            "FROM e_mall_blog_posts p\n" +
            "LEFT JOIN e_mall_user u ON p.author_id = u.oauth_id\n" +
            "LEFT JOIN e_mall_blog_category c ON p.category_id = c.category_id\n" +
            "LEFT JOIN e_mall_blog_posts_media m ON p.main_media_id = m.media_id\n" +
            "WHERE p.sticky_pin_no > 0\n" +
            "ORDER BY p.sticky_pin_no DESC, p.created_at DESC")
    fun findAllStickyPosts(): Flux<StickyBlogPostDto>

    @Query("SELECT c.category_id, c.name AS category_name, COUNT(p.post_id) AS post_count\n" +
            "FROM e_mall_blog_category c\n" +
            "LEFT JOIN e_mall_blog_posts p ON c.category_id = p.category_id\n" +
            "GROUP BY c.category_id, c.name\n" +
            "ORDER BY post_count DESC")
    fun findBlogPostsCountByCategory(): Flux<CategoryPostCountDto>
}
