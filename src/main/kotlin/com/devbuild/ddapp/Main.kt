package com.devbuild.ddapp

import com.devbuild.waveshare213v2.Waveshare213v2
import com.pi4j.io.gpio.RaspiPin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import javax.imageio.ImageIO

@Volatile
var keepRunning = true

fun main() {
    val log = LoggerFactory.getLogger("DisplayApp")

    log.info("setup data vault")
    DataVault.init()

    log.info("initializing click button gestures")
    val gestureDetector = GestureDetector(RaspiPin.GPIO_02)
    gestureDetector.addListener {
        log.info("got gesture event {}", it)
        if (it.type == GestureType.N_CLICK && it.amount == 1) {
            Display.start()
        } else if (it.type == GestureType.N_CLICK && it.amount == 2) {
            Display.stop()
        }
    }

    log.info("adding shutdown hook")
    val mainThread = Thread.currentThread()
    Runtime.getRuntime().addShutdownHook(Thread {
        fun run() {
            log.info("shutting down...")
            keepRunning = false
            mainThread.join()
            Display.stop()
        }
    })

    log.info("prepare display")
    Display.init()
    while (keepRunning) {
        log.info("running...")
        Thread.sleep(5000)
    }
}

object Display {
    private var job: Job? = null

    fun init() = Waveshare213v2.fullUpdate()

    fun start() {
        if (job?.isActive == true) return

        job = GlobalScope.launch {
            val images = DataVault.provideImages()
            repeat(images.size) {
                Waveshare213v2.printImage(images[it])
                ImageIO.write(images[it], "png", File("check$it.png"));
                delay(5000L)
            }
            clean()
        }
    }

    fun stop() {
        job?.let { it.cancel() }
        clean()
    }

    private fun clean() {
        Waveshare213v2.clear(0xFFu)
        Waveshare213v2.sleep()
    }
}