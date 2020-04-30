package com.devbuild.ddapp

import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.Pin
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

enum class GestureType {
    N_CLICK, LONG_PRESS
}

data class GestureEvent(val type: GestureType, val amount: Int)

class GestureDetector(pin: Pin) : GpioPinListenerDigital {
    val log = LoggerFactory.getLogger("GestureDetector")

    init {
        GpioFactory
            .getInstance()
            .provisionDigitalInputPin(pin, "BTN")
            .addListener(this)
    }

    private val listeners = ArrayList<(GestureEvent) -> Unit>()
    fun addListener(listener: (GestureEvent) -> Unit) = listeners.add(listener)

    @Volatile
    private var t0 = 0L
    @Volatile
    private var t1 = 0L
    @Volatile
    private var clickCount = 0
    private val executor = Executors.newCachedThreadPool()
    @Volatile
    private var lastClickCountThread: ClickCountRunnable? = null

    override fun handleGpioPinDigitalStateChangeEvent(event: GpioPinDigitalStateChangeEvent?) {
        if (event?.state == PinState.HIGH) {
            log.info("state high")
            lastClickCountThread?.send = false
            t0 = System.currentTimeMillis();
        } else if (event?.state == PinState.LOW) {
            log.info("state low")
            t1 = System.currentTimeMillis()
            if (t1 - t0 < 500) {
                clickCount++
                lastClickCountThread = ClickCountRunnable(this)
                executor.submit(lastClickCountThread)
            } else {
                sendGestureEvent(GestureType.LONG_PRESS, ((t1 - t0) / 1000L).toInt())
                reset()
            }
        }
    }

    private class ClickCountRunnable(val detector: GestureDetector) : Runnable {
        @Volatile
        var send = true

        override fun run() {
            Thread.sleep(500L)
            if (send) {
                val count = detector.clickCount
                detector.reset()
                // reset first as event handling can take some time - run it in separate thread?
                detector.sendGestureEvent(GestureType.N_CLICK, count)
            }
        }
    }

    private fun sendGestureEvent(type: GestureType, amount: Int) =
        listeners.forEach {
            log.info("sending $type, $amount event")
            it.invoke(GestureEvent(type, amount))
        }

    private fun reset() {
        log.info("resetting counters")
        t0 = 0L
        t1 = 0L
        clickCount = 0
    }
}