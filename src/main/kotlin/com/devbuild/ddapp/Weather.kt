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

class WeatherDataProvider() : DataProvider {

    var apiKey = ""
    var city = ""
    var units = ""

    override fun provideData(): GenericData = GenericData().apply {
        put("FORECAST", getForecast())
        put("CITY", city)
    }

    override fun config(config: Map<String, Any>) {
        apiKey = config["apiKey"] as? String ?: "invalid"
        city = config["city"] as? String ?: "Wroclaw"
        units = config["units"] as? String ?: "metric"
    }

    private fun getForecast(): Forecast {
        val resp = URL("http://api.openweathermap.org/data/2.5/forecast?q=$city&units=$units&appid=$apiKey").readText()
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(resp)
    }
}

class WeatherDataRenderer : DataRenderer<BufferedImage> {
    override fun renderData(dataProvider: DataProvider): List<BufferedImage> {
        val data = dataProvider.provideData()
        val forecast = data.get<Forecast>("FORECAST")
        val city = data.get<String>("CITY")

        val dd = forecast.list[0].dt
        val dt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dd * 1000L), ZoneOffset.UTC)
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Europe/Warsaw"))
        val dateTime = fmt.format(dt)

        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.color = Color.BLACK
        g2d.font =
            Font.createFont(
                Font.TRUETYPE_FONT,
                WeatherDataRenderer::class.java.getResourceAsStream("/fallout.ttf")
            ).deriveFont(18.0f)

        g2d.drawString("$city weather, $dateTime", 6, 24)
        g2d.drawString("Temp C  Wind m/s  Pressure  Fall mm", 6, 44)

        g2d.drawString(String.format("%.1f", forecast.list[0].main.temp), 6, 65)
        g2d.drawString(String.format("%.1f", getWind(forecast.list[0])), 60, 65)
        g2d.drawString(String.format("%d", forecast.list[0].main.groundLevelPressure), 125, 65)
        g2d.drawString(String.format("%.1f", getFall(forecast.list[0])), 187, 65)

        g2d.drawString("+6h", 214, 73)

        g2d.drawString(String.format("%.1f", forecast.list[2].main.temp), 6, 85)
        g2d.drawString(String.format("%.1f", getWind(forecast.list[2])), 60, 85)
        g2d.drawString(String.format("%d", forecast.list[2].main.groundLevelPressure), 125, 85)
        g2d.drawString(String.format("%.1f", getFall(forecast.list[2])), 187, 85)

        g2d.drawString("+6h", 214, 93)

        g2d.drawString(String.format("%.1f", forecast.list[4].main.temp), 6, 105)
        g2d.drawString(String.format("%.1f", getWind(forecast.list[4])), 60, 105)
        g2d.drawString(String.format("%d", forecast.list[4].main.groundLevelPressure), 125, 105)
        g2d.drawString(String.format("%.1f", getFall(forecast.list[4])), 187, 105)
        g2d.dispose()

        return listOf(image)
    }

    fun getWind(forecastItem: ForecastItem): Float = forecastItem.wind?.speed ?: 0.0f

    fun getFall(forecastItem: ForecastItem): Float = forecastItem.rain?.last3h ?: forecastItem.snow?.last3h ?: 0.0f
}

class WeatherConsoleRenderer: DataRenderer<String> {
    // FIXME
    override fun renderData(dataProvider: DataProvider): List<String> = listOf<String>("weather....")
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
