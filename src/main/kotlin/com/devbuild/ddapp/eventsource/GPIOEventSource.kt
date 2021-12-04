package com.devbuild.ddapp.eventsource

import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.Pin
import com.pi4j.io.gpio.PinState
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent
import com.pi4j.io.gpio.event.GpioPinListenerDigital
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GPIOEventSource(pin: Pin) : EventSource(), GpioPinListenerDigital {

    init {
        GpioFactory
            .getInstance()
            .provisionDigitalInputPin(pin, "BTN")
            .addListener(this)
    }

    private var t0 = 0L
    private var t1 = 0L
    private var clickCount = 0L

    override fun handleGpioPinDigitalStateChangeEvent(gprioEvent: GpioPinDigitalStateChangeEvent) {
        if (gprioEvent.state == PinState.HIGH) {
            t0 = System.currentTimeMillis();
        } else if (gprioEvent.state == PinState.LOW) {
            t1 = System.currentTimeMillis()
            if (t1 - t0 < 500) {
                clickCount++
                GlobalScope.launch {
                    delay(500L)
                    if (clickCount > 0L) {
                        sendGestureEvent(Event(EventType.N_CLICK, clickCount))
                    }
                }
            } else {
                sendGestureEvent(Event(EventType.LONG_PRESS, ((t1 - t0) / 1000L)))
            }
        }
    }

    private fun sendGestureEvent(e: Event) =
        GlobalScope.launch {
            notifyListeners(e)
            reset()
        }

    private fun reset() {
        t0 = 0L
        t1 = 0L
        clickCount = 0
    }
}
