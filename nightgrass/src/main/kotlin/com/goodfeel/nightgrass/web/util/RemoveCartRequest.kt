package com.goodfeel.nightgrass.web.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class RemoveCartRequest @JsonCreator constructor(
    @JsonProperty("itemId") val itemId: Long
)
