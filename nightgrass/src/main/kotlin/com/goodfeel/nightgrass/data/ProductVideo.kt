package com.goodfeel.nightgrass.data

import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_product_video")
data class ProductVideo(
    val videoId: Long,
    val productId: Long,
    val videoType: VideoType = VideoType.FILE,
    val videoUrl: String,
    val orderIndex: Int = 0
)

enum class VideoType{
    FILE,
    YOUTUBE,
    VIMEO
}
