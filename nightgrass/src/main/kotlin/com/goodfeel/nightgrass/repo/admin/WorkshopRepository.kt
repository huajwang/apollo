package com.goodfeel.nightgrass.repo.admin

import com.goodfeel.nightgrass.data.admin.Workshop
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux

interface WorkshopRepository : ReactiveCrudRepository<Workshop, Long> {
    fun findByShowOnHomepage(showOnHomepage: Boolean): Flux<Workshop>
}