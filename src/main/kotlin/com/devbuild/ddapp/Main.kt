package com.devbuild.ddapp

import com.devbuild.ddapp.eventsource.EventType
import com.devbuild.ddapp.eventsource.GPIOEventSource
import com.devbuild.ddapp.gesture.GestureDetector
import com.devbuild.waveshare213v2.Waveshare213v2
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.RaspiPin
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlin.system.exitProcess

fun main() {
    var keepRunning = true

    val log = LoggerFactory.getLogger("DisplayApp")

    log.info("setup data vault")
    val dataVault = DataVault<BufferedImage>()
    dataVault.init()

    val display = Waveshare213v2Display(dataVault)

    log.info("initializing click button gestures")
    val gestureDetector = GestureDetector(GPIOEventSource(RaspiPin.GPIO_02)) {
        log.info("got gesture event {}", it)
        if (it.type == EventType.N_CLICK && it.value == 1L) {
            display.start()
        } else if (it.type == EventType.N_CLICK && it.value == 2L) {
            display.stop()
            display.startIdle()
        } else if (it.type == EventType.LONG_PRESS && it.value >= 3) {
            display.stop()
            display.sleep()
            GpioFactory.getInstance().shutdown()
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
            GpioFactory.getInstance().shutdown()
        }
    })

    log.info("prepare display")
    while (keepRunning) {
        log.info("running...")
        Thread.sleep(5000)
    }
}

class Waveshare213v2Consumer : DataConsumer<BufferedImage> {
    override fun consume(data: BufferedImage) = Waveshare213v2.printImage(data)
    override fun init() = Waveshare213v2.fullUpdate()
    override fun sleep() = Waveshare213v2.sleep()
    override fun clear() = Waveshare213v2.clear(0xFFu)
}

class Waveshare213v2Display(
    dataVault: DataVault<BufferedImage>,
    consumer: DataConsumer<BufferedImage> = Waveshare213v2Consumer(),
    screenSaver: SingleRenderer<BufferedImage> = BIScreenSaver(),
    loadingScreen: SingleRenderer<BufferedImage> = BILoadingScreen(),
    shutdownScreen: SingleRenderer<BufferedImage> = BIShutdownScreen(),
) : Display<BufferedImage>(dataVault, consumer, screenSaver, loadingScreen, shutdownScreen)

class BIScreenSaver : SingleRenderer<BufferedImage> {

    override fun render(): BufferedImage {
        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font = Font.createFont(
            Font.TRUETYPE_FONT, WeatherDataRenderer::class.java.getResourceAsStream("/fallout.ttf")
        ).deriveFont(24.0f)

        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        g2d.drawString("$time", Random.nextInt(10, 80), Random.nextInt(34, 105))

        return image
    }
}

class BILoadingScreen : SingleRenderer<BufferedImage> {

    override fun render(): BufferedImage {
        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font = Font.createFont(
            Font.TRUETYPE_FONT, WeatherDataRenderer::class.java.getResourceAsStream("/fallout.ttf")
        ).deriveFont(24.0f)

        g2d.drawString("Starting up...", 10, 34)

        return image
    }
}

class BIShutdownScreen : SingleRenderer<BufferedImage> {

    override fun render(): BufferedImage {
        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font = Font.createFont(
            Font.TRUETYPE_FONT, WeatherDataRenderer::class.java.getResourceAsStream("/fallout.ttf")
        ).deriveFont(24.0f)

        g2d.drawString("Shutting down...", 10, 34)

        return image
    }
}