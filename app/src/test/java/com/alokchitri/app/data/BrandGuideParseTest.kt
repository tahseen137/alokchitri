package com.alokchitri.app.data

import com.alokchitri.app.ui.screens.PhoneBrand
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BrandGuideParseTest {
    private val repoGuide = File("../guides/brand-guides.json").readText()

    @Test
    fun `published guide parses for every brand in both languages`() {
        for (lang in listOf("bn", "en")) {
            val guide = parseBrandGuide(repoGuide, lang)!!
            PhoneBrand.entries.forEach { assertTrue(guide.getValue(it).isNotEmpty(), "$lang ${it.name}") }
        }
        assertTrue(parseBrandGuide(repoGuide, "en")!!.getValue(PhoneBrand.SAMSUNG)[1].text.contains("PRO"))
    }

    @Test
    fun `null screenshot stays null, a url is kept`() {
        val json = """{"version":1,"brands":{"XIAOMI":[
            {"en":"a","screenshot":null},{"en":"b","screenshot":"https://x/y.png"},{"en":"c"}]}}"""
        val steps = parseBrandGuide(json, "en")!!.getValue(PhoneBrand.XIAOMI)
        assertEquals(listOf(null, "https://x/y.png", null), steps.map { it.screenshotUrl })
    }

    @Test
    fun `missing language falls back to english`() {
        val json = """{"version":1,"brands":{"REALME":[{"en":"english only"}]}}"""
        assertEquals("english only", parseBrandGuide(json, "bn")!!.getValue(PhoneBrand.REALME)[0].text)
    }

    @Test
    fun `unknown version, broken json and empty guide are rejected`() {
        assertNull(parseBrandGuide("""{"version":2,"brands":{"XIAOMI":[{"en":"a"}]}}""", "en"))
        assertNull(parseBrandGuide("not json", "en"))
        assertNull(parseBrandGuide("""{"version":1,"brands":{}}""", "en"))
    }
}
