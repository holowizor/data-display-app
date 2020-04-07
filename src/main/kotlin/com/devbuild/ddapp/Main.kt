package com.devbuild.ddapp

import com.devbuild.ddapp.gesture.GestureDetector
import com.devbuild.ddapp.gesture.GestureType
import com.devbuild.waveshare213v2.Waveshare213v2
import com.pi4j.io.gpio.RaspiPin
import org.slf4j.LoggerFactory
import java.io.File
import javax.imageio.ImageIO

@Volatile
var keepRunning = true
val logger = LoggerFactory.getLogger("DisplayApp")

fun main() {
    logger.info("initializing click button gestures")
    val gestureDetector = GestureDetector(RaspiPin.GPIO_02)
    gestureDetector.addListener {
        logger.info("got gesture event {}", it)
        if (it.type == GestureType.N_CLICK && it.amount == 1) {
            Display.display()
        } else if (it.type == GestureType.N_CLICK && it.amount == 2) {
            Waveshare213v2.clear(0xFFu)
        }
    }

    logger.info("adding shutdown hook")
    val mainThread = Thread.currentThread()
    Runtime.getRuntime().addShutdownHook(Thread {
        fun run() {
            logger.info("shutting down...")
            keepRunning = false
            mainThread.join()
            Waveshare213v2.clear(0xFFu)
            Waveshare213v2.sleep()
        }
    })

    logger.info("preparing display")
    Waveshare213v2.fullUpdate()
    while (keepRunning) {
        logger.info("waiting...")
        Thread.sleep(5000)
    }
}

object Display {
    @Volatile
    var displayBusy = false
    var inc = 0

    fun display() {
        if (displayBusy) return

        displayBusy = true
        val image = InfectionsInPolandImageProvider().provideImage(InfectionsInPolandDataProvider())
        Waveshare213v2.printImage(image)

        // for testing
        ImageIO.write(image, "jpg", File("test-${inc++}.jpg"));

        Thread.sleep(5000)
        displayBusy = false
    }
}