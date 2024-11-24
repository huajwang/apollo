package com.goodfeel.nightgrass

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
open class NightgrassApplication

fun main(args: Array<String>) {
    runApplication<NightgrassApplication>(*args)
}
