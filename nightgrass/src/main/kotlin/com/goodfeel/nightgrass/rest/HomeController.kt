package com.goodfeel.nightgrass.rest

import com.goodfeel.nightgrass.service.BlogPostService
import com.goodfeel.nightgrass.service.workshop.WorkshopService
import com.goodfeel.nightgrass.serviceImpl.ProductService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalTime

@RestController("restHomeController")
@RequestMapping("/api")
class HomeController(
    private val productService: ProductService,
    private val workshopService: WorkshopService,
    private val blogPostService: BlogPostService
) {
    @GetMapping
    fun home(): Mono<HomeResponse> {
        val bigHitsMono = productService.getTop3BigHits().collectList()
        val popularsMono = productService.getTop8PopularOrNewProducts().collectList()
        val heroCardMono =  workshopService.getHomePageWorkshop()
            .map {
                HeroCard(
                    title = it.title,
                    description = it.description,
                    location = it.location,
                    timeStart = it.timeStart,
                    timeEnd = it.timeEnd,
                    eventDate = it.date
                )
            }.switchIfEmpty(
                blogPostService.findByShowOnHomepage().map {
                    HeroCard(
                        type = 2,
                        title = it.title,
                        description = it.abstract,
                        blogPostId = it.postId,
                        blogPostThumbnail = it.thumbnail
                    )
                }
            ) // TODO - make sure at least one workshop or one blog post for showing on home page hero card

        return Mono.zip(bigHitsMono, popularsMono, heroCardMono).map { tuple ->
            HomeResponse(
                bigHits = tuple.t1,
                populars = tuple.t2,
                heroCard = tuple.t3
            )
        }
    }

    data class HomeResponse(
        val bigHits: List<Any>,
        val populars: List<Any>,
        val heroCard: HeroCard
    )

    data class HeroCard(
        val type: Int = 1, // 1 workshop or event; 2 blog post
        val title: String,
        val description: String,
        val eventDate: LocalDate? = null,
        val timeStart: LocalTime? = null,
        val timeEnd: LocalTime?= null,
        val location: String? = null,

        val blogPostId: Int? = null,
        val blogPostThumbnail: String? = null
    )
}
