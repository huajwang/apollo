package com.goodfeel.nightgrass.web

import com.goodfeel.nightgrass.dto.CategoryPostCountDto
import com.goodfeel.nightgrass.dto.RecentBlogPostDto
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
        val postCountByCategoryFlux: Flux<CategoryPostCountDto> = blogPostService.getPostCountByCategory()
        val recentPostsFlux: Flux<RecentBlogPostDto> = blogPostService.getRecentPosts()

        return Mono.zip(
            blogPostFlux.collectList(),
            postCountByCategoryFlux.collectList(),
            recentPostsFlux.collectList()
        ).doOnNext { tuple ->
            val blogPosts = tuple.t1
            val categoryPostCounts = tuple.t2
            val recentPosts = tuple.t3
            model.addAttribute("blogPosts", blogPosts)
            model.addAttribute("categoryPostCounts", categoryPostCounts)
            model.addAttribute("recentPosts", recentPosts)
        }.then(Mono.just("blog"))
    }
}
