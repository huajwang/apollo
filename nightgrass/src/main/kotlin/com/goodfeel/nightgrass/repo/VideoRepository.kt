package com.goodfeel.nightgrass.repo

import com.goodfeel.nightgrass.data.Video
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface VideoRepository : ReactiveCrudRepository<Video, String>

