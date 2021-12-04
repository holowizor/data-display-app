package com.devbuild.ddapp

import com.devbuild.ddapp.eventsource.EventType
import com.devbuild.ddapp.eventsource.KeyboardEventSource
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

var runs = true

fun main() {
    val log = LoggerFactory.getLogger("DisplayApp")

    log.info("initializing click button gestures")
    val gestureDetector = GestureDetector(KeyboardEventSource()) {
        log.info("got gesture event {}", it)
        if (it.type == EventType.N_CLICK && it.value == 1L) {
            log.info("1 click")
        } else if (it.type == EventType.N_CLICK && it.value == 2L) {
            log.info("2 click")
        } else if (it.type == EventType.LONG_PRESS && it.value >= 3) {
            log.info("exit")
            exitProcess(0)
        }
    }

    while (runs) {
        log.info("running...")
        Thread.sleep(5000)
    }
}

