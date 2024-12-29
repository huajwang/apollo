package com.goodfeel.nightgrass.service.workshop

import com.goodfeel.nightgrass.data.admin.Workshop
import com.goodfeel.nightgrass.repo.admin.WorkshopRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class WorkshopService(
    private val workshopRepository: WorkshopRepository
) {

    fun getHomePageWorkshop(): Mono<Workshop> =
        workshopRepository.findByShowOnHomepage(true).next()
}
