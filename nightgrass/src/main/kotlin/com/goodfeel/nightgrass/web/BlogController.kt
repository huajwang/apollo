package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.dto.CategoryPostCountDto
import com.goodfeel.nightgrass.dto.StickyBlogPostDto
import com.goodfeel.nightgrass.service.BlogPostService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/blog")
class BlogController(
    private val blogPostService: BlogPostService
) {

    @GetMapping
    fun blogHome(model: Model): Mono<String> {
        val blogPostFlux: Flux<StickyBlogPostDto> = blogPostService.getStickyBlogPosts()
        val postCountByCategory: Flux<CategoryPostCountDto> = blogPostService.getPostCountByCategory()

        return Mono.zip(
            blogPostFlux.collectList(),
            postCountByCategory.collectList()
        ).doOnNext { tuple ->
            val blogPosts = tuple.t1
            val categoryPostCounts = tuple.t2
            model.addAttribute("blogPosts", blogPosts)
            model.addAttribute("categoryPostCounts", categoryPostCounts)
        }.then(Mono.just("blog"))
    }
}
