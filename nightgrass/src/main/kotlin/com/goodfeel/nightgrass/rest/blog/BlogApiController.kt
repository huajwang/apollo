package com.goodfeel.nightgrass.rest.blog

import com.goodfeel.nightgrass.data.BlogPost
import com.goodfeel.nightgrass.service.BlogPostService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.security.Principal

@RestController
@RequestMapping("/api/blog")
class BlogApiController(private val blogPostService: BlogPostService) {

    @PostMapping
    fun createBlog(
        @ModelAttribute blogPostRequest: BlogPostRequest,
        principal: Principal
    ): Mono<BlogPost> {
        val blogPost = BlogPost(
            title = blogPostRequest.title,
            content = blogPostRequest.content,
            authorId = principal.name,
            slug = "",
            abstract = "",
            categoryId = 1,
            thumbnail = "",
            mainMediaId = 1
        )
        return blogPostService.saveBlogPost(blogPost)
    }

    @GetMapping("/{id}")
    fun getBlog(@PathVariable id: Int): Mono<BlogPost> {
        return blogPostService.findByPostId(id)
    }



    data class BlogPostRequest(
        val title: String,
        val content: String
    )

}
