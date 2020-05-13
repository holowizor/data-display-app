package com.devbuild.ddapp

import com.devbuild.waveshare213v2.Waveshare213v2
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.RaspiPin
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.system.exitProcess

@Volatile
var keepRunning = true

fun main() {
    val log = LoggerFactory.getLogger("DisplayApp")
    Display.init()

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
            Display.startIdle()
        } else if (it.type == GestureType.LONG_PRESS && it.amount >= 3) {
            Display.stop()
            Display.sleep()
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
            Display.stop()
            Display.sleep()
            GpioFactory.getInstance().shutdown()
        }
    })

    log.info("prepare display")
    while (keepRunning) {
        log.info("running...")
        Thread.sleep(5000)
    }
}

object Display {
    private var job: Job? = null
    private var idleJob: Job? = null

    fun init() {
        Waveshare213v2.fullUpdate()
        Waveshare213v2.printImage(LoadingScreen.render())
        startIdle()
    }

    fun start() {
        idleJob?.let { it.cancel() }

        if (job?.isActive == true) return

        job = GlobalScope.launch {
            val images = DataVault.provideImages()
            repeat(images.size) {
                Waveshare213v2.printImage(images[it])
                ImageIO.write(images[it], "png", File("check$it.png"));
                delay(5000L)
            }
            startIdle()
        }
    }

    fun startIdle() {
        if (idleJob?.isActive == true) return

        idleJob = GlobalScope.launch {
            delay(5000L)
            Waveshare213v2.printImage(ScreenSaver.render())
            delay(25000L)
        }
    }

    fun stop() {
        job?.let { it.cancel() }
        clean()
    }

    fun sleep() {
        Waveshare213v2.sleep()
    }

    private fun clean() {
        Waveshare213v2.clear(0xFFu)
    }
}

object ScreenSaver {

    fun render(): BufferedImage {
        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font =
            Font.createFont(
                Font.TRUETYPE_FONT,
                WeatherImageProvider::class.java.getResourceAsStream("/fallout.ttf")
            ).deriveFont(24.0f)

        val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        g2d.drawString("$time", Random.nextInt(10, 80), Random.nextInt(34, 105))

        return image
    }
}

object LoadingScreen {

    fun render(): BufferedImage {
        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font =
            Font.createFont(
                Font.TRUETYPE_FONT,
                WeatherImageProvider::class.java.getResourceAsStream("/fallout.ttf")
            ).deriveFont(24.0f)

        g2d.drawString("Starting up...", 10, 34)

        return image
    }
}

object ShutDownScreen {

    fun render(): BufferedImage {
        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font =
            Font.createFont(
                Font.TRUETYPE_FONT,
                WeatherImageProvider::class.java.getResourceAsStream("/fallout.ttf")
            ).deriveFont(24.0f)

        g2d.drawString("Shutting down...", 10, 34)

        return image
    }
}