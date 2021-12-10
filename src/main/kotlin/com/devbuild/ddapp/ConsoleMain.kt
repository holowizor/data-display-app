package com.devbuild.ddapp

import com.devbuild.ddapp.eventsource.EventType
import com.devbuild.ddapp.eventsource.KeyboardEventSource
import com.devbuild.ddapp.gesture.GestureDetector
import com.devbuild.waveshare213v2.Waveshare213v2
import com.pi4j.io.gpio.GpioFactory
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

fun main() {
    var keepRunning = true

    val log = LoggerFactory.getLogger("DisplayApp")
    log.info("setup data vault")
    val dataVault = DataVault<String>()
    dataVault.init("/console.yaml")

    val display = ConsoleDisplay(dataVault)

    val gestureDetector = GestureDetector(KeyboardEventSource()) {
        log.info("got gesture event {}", it)
        if (it.type == EventType.N_CLICK && it.value == 1L) {
            log.info("1 click")
            display.start()
        } else if (it.type == EventType.N_CLICK && it.value == 2L) {
            log.info("2 click")
            display.stop()
            display.startIdle()
        } else if (it.type == EventType.LONG_PRESS && it.value >= 3) {
            display.stop()
            display.sleep()
            log.info("exit")
            exitProcess(0)
        }
    }

    log.info("adding shutdown hook")
    val mainThread = Thread.currentThread()
    Runtime.getRuntime().addShutdownHook(Thread {
        fun run() {
            log.info("shutting down...")
            keepRunning = false
            mainThread.join()
            display.stop()
            display.sleep()
        }
    })

    log.info("prepare display")
    while (keepRunning) {
        log.info("running...")
        Thread.sleep(5000)
    }
}

class ConsoleConsumer : DataConsumer<String> {
    override fun consume(data: String) = println(data)
    override fun init() {}
    override fun sleep() {}
    override fun clear() {}
}

class ConsoleDisplay(
    dataVault: DataVault<String>,
    consumer: DataConsumer<String> = ConsoleConsumer(),
    screenSaver: SingleRenderer<String> = ConsoleScreenSaver(),
    loadingScreen: SingleRenderer<String> = ConsoleLoadingScreen(),
    shutdownScreen: SingleRenderer<String> = ConsoleShutdownScreen(),
) : Display<String>(dataVault, consumer, screenSaver, loadingScreen, shutdownScreen)

class ConsoleScreenSaver : SingleRenderer<String> {
    override fun render(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}

class ConsoleLoadingScreen : SingleRenderer<String> {
    override fun render(): String = "Starting up..."
}

class ConsoleShutdownScreen : SingleRenderer<String> {
    override fun render(): String = "shutting down..."
}

