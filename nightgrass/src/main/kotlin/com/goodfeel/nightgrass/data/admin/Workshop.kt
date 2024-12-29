package com.goodfeel.nightgrass.data.admin

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Table("e_mall_workshops")
data class Workshop(
    @Id
    val id: Long? = null,
    val title: String,
    val description: String,
    val date: LocalDate,
    val timeStart: LocalTime,
    val timeEnd: LocalTime,
    val location: String,
    val activities: String,
    val showOnHomepage: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime? = null
)
