package com.devbuild.ddapp

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

class InfectionsInPolandDataProvider : DataProvider {
    override fun provideData(): GenericData = GenericData().apply {
        val infection = getInfection()
        put("INFECTED", infection.infected)
        put("DECEASED", infection.deceased)
    }

    private fun getInfection(): Infection {
        val resp =
            URL("https://api.apify.com/v2/key-value-stores/3Po6TV7wTht4vIEid/records/LATEST?disableRedirect=true").readText()
        val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        return mapper.readValue(resp)
    }

    private data class Infection(
        val infected: Int,
        val deceased: Int,
        val lastUpdatedAtApify: Date,
        val infectedByRegion: List<InfectedRegion>
    )

    private data class InfectedRegion(val region: String, val infectedCount: Int, val deceasedCount: Int)
}

class InfectionsInPolandDataRenderer : DataRenderer<BufferedImage> {
    override fun renderData(dataProvider: DataProvider): List<BufferedImage> {
        val data = dataProvider.provideData()

        val image = BufferedImage(250, 122, BufferedImage.TYPE_INT_RGB)

        val g2d = image.createGraphics()
        g2d.background = Color.white
        g2d.clearRect(0, 0, 250, 122)

        g2d.drawImage(
            ImageIO.read(InfectionsInPolandDataRenderer::class.java.getResourceAsStream("/pipboy-doctor.png")),
            0,
            0,
            null
        )
        g2d.color = Color.BLACK

        g2d.font =
            Font.createFont(
                Font.TRUETYPE_FONT,
                InfectionsInPolandDataRenderer::class.java.getResourceAsStream("/fallout.ttf")
            ).deriveFont(20.0f)
        g2d.drawString("COVID-19 IN POLAND", 96, 30)
        g2d.drawString("INFECTED: ${data.get<Int>("INFECTED")}", 96, 60)
        g2d.drawString("DECEASED: ${data.get<Int>("DECEASED")}", 96, 90)
        g2d.dispose()

        return listOf(image)
    }
}

class InfectionsInPolandConsoleRenderer : DataRenderer<String> {
    override fun renderData(dataProvider: DataProvider): List<String> {
        val data = dataProvider.provideData()
        return listOf("INFECTED: ${data.get<Int>("INFECTED")}", "DECEASED: ${data.get<Int>("DECEASED")}")
    }
}