package com.goodfeel.nightgrass.data

import org.springframework.data.relational.core.mapping.Table

@Table("e_mall_blog_category")
data class BlogCategory(
    val categoryId: Int? = null,
    val slug: String,
    val name: String
)
