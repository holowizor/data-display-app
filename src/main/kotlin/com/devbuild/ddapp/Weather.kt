package com.devbuild.ddapp

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

//Wroclaw, metric, 5db2640de7dbeb2a3327b13596b565cb
class WeatherDataProvider(val apiKey: String, val city: String = "Wroclaw", val units: String = "metric") :
    DataProvider {

    override fun provideData(): GenericData = GenericData().apply {
        val forecast = getForecast()
        put("FORECAST", forecast)
    }

    private fun getForecast(): Forecast {
        val resp = URL("http://api.openweathermap.org/data/2.5/forecast?q=$city&units=$units&appid=$apiKey").readText()
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(resp)
    }
}

class WeatherImageProvider : ImageProvider {
    override fun provideImage(dataProvider: DataProvider): List<BufferedImage> {
        val data = dataProvider.provideData()

        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.drawImage(
            ImageIO.read(InfectionsInPolandImageProvider::class.java.getResourceAsStream("/pipboy-doctor.png")),
            0,
            0,
            null
        )
        g2d.color = Color.BLACK

        g2d.font =
            Font.createFont(
                Font.TRUETYPE_FONT,
                InfectionsInPolandImageProvider::class.java.getResourceAsStream("/fallout.ttf")
            ).deriveFont(20.0f)
        g2d.drawString("COVID-19 IN POLAND", 96, 30)
        g2d.drawString("INFECTED: ${data.get<Int>("INFECTED")}", 96, 60)
        g2d.drawString("DECEASED: ${data.get<Int>("DECEASED")}", 96, 90)
        g2d.dispose()

        return listOf(image)
    }
}

data class Forecast(val list: List<ForecastItem>)
data class ForecastItem(
    val dt: Long,
    val dt_txt: String,
    val main: ForecastMain,
    val wind: ForecastWind?,
    val rain: ForecastFall?,
    val snow: ForecastFall?
)

data class ForecastMain(
    val temp: Float,
    val feels_like: Float, @JsonProperty("pressure") val seaLevelPressure: Int, @JsonProperty("grnd_level") val groundLevelPressure: Int,
    val humidity: Int
)

data class ForecastWind(val speed: Float, val deg: Int)
data class ForecastFall(@JsonProperty("3h") val last3h: Float)

fun main() {
    val dataProvider = WeatherDataProvider("5db2640de7dbeb2a3327b13596b565cb")
    val forecast: Forecast = dataProvider.provideData().get("FORECAST")
    println(forecast)

    val dd = 1587502800L // unix seconds in utc
    val dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dd * 1000L), ZoneOffset.UTC)
    val localTime = dt.withZoneSameLocal(ZoneId.of("Europe/Warsaw"))
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Europe/Warsaw"))

    println(dt)
    println(localTime)
    println(fmt.format(dt))
}