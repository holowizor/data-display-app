package com.devbuild.ddapp

import com.devbuild.waveshare213v2.Waveshare213v2
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.RaspiPin
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import java.io.File
import javax.imageio.ImageIO

@Volatile
var keepRunning = true

fun main() {

    val btn = GpioFactory.getInstance().provisionDigitalInputPin(RaspiPin.GPIO_02, "BTN")
    btn.addListener(GpioPinListenerDigital {
        if (it.state.isHigh) {
            Display.display()
        }
    })

    val mainThread = Thread.currentThread()
    Runtime.getRuntime().addShutdownHook(Thread {
        fun  run() {
            keepRunning = false
            mainThread.join()
            Waveshare213v2.clear(0xFFu)
            Waveshare213v2.sleep()
        }
    })

    Waveshare213v2.fullUpdate()
    while (keepRunning) {
        Thread.sleep(5000)
    }
}

object Display {
    @Volatile
    var displayBusy = false

    fun display() {
        if (displayBusy) return

        displayBusy = true
        val image = InfectionsInPolandImageProvider().provideImage(InfectionsInPolandDataProvider())
        Waveshare213v2.printImage(image)

        // for testing
        ImageIO.write(image, "jpg", File("test.jpg"));

        Thread.sleep(5000)
        displayBusy = false
    }
}